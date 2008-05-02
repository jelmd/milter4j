/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.util.FutureTaskExecutor;

/**
 * The Mail Filter server, which can be used as a multiplex for several
 * Sub-Mail-Filters.
 * <p>
 * The server goes into daemon mode and starts to listen on the configured 
 * address as soon as there are any successfully instantiate filters.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Server extends Thread
	implements PropertyChangeListener, ServerMBean
{
	static final Logger log = LoggerFactory
		.getLogger(Server.class);
	private Configuration cfg;
	
	private ServerSocketChannel socketChannel;
	private boolean socketChanged;
	private boolean filtersChanged;
	private boolean versionChanged;
	private boolean rcptToChanged;

	boolean shutdown = false;
	
	private FutureTaskExecutor executor;
	private ArrayList<MailFilter> filters;
	private ArrayList<Worker> workers;
	private StatsCollector stats;
	Thread shutdownListener;
	private int workerOffset = 0;
	
	private static final ObjectName getMBeanName(boolean server) { 
		try {
			return server 
				? new ObjectName("Milter4J:type=Server")
				: new ObjectName("Milter4J:type=ExecutorService");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
	
	/**
	 * Create a new Server using the given configuration file.
	 * 
	 * @param configFile	config file to use. If <code>null</code>, the 
	 * 		default will be used ({@value Configuration#DEFAULT_CONFIG}.
	 * @see Configuration
	 */
	public Server(String configFile) {
		cfg = new Configuration(configFile);
		cfg.add(this);
		executor = new FutureTaskExecutor(3, cfg.getMaxWorkers(), 
			5L, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());
		stats = new StatsCollector(cfg.getSampleRates(), cfg.getSamples());
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(executor, getMBeanName(false));
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("constructor", e);
			}
		}
		configureShutdown();
		socketChanged = true;
		filtersChanged = true;
		redoConf();
		try {
			mbs.registerMBean(this, getMBeanName(true));
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("constructor", e);
			}
		}
	}

	private Worker getFreeWorker() {
		lock.lock();
		try {
			if (workers == null) {
				workers = new ArrayList<Worker>();
			} else {
				int stop = workerOffset;
				int size = workers.size();
				for (;workerOffset < size; workerOffset++) {
					Worker w = workers.get(workerOffset);
					if (w.isReady()) {
						workerOffset++;
						return w;
					}
				}
				workerOffset = 0;
				if (stop > size) {
					stop = size;
				}
				for (;workerOffset < stop; workerOffset++) {
					Worker w = workers.get(workerOffset);
					if (w.isReady()) {
						workerOffset++;
						return w;
					}
				}
			}
			// since thread per worker, make sure, that each one has its own instance
			ArrayList<MailFilter> newFilters  = new ArrayList<MailFilter>();
			for (MailFilter mf : filters) {
				newFilters.add(mf.getInstance());
			}
			Worker w = new Worker(newFilters, stats);
			w.enableVersionHeader(cfg.addRecipient());
			w.enableRcptToHeader(cfg.addRecipient());
			workers.add(w);
			workerOffset = 0;
			return w;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		while (!shutdown) {
			if (filtersChanged || socketChanged || !socketChannel.isOpen()
				|| rcptToChanged || versionChanged) 
			{
				redoConf();
			}
			if (socketChannel == null) {
				log.warn("socket unavailable - terminating");
				return;
			}
			SocketChannel sc = null;
			Worker w = null;
			try {
				sc = socketChannel.accept();
				if (shutdown) {
					return;
				}
				sc.configureBlocking(true);
				w = getFreeWorker();
				w.prepare(sc);
				executor.submit(w);
				stats.addConnection();
				if (log.isDebugEnabled()) {
					log.debug("Worker {} registered for accept()", 
						w.getName());
				}
				sc = null; // indicate, everything is ok
			} catch (RejectedExecutionException e) {
				log.warn("Thread Pool execution limit reached: " 
					+ executor.getActiveCount() + "/" 
					+ executor.getMaximumPoolSize());
				log.info("Increasing the 'worker' config attribute value may help"
					+ " (which may overload the machine) or limit the number of"
					+ " active concurrent sendmail connections");
				if (log.isDebugEnabled()) {
					log.debug("run", e);
				}
				w.prepare(null);
			} catch (Exception e) {
				// might be IO or RejectedExecutionException
				if (!shutdown) {
					log.warn(e.getClass().getSimpleName() + ": " 
						+ e.getLocalizedMessage());
					if (log.isDebugEnabled()) {
						log.debug("run", e);
					}
				}
			}
			if (sc != null && sc.isOpen()) {
				try { sc.close(); } catch (Exception e1) { /* ignore */ }
			}
		}
	}

	/**
	 * Initialize Socket, Channel and Selector
	 */
	private void initSocket() {
		if (socketChannel != null && socketChannel.isOpen()) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
		SocketAddress addr = cfg.getAddress();
		ServerSocketChannel ssc = null;
		try {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(true);
			ssc.socket().bind(addr);
			socketChannel = ssc;
		} catch (IOException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("initSocket", e);
			}
			if (ssc != null) {
				try { ssc.close(); } catch (Exception x) { /* ignore */ }
			}
		}
	}
	
	private ReentrantLock lock = new ReentrantLock();

	private void initFilters() {
		lock.lock();
		try {
			if (workers != null) {
				for (Worker worker : workers) {
					worker.shutdown();
				}
				workers.clear();
			}
			ArrayList<MailFilter> newFilters = new ArrayList<MailFilter>();
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			stats.removeAll(mbs);
			String[] cff = cfg.getFilters();
			if (cff.length == 0) {
				return;
			}
			// order is important
			for (int i=0; i < cff.length; i++) {
				String[] tmp = cff[i].split(";", 2);
				try {
					Class<?> clazz = Class.forName(tmp[0]);
					Constructor<?> c = clazz.getConstructor(String.class);
					MailFilter f = (MailFilter) c.newInstance(tmp[1]);
					newFilters.add(f);
					stats.add(f.getStatName(), mbs);
				} catch (Exception e) {
					log.warn(e.getLocalizedMessage());
					if (log.isDebugEnabled()) {
						log.debug("initFilters", e);
					}
				}
			}
			if (filters == null) {
				filters = newFilters;
			} else {
				filters.clear();
				filters.addAll(newFilters);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void reconfigure() {
		if (cfg != null) {
			if (!cfg.reconfigure()) {
				// params are the same, but the config (e.g. read via file) might
				// have been changed - so force reconfig of filters as well
				filtersChanged = true;
			}
		}
	}

	@Override
	public String listConfig() {
		if (cfg != null) {
			return cfg.listConfig();
		}
		return "";
	}

	/**
	 * Reconfigure socket and/or filters, depending on the current flagged
	 * change state.
	 */
	private void redoConf() {
		if (filtersChanged) {
			initFilters();
			filtersChanged = false;
		}
		if (socketChanged) {
			initSocket();
			socketChanged = false;
		}
		if (workers != null && (versionChanged || rcptToChanged)) {
			boolean v = cfg.addVersion();
			boolean r = cfg.addRecipient();
			for (Worker t : workers) {
				t.enableVersionHeader(v);
				t.enableRcptToHeader(r);
			}
		}
	}

	private void configureShutdown() {
		final InetSocketAddress sa = cfg.getShutdownAddress();
		Runnable r = new Runnable() {
			@Override
			public void run() {
				ServerSocketChannel ssc = null;
				try {
					ssc = ServerSocketChannel.open();
					ssc.configureBlocking(true);
					ssc.socket().bind(sa);
					while (!shutdown) {
						SocketChannel sc = ssc.accept();
						try {
							sc.socket().setKeepAlive(false);
							sc.socket().setSoTimeout(3 * 1000);
							ByteBuffer buf = ByteBuffer.allocate(8);
							int res = sc.read(buf);
							if (res == 8) {
								String s = new String(buf.array());
								if (s.equals("shutdown")) {
									break;
								}
							}
						} catch (Exception e) {
							/** ignore */
						} finally {
							try { sc.close(); } catch (Exception e) { /* */ } 
						}
					}
				} catch (IOException e) {
					if (!shutdown) {
						log.warn("shutdown listener: " + e.getLocalizedMessage());
						if (log.isDebugEnabled()) {
							log.debug("configureShutdown", e);
						}
					}
				} finally {
					try { ssc.close(); } catch (Exception e) { /* ignore */ }
				}
				shutdownListener = null;
				shutdown();
			}
		};
		// don't put it into the executor - might be shutdowned before this one
		// gets into action
		shutdownListener = new Thread(r, "ShutdownListener");
		shutdownListener.start();
	}
	
	/**
	 * Handle the property change request for socket and filter changes.
	 * @param evt {@inheritDoc}
	 * @see Configuration#FILTER_CHANGED
	 * @see Configuration#SOCKET_CHANGED
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() != cfg) {
			return;
		}
		String tmp = evt.getPropertyName();
		if (tmp.equals(Configuration.FILTER_CHANGED)) {
			filtersChanged = true;
		} else if (tmp.equals(Configuration.SOCKET_CHANGED)) {
			socketChanged = true;
		} else if (tmp.equals(Configuration.SHUTDOWN_CHANGED)) {
			configureShutdown();
		} else if  (tmp.equals(Configuration.VERSION_CHANGED)) {
			versionChanged = true;
		} else if (tmp.equals(Configuration.RCPTTO_CHANGED)) {
			rcptToChanged = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int getWorkers() {
		return workers == null ? 0 : workers.size();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getFilterNames() {
		String[] list = new String[filters.size()];
		for (int i=0; i < list.length; i++) {
			list[i] = filters.get(i).getStatName();
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	public Date getStartTime() {
		return stats.getStartTime();
	}
	/**
	 * {@inheritDoc}
	 */
	public long[] getSampleRates() {
		return stats.getSampleRates();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TabularData getHistory() {
		return stats.getHistory(0, true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public TabularData getHistory(int idx, boolean relative) {
		return stats.getHistory(idx, relative);
	}

	/**
	 * {@inheritDoc}
	 */
	public long getConnections() {
		return stats.getConnections();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getVersion() {
		return new Version().getVersionInfo();
	}
	/**
	 * {@inheritDoc}
	 */
	public void shutdown() {
		log.info("shutdown initiated ...");
		if (shutdown) {
			// don't need to do that several times ;-)
			return;
		}
		shutdown = true;
		if (shutdownListener != null) {
			shutdownListener.interrupt();
		}
		if (socketChannel != null) {
			try {
				socketChannel.close();
			} catch (Exception e1) {
				log.warn(e1.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e1);
				}
			}
		}
		cfg.remove(this);
		executor.shutdown();
		if (filters != null) {
			filters.clear();
		}
		if (workers != null) {
			for (Worker worker : workers) {
				worker.shutdown();
			}
		}
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if (stats != null) {
			stats.removeAll(mbs);
			stats.shutdown();
		}
		try {
			mbs.unregisterMBean(getMBeanName(false));
		} catch (Exception e) {
			// ignore
		}
		try {
			mbs.unregisterMBean(getMBeanName(true));
		} catch (Exception e) {
			// ignore
		}
		log.info("done.");
	}

	/**
	 * @param args  [configurationFile] ["shutdown"]
	 */
	public static void main(String[] args) {
		boolean shutdown = false;
		String config = null;
		if (args.length > 1 && args[1].equals("shutdown")) {
			shutdown = true;
			config = args[0];
		} else if (args.length > 0) {
			if (args[0].equals("shutdown")) {
				shutdown = true;
			} else {
				config = args[0];
			}
		}
		if (shutdown) {
			Configuration cfg = new Configuration(config);
			InetSocketAddress sa = cfg.getShutdownAddress();
			SocketChannel sc = null;
			try {
				sc = SocketChannel.open(sa);
				sc.write(ByteBuffer.wrap("shutdown".getBytes()));
				log.info("shutdown command sent");
			} catch (IOException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("main", e);
				}
			} finally {
				try { sc.close(); } catch (Exception x) { /* ignore */ }
			}
		} else {
			Server s = new Server(config);
	//		s.setDaemon(true);
			s.start();
		}
	}

}
