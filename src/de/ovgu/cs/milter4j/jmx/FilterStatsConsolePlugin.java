/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.jconsole.JConsoleContext.ConnectionState;

/**
 * An extension to JConsole to display filter stats properly.
 * 
 * @author Jens Elkner
 * @version $Revision$
 */
public class FilterStatsConsolePlugin
	extends JConsolePlugin
	implements PropertyChangeListener
{
	private FilterStatsConsole jstats = null;
	private Map<String, JPanel> tabs = null;

	/**
	 * Create the Plugin.
	 */
	public FilterStatsConsolePlugin() {
		addContextPropertyChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void propertyChange(PropertyChangeEvent ev) {
		String prop = ev.getPropertyName();
		if (prop == JConsoleContext.CONNECTION_STATE_PROPERTY) {
//			ConnectionState oldState = (ConnectionState) ev.getOldValue();
			ConnectionState newState = (ConnectionState) ev.getNewValue();
			// JConsole supports disconnection and reconnection
			// The MBeanServerConnection will become invalid when
			// disconnected. Need to use the new MBeanServerConnection object
			// created at reconnection time.
			if (newState == ConnectionState.CONNECTED && jstats != null) {
				jstats.setMBeanServerConnection(getContext()
					.getMBeanServerConnection());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, JPanel> getTabs() {
		if (tabs == null) {
			jstats = new FilterStatsConsole();
			jstats.setMBeanServerConnection(getContext()
				.getMBeanServerConnection());
			// want a predictable order of the tabs to be added in JConsole
			tabs = new LinkedHashMap<String, JPanel>();
			tabs.put("Mail Filter", jstats);
		}
		return tabs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SwingWorker<? , ? > newSwingWorker() {
		return null;
	}
}
