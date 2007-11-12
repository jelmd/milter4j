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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.reply.Packet;
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
public class Server extends Thread implements PropertyChangeListener {
	private static final Logger log = LoggerFactory
		.getLogger(Server.class);
	private Configuration cfg;
	
//	private ServerSocketChannel socketChannel;
	private Selector socketSelector;
	private boolean socketChanged;
	private boolean filtersChanged;
	
	private ExecutorService executor;
	private ArrayList<MailFilter> filters;
	private ConcurrentSkipListSet<Worker> workers;
	
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
		executor = new FutureTaskExecutor(3, 256, 5L, TimeUnit.MINUTES,
            new SynchronousQueue<Runnable>());
		socketChanged = true;
		filtersChanged = true;
		reconfigure();
	}

	private Worker getFreeWorker() {
		if (workers == null) {
			workers = new ConcurrentSkipListSet<Worker>();
		} else {
			for (Worker t : workers) {
				if (t.isReady()) {
					return t;
				}
			}
		}
		Worker w = new Worker(filters, executor);
		workers.add(w);
		return w;
	}

	private void write(SelectionKey key) {
		Object o = key.attachment();
		if (o != null) {
			Worker w = (Worker) o;
			SocketChannel sc = (SocketChannel) key.channel();
			// we assume, packets are small and can be send at once
			ConcurrentLinkedQueue<Packet> queue = 
				w.getQueue2send();
			Packet p;
			while ((p = queue.peek()) != null) {
				boolean ok = false;
				try {
					if (log.isDebugEnabled()) {
						log.debug("{} sending {} ...", w, p);
					}
					ok = p.send(sc);
					log.debug("{} done", w, p);
				} catch (Exception e) {
					queue.clear();
					try { 
						key.channel().close(); 
					} catch (Exception x) {
						// ignore
					}
					log.warn(e.getLocalizedMessage());
					if (log.isDebugEnabled()) {
						log.debug("run - key canceled", e);
					}
				}
				if (ok) {
					queue.poll();
				} else {
					// give the socket a little bit time to flush
					// its buffers
					break;
				}
			}
			if (queue.isEmpty() && key.isValid()) {
				key.selector().wakeup();
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void read(SelectionKey key) throws IOException {
			Object w = key.attachment();
			if (w != null) {
				synchronized (workers) {
					key.interestOps(0);
					boolean tryNext = true;
					while (tryNext) {
						if (log.isDebugEnabled()) {
							log.debug("{} reading ...", w);
						}
						tryNext = ((Worker) w).read(key);
						if (log.isDebugEnabled()) {
							log.debug("{} done (full paket: {})", w, tryNext);
						}
					}
				}
			}		
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel ssch = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssch.accept();
		sc.configureBlocking(false);
		Worker w = getFreeWorker();
		sc.register(socketSelector, SelectionKey.OP_READ, w);
		if (log.isDebugEnabled()) {
			log.debug("Worker {} registered for accept()", w.getName());
		}
	}

	private void debugKey(SelectionKey key) {
		if (!key.isValid()) {
			log.debug("State: invalid");
			return;
		}
		StringBuilder buf = new StringBuilder("State: ");
		int i = key.interestOps();
		if ((i & SelectionKey.OP_ACCEPT) > 0) {
			buf.append("accept,");
		}
		if ((i & SelectionKey.OP_CONNECT) > 0) {
			buf.append("connect,");
		}
		if ((i & SelectionKey.OP_READ) > 0) {
			buf.append("read,");
		}
		if ((i & SelectionKey.OP_WRITE) > 0) {
			buf.append("write,");
		}
		buf.append("  ");
		if (key.isAcceptable()) {
			buf.append("acceptable,");
		}
		if (key.isConnectable()) {
			buf.append("connectable,");
		}
		if (key.isReadable()) {
			buf.append("readable,");
		}
		if (key.isWritable()) {
			buf.append("writable,");
		}
		if (key.isValid()) {
			buf.append("valid");
		}
		log.debug(buf.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		while (true) {
			if (filtersChanged || socketChanged || !socketSelector.isOpen()) {
				reconfigure();
			}
			if (socketSelector == null) {
				log.warn("Unable to open socket - terminating");
				return;
			}
			SelectionKey key = null;
			try {
				if (log.isDebugEnabled()) {
					log.debug("Socket selector is open: " 
						+ socketSelector.isOpen());
				}
				// Wait for an event one of the registered channels
				socketSelector.select();
				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> keys = 
					socketSelector.selectedKeys().iterator();
				while (keys.hasNext()) {
					key = keys.next();
					keys.remove();
					if (!key.isValid()) {
						continue;
					}
					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						accept(key);
					} else if (key.isWritable()) {
						write(key);
					} else if (key.isReadable()) {
						read(key);
					}
					if (log.isDebugEnabled()) {
						debugKey(key);
					}
				}
			} catch (IOException e) {
				log.warn(e.getLocalizedMessage());
				if (key != null) {
					try { key.channel().close(); } catch (IOException e1) { /**/ }
					Object o = key.attachment();
					((Worker) o).cleanup(false, null);
					log.debug("connection closed");
					key.selector().wakeup();
				}
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
	}

	/**
	 * Initialize Socket, Channel and Selector
	 */
	private void initSocket() {
		if (socketSelector != null && socketSelector.isOpen()) {
			try {
				socketSelector.close();
			} catch (IOException e) {
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
		SocketAddress addr = cfg.getAddress();
		ServerSocketChannel socketChannel = null;
		try {
			socketSelector = Selector.open();
			socketChannel = ServerSocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.socket().bind(addr);
			socketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", e);
			}
			if (socketChannel != null) {
				try { socketChannel.close(); } catch (Exception x) { /* ignore */ }
			}
			if (socketSelector != null) {
				try { socketSelector.close(); } catch (Exception x) { /* ignore */ }
			}
		}
	}
	
	private void initFilters() {
		if (workers != null) {
			for (Worker worker : workers) {
				worker.shutdown();
			}
		}
		if (filters == null) {
			filters = new ArrayList<MailFilter>();
		} else {
			filters.clear();
		}
		String[] cff = cfg.getFilters();
		if (cff.length == 0) {
			return;
		}
		// order is important
		for (int i=0; i < cff.length; i++) {
			String[] tmp = cff[i].split(";", 2);
			try {
				Class<?> clazz = Class.forName(tmp[i]);
				Constructor<?> c = clazz.getConstructor(String.class);
				MailFilter f = (MailFilter) c.newInstance(tmp[1]);
				filters.add(f);
			} catch (ClassNotFoundException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (SecurityException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (NoSuchMethodException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (IllegalArgumentException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (InstantiationException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (IllegalAccessException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (InvocationTargetException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
	}
	
	/**
	 * Reconfigure socket and/or filters, depending on the current flagged
	 * change state.
	 */
	private void reconfigure() {
		if (filtersChanged) {
			initFilters();
			filtersChanged = false;
		}
		if (socketChanged) {
			initSocket();
			socketChanged = false;
		}
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
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Server s = new Server(args.length > 0 ? args[0] : null);
//		s.setDaemon(true);
		s.start();
	}

}
