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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.util.Misc;

/**
 * Storage for the mail filter server configuration.
 * 
 * File format used:
 * <pre>
 * &lt;config port="4444" host="*" shutdown="4445" workers="256"
 * 	samples="255" samplerates="1m, 5m, 4h, 1d, 1w"
 * 	&gt;
 * 	&lt;filter class="org.bla.fahsel.milter.Cool" conf="/etc/cool.conf"/&gt;
 * &lt;/config&gt;
 * </pre>
 * 
 * The {@code config} attributes have the following meaning:
 * <dl>
 * <dt>port</dt>
 * <dd>
 * The port, where the filter manager should listen for MTA commands. If it 
 * is omitted, the default port {@value #DEFAULT_PORT} will be used.
 * </dd>
 * <dt>host</dt>
 * <dd>
 * The interface, where the filter manager should bind to. If it is ommited or 
 * is "<code>*</code>", the server binds to all interfaces of the host. 
 * Otherwise it binds to the given interface, only.
 * <dd>
 * <dt>shutdown</dt>
 * <dd>The port, on which the filter manager should listen for shutdown commands.
 * It will always bind to the {@code localhost} interface. If ommitted, it will
 * be set to {@value #DEFAULT_SHUTDOWN_PORT}.
 * </dd>
 * <dt>workers</dt>
 * <dd>
 * The max. number of workers to be used in the thread pool for handling 
 * mail requests.
 * </dt>
 * <dt>samplerates</dt>
 * <dd>
 * A comma separated list of time intervalls, at which "number of connections 
 * seen" snapshots should be made. If omitted, a reasonable default wil be used.
 * A time intervall is defined as an int value , optionally followed by 
 * {@code s} (seconds), {@code m} (minutes), {@code h} (hours), {@code d} (days)
 * or {@code w} (weeks). If omitted, {@code s} is assumed. 
 * </dd>
 * <dt>samples</dt>
 * <dd>
 * The number of samples (snapshots), which should be kept for each sample 
 * intervall. It should be a 2<sup>n</sup>-1 value.
 * </dd>
 * </dl>
 * The {@code filter} element may occure several times. Its attributes have the 
 * following meaning:
 * <dl>
 * <dt>class</dt>
 * <dd>
 * The fully qualified classname of a mail filter to use.
 * </dd>
 * <dt>conf</dt>
 * <dd>
 * It may be used to pass an additional parameter (e.g. configuration
 * filename) to the given class, when the server instantiates the mail filter.
 * </dd>
 * </dl>
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Configuration {
	private static final Logger log = LoggerFactory
		.getLogger(Configuration.class);

	/** the default mail filter server configuration file */ 
	public static final String DEFAULT_CONFIG = "/etc/mail/milter.conf";
	/** the default port, the mail filter server should listen for connections */
	public static final int DEFAULT_PORT = 4444;

	/** the default port to listen for shutdown instruction */
	public static final int DEFAULT_SHUTDOWN_PORT = DEFAULT_PORT+1;

	/** property name used to notify config listeners about filter changes */ 
	public static final String FILTER_CHANGED = "filter";
	/** property name used to notify config listeners about socket changes */ 
	public static final String SOCKET_CHANGED = "socket";
	/** property name used to notify config listeners about shutdown port changes */ 
	public static final String SHUTDOWN_CHANGED = "shutdown";
	/** default sample rate/hiistory size for connection statistics */
	public static final int DEFAULT_SAMPLES = 255;
	static final int[] DEFAULT_SAMPLE_RATES = new int[] {
		60, 5 * 60, 30 * 60, 4 * 60 * 60, 24 * 60 * 60
	};
	/** default number of max. threads for the executor service beeing used */
	public static final int DEFAULT_WORKERS = 256;
	
	private File conf;
	private InetSocketAddress address;
	private ArrayList<String> filter = new ArrayList<String>();
	private PropertyChangeSupport pcs;
	private int shutdownPort;
	private int[] sampleRate;
	private int samples;
	private int maxWorkers;

	/**
	 * Create a new Configuration using the given config file.
	 * 
	 * @param configFile
	 */
	public Configuration(String configFile) {
		shutdownPort = DEFAULT_SHUTDOWN_PORT;
		conf = new File(configFile == null ? DEFAULT_CONFIG : configFile);
		reconfigure();
	}
	
	/**
	 * Get the configured socket address, where the server should bind to.
	 * @return <code>null</code> if none is configured, the configured address
	 * 		otherwise.
	 */
	public InetSocketAddress getAddress() {
		return address;
	}
	
	/**
	 * Get the list of configured filters.
	 * <p>
	 * Format: $javaClassName;$param
	 * 
	 * @return a possibly empty list of filters
	 */
	public String[] getFilters() {
		return filter.toArray(new String[filter.size()]);
	}
	
	private InetSocketAddress getAddress(XMLStreamReader in) 
		throws XMLStreamException 
	{
		String aPort = in.getAttributeValue(null, "port");
		String hostname = in.getAttributeValue(null, "host");
		int port = 0;
		if (aPort != null) {
			try {
				port = Integer.parseInt(aPort, 10);
			} catch (Exception e) {
				// handle later
			}
			if (port <= 0 || port >= 0xffff) {
				throw new  XMLStreamException("Invalid port '" + aPort
					+ "' found in config file", in.getLocation());
			}
		} else {
			port = DEFAULT_PORT;
		}
		return hostname == null || hostname.isEmpty() || hostname.equals("*") 
			? new InetSocketAddress(port)
			: new InetSocketAddress(hostname, port);
	}

	private void addFilter(XMLStreamReader in, ArrayList<String> filters) 
		throws XMLStreamException 
	{
		String aClass = in.getAttributeValue(null, "class");
		String aConf = in.getAttributeValue(null, "conf");
		if (aClass == null) {
			aClass = "";
		} else {
			aClass = aClass.trim();
		}
		if (aClass.length() == 0) {
			log.warn("missing 'class' attribute for 'filter' at "
				+ Misc.xmlLocation2string(in.getLocation()));
		} else {
			filters.add(aClass + ";" + (aConf == null ? "" : aConf.trim()));
		}
		Misc.fastForwardToEndOfElement(in);
	}

	private void reconfigure() {
		log.info("Reading config file " + conf.getAbsolutePath());
		StreamSource src = null; 
		try {
			src = Misc.getInputSourceByFile(conf, false);
		} catch (IOException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", e);
			}
			return;
		}
		XMLStreamReader reader = Misc.getReader(src, "config", false);
		if (reader == null) {
			return;
		}
		ArrayList<String> newfilters = new ArrayList<String>();
		InetSocketAddress addr = null;
		int port = DEFAULT_SHUTDOWN_PORT;
		try {
			addr = getAddress(reader);
			String aPort = reader.getAttributeValue(null, "shutdown");
			try {
				port = Integer.parseInt(aPort, 10);
			} catch (Exception e) {
				port = DEFAULT_SHUTDOWN_PORT;
			}
			try {
				samples = Integer.parseInt(reader
					.getAttributeValue(null, "samples"), 10);
				if (samples < 1) {
					samples = DEFAULT_SAMPLES;
				}
			} catch (Exception e) {
				samples = DEFAULT_SAMPLES;
			}
			setSampleRates(reader.getAttributeValue(null, "samplerates"));
			String tmp = reader.getAttributeValue(null, "workers");
			int workers = -1;
			try {
				workers = Integer.parseInt(tmp,10);
			} catch (Exception e) {
				// ignore
			}
			maxWorkers = workers > 0 ? workers : DEFAULT_WORKERS;
			while (reader.hasNext()) {
				int res = reader.next();
				if (res == XMLStreamConstants.END_ELEMENT) {
					break;
				}
				if (res == XMLStreamConstants.START_ELEMENT) {
					tmp = reader.getLocalName();
					if (tmp.equals("filter")) {
						addFilter(reader, newfilters);
					} else {
						log.warn("Unknown element '" + tmp + "' ignored");
						Misc.fastForwardToEndOfElement(reader);
					}
				}
			}
		} catch (XMLStreamException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", e);
			}
			return;
		} catch (Exception x) {
			log.warn(x.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", x);
			}
			return;
		} finally {
			try { reader.close(); } catch (Exception e) { /* ignore */ }
			try { src.getInputStream().close(); } catch (Exception e) { 
				/* some readers do not close the underlying stream */ 
			}
		}
		if (!addr.equals(address)) {
			InetSocketAddress old = addr;
			address = addr;
			if (pcs != null) {
				pcs.firePropertyChange(SOCKET_CHANGED, old, addr);
			}
		}
		if (port != shutdownPort) {
			int oldPort = shutdownPort;
			shutdownPort = port;
			if ( pcs != null) {
				pcs.firePropertyChange(SHUTDOWN_CHANGED, oldPort, shutdownPort);
			}
		}
		if (newfilters.size() == 0) {
			log.warn("no filters found");
		}
		if (newfilters.size() != filter.size()) {
			newfilters.trimToSize();
			ArrayList<String> old = filter;
			filter = newfilters;
			if (pcs != null) {
				pcs.firePropertyChange(FILTER_CHANGED, old, newfilters);
			}
		}
		log.info("configuration update done");
	}
	
	private void setSampleRates(String param) {
		if (param == null || param.length() == 0) {
			sampleRate = DEFAULT_SAMPLE_RATES;
			return;
		}
		String tmp[] = param.split(",");
		TreeSet<Integer> vals = new TreeSet<Integer>();
		for (int i=tmp.length-1; i >= 0; i++) {
			int factor = 1;
			tmp[i] = tmp[i].trim().toLowerCase();
			try {
				boolean suffix = false;
				if (tmp[i].endsWith("s")) {
					suffix = true;
				} else if (tmp[i].endsWith("m")) {
					suffix = true;
					factor = 60;
				} else if (tmp[i].endsWith("h")) {
					suffix = true;
					factor = 60 * 60;
				} else if (tmp[i].endsWith("d")) {
					suffix = true;
					factor = 60 * 60 * 24;
				} else if (tmp[i].endsWith("w")) {
					suffix = true;
					factor = 60 * 60 * 24 * 7;
				}
				if (suffix) {
					tmp[i] = tmp[i].substring(0, tmp[i].length()-1);
				}
				int val = factor * Integer.parseInt(tmp[i], 10);
				vals.add(new Integer(val));
			} catch (Exception e) {
				log.warn("Invalid intervall value tmp[i] ignored");
				if (log.isDebugEnabled()) {
					log.debug("setIntervalls", e);
				}
			}
		}
		if (vals.size() == 0) {
			log.info("Using default intervalls for statistics collections");
			sampleRate = DEFAULT_SAMPLE_RATES;
			return;
		}
		sampleRate = new int[vals.size()];
		for (int i=vals.size()-1; i >= 0; i--) {
			sampleRate[i] = vals.pollLast().intValue();
		}
	}

	/**
	 * Add a the given configuration listener.
	 * @param listener	listener to add
	 */
	public synchronized void add(PropertyChangeListener listener) {
		if (listener == null) {
			return;
		}
		if (pcs == null) {
			pcs = new PropertyChangeSupport(this);
		}
		pcs.addPropertyChangeListener(listener);
	}

	/**
	 * Remove the given configuration listener
	 * @param listener	listener to remove
	 */
	public synchronized void remove(PropertyChangeListener listener) {
		if (listener == null || pcs == null) {
			return;
		}
		pcs.removePropertyChangeListener(listener);
	}	
	
	/**
	 * Just for debugging purposes.
	 * 
	 * @param args	configuration file (optional)
	 */
	public static void main(String[] args) {
		Configuration conf = new Configuration(args.length > 0 ? args[0] : null);
		System.out.println("Socket = " + conf.address);
		for (String f : conf.filter) {
			System.out.println(f.replace(";", ": "));
		}
	}

	/**
	 * Get the shutdown port of the server
	 * @return the shutdownPort.
	 */
	public InetSocketAddress getShutdownAddress() {
		return new InetSocketAddress("localhost", shutdownPort);
	}
	
	/**
	 * Get the number of samples, which should be kept for connection statistics.
	 * @return always something &gt; <code>0</code>
	 */
	public int getSamples() {
		return samples;
	}
	
	/**
	 * Get history collection intervalls aka sample rates for connection 
	 * statistics.
	 * @return always an array with a size &gt; <code>0</code>
	 */
	public int[] getSampleRates() {
		return Arrays.copyOf(sampleRate, sampleRate.length);
	}
	
	/**
	 * Get the max. number of workers in the thread pool to be used.
	 * @return always a value &gt; 0
	 * @see #DEFAULT_WORKERS
	 */
	public int getMaxWorkers() {
		return maxWorkers;
	}
}
