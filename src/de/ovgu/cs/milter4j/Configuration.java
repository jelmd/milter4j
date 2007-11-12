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
 * &lt;config port="4444" host="*"&gt;
 * 		&lt;filter class="org.bla.fahsel.milter.Cool" conf="/etc/cool.conf"/&gt;
 * &lt;/config&gt;
 * </pre>
 * 
 * If the port parameter is missing, the default port {@value #DEFAULT_PORT}
 * will be used. If the host parameter is missing or is "<code>*</code>",
 * the server binds to all interfaces of the host. Otherwise it binds to the 
 * given interface, only.
 * <p>
 * The attribute <code>class</code> of filter element contains the
 * java class name of a mail filter to use. The optional <code>conf</code>
 * attribute may be used to pass an additional parameter (e.g. configuration
 * filename) to the given class, when the server instantiates it.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Configuration {
	private static final Logger log = LoggerFactory
		.getLogger(Configuration.class);

	/** the default mail filter server configuration file */ 
	public static final String DEFAULT_CONFIG = "/etc/milter.conf";
	/** the default port, the mail filter server should listen for connections */
	public static final int DEFAULT_PORT = 4444;
	
	/** property name used to notify config listeners about filter changes */ 
	public static final String FILTER_CHANGED = "filter";
	/** property name used to notify config listeners about socket changes */ 
	public static final String SOCKET_CHANGED = "socket";
	
	private File conf;
	private InetSocketAddress address;
	private ArrayList<String> filter = new ArrayList<String>();
	private PropertyChangeSupport pcs;
	
	/**
	 * Create a new Configuration using the given config file.
	 * 
	 * @param configFile
	 */
	public Configuration(String configFile) {
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
		try {
			addr = getAddress(reader);
			while (reader.hasNext()) {
				int res = reader.next();
				if (res == XMLStreamConstants.END_ELEMENT) {
					break;
				}
				if (res == XMLStreamConstants.START_ELEMENT) {
					String tmp = reader.getLocalName();
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
}
