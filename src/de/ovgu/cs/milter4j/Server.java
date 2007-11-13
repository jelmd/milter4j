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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

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
public class Server extends Thread implements PropertyChangeListener {
	private static final Logger log = LoggerFactory
		.getLogger(Server.class);
	private Configuration cfg;
	
	private ServerSocketChannel socketChannel;
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
		Worker w = new Worker(filters);
		workers.add(w);
		return w;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		while (true) {
			if (filtersChanged || socketChanged || !socketChannel.isOpen()) {
				reconfigure();
			}
			if (socketChannel == null) {
				log.warn("Unable to open socket - terminating");
				return;
			}
			try {
				SocketChannel sc = socketChannel.accept();
				sc.configureBlocking(true);
				Worker w = getFreeWorker();
				w.prepare(sc);
				executor.submit(w);
				if (log.isDebugEnabled()) {
					log.debug("Worker {} registered for accept()", 
						w.getName());
				}
			} catch (IOException e) {
				log.warn(e.getLocalizedMessage());
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
				log.debug("method()", e);
			}
			if (ssc != null) {
				try { ssc.close(); } catch (Exception x) { /* ignore */ }
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
