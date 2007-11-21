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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import de.ovgu.cs.milter4j.ServerMBean;

/**
 * An extension to JConsole to display tabular data of simple types correctly.
 * 
 * @author Jens Elkner
 * @version $Revision$
 */
public class FilterStatsConsole
	extends JPanel
{
	private static final long serialVersionUID = 2146964076441935247L;

	static final Logger log = Logger.getLogger(FilterStatsConsole.class.getName());
	MBeanServerConnection server;
	ServerMBean sbean;
	DefaultComboBoxModel filterComboModel;
	JComboBox filterCombo;
	private JButton filterUpdateButton;
	private JButton statsUpdateButton;
	JScrollPane statsPane;
	JTable statsTable;
	boolean ignoreUpdates;

	/**
	 * Create a new JPanel, which displays stats.
	 */
	public FilterStatsConsole() {
		super(new BorderLayout());
		filterComboModel = new DefaultComboBoxModel(new String[0]);
		filterCombo = new JComboBox(filterComboModel);
		filterCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateStatsTable();
			}
		});
		statsPane = new JScrollPane();
		statsUpdateButton = new JButton("Update");
		statsUpdateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateStatsTable();
			}
		});
		filterUpdateButton = new JButton("Update");
		filterUpdateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilterCombo();
			}
		});
		JPanel top = new JPanel(new FlowLayout());
		top.add(filterCombo);
		top.add(filterUpdateButton);
		add(top, BorderLayout.NORTH);
		add(statsPane, BorderLayout.CENTER);
		add(statsUpdateButton, BorderLayout.SOUTH);
	}

	void updateFilterCombo() {
		SwingWorker<String[], Object> w = 
			new SwingWorker<String[], Object>() 
		{
			@Override
			protected String[] doInBackground() throws Exception {
				return sbean.getFilterNames();
			}
			
			@Override
			protected void done() {
				String[] fNames = null;
				try {
					fNames = get();
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "done", e);
					}
					return;
				}
				boolean needUpdate = fNames.length != filterComboModel.getSize();
				if (!needUpdate) {
					for (String name : fNames) {
						if (filterComboModel.getIndexOf(name) < 0) {
							needUpdate = true;
							break;
						}
					}
				}
				if (!needUpdate) {
					return;
				}
				ignoreUpdates = true;
				filterComboModel.removeAllElements();
				String selected = (String) filterComboModel.getSelectedItem();
				int idx = -1;
				for (int i = 0; i < fNames.length; i++ ) {
					String name = fNames[i];
					filterComboModel.addElement(name);
					if (selected != null && name.equals(selected)) {
						idx = i;
					}
				}
				if (idx != -1) {
					filterCombo.setSelectedIndex(idx);
					ignoreUpdates = false;
					return;
				}
				// trigger a stats table update
				ignoreUpdates = false;
				filterCombo.setSelectedIndex(0);
			}
		};
		w.execute();
	}

	/**
	 * Update stats table (check filterCombo, whether selection was changed,
	 * too)
	 */
	void updateStatsTable() {
		if (ignoreUpdates) {
			return;
		}
		final Object o = filterCombo.getSelectedItem();
		if (o == null) {
			return;
		}
		SwingWorker<DefaultTableModel, Object> w = 
			new SwingWorker<DefaultTableModel, Object>() 
		{
			@Override
			protected DefaultTableModel doInBackground() throws Exception {
				FilterStatsMBean bean = null;
				try {
					ObjectName name = 
						new ObjectName("Milter4J:type=FilterStats,name="
							+ o.toString());
					bean = JMX.newMBeanProxy(server, name, 
						FilterStatsMBean.class, false);
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "setMBeanServerConnection", e);
					}
					return null;
				}
				TabularData td = bean.getStats();
				TabularType tt = td.getTabularType();
				List<String> idxNames = tt.getIndexNames();
				CompositeType ct = tt.getRowType();
				Set<String> headers = ct.keySet();
				boolean allSimple = true;
				List<String> tHeaders = new ArrayList<String>();
				tHeaders.addAll(idxNames);
				for (String name : idxNames) {
					if (! (ct.getType(name) instanceof SimpleType)) {
						allSimple = false;
					}
				}
				for (String name : headers) {
					if (!idxNames.contains(name)) {
						tHeaders.add(name);
					}
					if (! (ct.getType(name) instanceof SimpleType)) {
						allSimple = false;
					}
				}
				if (log.isLoggable(Level.FINE)) {
					StringBuilder buf = new StringBuilder("indices: ");
					for (String s : idxNames) {
						buf.append(s).append(",");
					}
					buf.setLength(buf.length()-1);
					log.fine(buf.toString());
					buf.setLength(0);
					buf.append("headers: ");
					for (String s : headers) {
						buf.append(s).append(",");
					}
					buf.setLength(buf.length()-1);
					buf.append(" (");
					if (!allSimple) {
						buf.append("not ");
					}
					buf.append("all simple types)");
					log.fine(buf.toString());
				}
				if (!allSimple) {
					log.severe("data ignored since not all columns are of SimpleType");
					return null;
				}
				Set<?> keys = td.keySet();
				// generate the table
				Object[][] vals = new Object[keys.size()][headers.size()];
				String[] tHead = tHeaders.toArray(new String[tHeaders.size()]);
				Object[] allKeys = keys.toArray();
				for (int i=0; i < allKeys.length; i++) {
					Object[] key = ((List<?>) allKeys[i]).toArray();
					CompositeData data = td.get(key);
					for (int k=0; k < tHead.length; k++) {
						vals[i][k] = data.get(tHead[k]);
					}
				}
				return new DefaultTableModel(vals, tHead);
			}
			
			@Override
			protected void done() {
				DefaultTableModel dtm = null;
				try {
					dtm = get();
				} catch (Exception e) {
					log.severe(e.getLocalizedMessage());
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "done", e);
					}
				}
				if (dtm == null) {
					return;
				}
				if (statsTable == null) {
					statsTable = new JTable(dtm);
					statsTable.setAutoCreateRowSorter(true);
					statsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
					remove(statsPane);
					statsPane = new JScrollPane(statsTable);
					add(statsPane, BorderLayout.CENTER);
				} else {
					statsTable.setModel(dtm);
				}
				doLayout();
			}
		};
		w.execute();
	}

	/**
	 * Set the MBeanServerConnection object for communicating with the target VM
	 * 
	 * @param mbs
	 *            connection to use to get MXBean Refs
	 */
	public void setMBeanServerConnection(MBeanServerConnection mbs) {
		server = mbs;
		try {
			ObjectName name = new ObjectName("Milter4J:type=Server");
			sbean = JMX.newMBeanProxy(server, name, ServerMBean.class, false);
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "setMBeanServerConnection", e);
			}
		}
	}

	private static void usage() {
		System.out.println("Usage: java FilterStatsConsole <hostname>:<port>");
		System.exit(1);
	}

	/**
	 * Establish an RMI connection with the remote JVM
	 * 
	 * @param hostname
	 *            machine to connect to
	 * @param port
	 *            port, at which jconsole/mbeanserver is listening
	 */
	private static MBeanServerConnection connect(String hostname, int port) {
		String urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
		MBeanServerConnection server = null;
		try {
			JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
			JMXConnector jmxc = JMXConnectorFactory.connect(url);
			server = jmxc.getMBeanServerConnection();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "connect", e);
			}
			System.exit(1);
		}
		return server;
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	static void createAndShowGUI(JPanel panel) {
		JFrame frame = new JFrame("FilterStatsConsole");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JComponent contentPane = (JComponent) frame.getContentPane();
		contentPane.add(panel, BorderLayout.CENTER);
		contentPane.setOpaque(true); // content panes must be opaque
		contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
		frame.setContentPane(contentPane);
		frame.setSize(new Dimension(800, 600));
		frame.validate();
		frame.setVisible(true);
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			usage();
		}
		String[] arg2 = args[0].split(":");
		if (arg2.length != 2) {
			usage();
		}
		String hostname = arg2[0];
		int port = -1;
		try {
			port = Integer.parseInt(arg2[1]);
		} catch (NumberFormatException x) {
			usage();
		}
		if (port < 0) {
			usage();
		}
		final FilterStatsConsole cons = new FilterStatsConsole();
		MBeanServerConnection server = connect(hostname, port);
		cons.setMBeanServerConnection(server);
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				cons.updateFilterCombo();
			}
		};
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				createAndShowGUI(cons);
			}
		});
		// refresh every 60 seconds
		Timer timer = new Timer("Console update thread");
		timer.schedule(timerTask, 0, 60000);
	}
}
