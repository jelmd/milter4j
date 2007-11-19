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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.cmd.Type;
import de.ovgu.cs.milter4j.jmx.FilterStats;

/**
 * MailFilter stats collector.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class StatsCollector {
	private static final Logger log = LoggerFactory
		.getLogger(StatsCollector.class);
	private HashMap<String, FilterStats> stats;

	private int[] history;
	private long[] intervall;
	private long[] lastTime;
	private long startTime;
	private int connections;
	private TimerTask timerTask;
	
	/**
	 * Default constructor
	 * @param collectionTimes history collection intervalls wrt. connections
	 */
	public StatsCollector(int[] collectionTimes) {
		stats = new HashMap<String, FilterStats>();
		startTime = System.currentTimeMillis();
		if (collectionTimes != null) {
			Arrays.sort(collectionTimes);
			int count = 0;
			for (;count < collectionTimes.length; count++) {
				if (collectionTimes[count] > 5) {
					break;
				}
			}
			if (count != collectionTimes.length) {
				history = new int[collectionTimes.length-count];
				intervall = new long[collectionTimes.length-count];
				lastTime = new long[collectionTimes.length-count];
				for (int i=0; i < lastTime.length; i++) {
					lastTime[i] = startTime;
				}
				for (int i=0; i < intervall.length; i++, count++) {
					intervall[i] = collectionTimes[count] * 1000;
				}
				timerTask = new TimerTask() {
					@Override
					public void run() {
						doStats();
					}
				};
				Timer timer = new Timer("HistoryCollector");
				timer.schedule(timerTask, 0, intervall[0]);
			}
		}
	}
	
	void doStats() {
		long now = System.currentTimeMillis();
		int cons = connections;
		for (int i=0; i < lastTime.length; i++) {
			if (now - lastTime[i] < intervall[i]) {
				break;
			}
			history[i] = cons - history[i];
			lastTime[i] = now;
		}
	}
	
	/**
	 * Increment the counter for connections.
	 */
	public void addConnection() {
		connections++;
	}
	
	/**
	 * Get all history values
	 * @return	a possible empty array
	 */
	public Integer[] getHistory() {
		if (history == null) {
			return new Integer[0];
		}
		Integer[] h = new Integer[history.length];
		for (int i=history.length-1; i >= 0; i--) {
			h[i] = new Integer(history[i]);
		}
		return h;
	}
	
	/**
	 * Add a new statistics set for the mail filter with the given name.
	 * @param displayName	the display name of the corresponding mail filter
	 * @param server if not <code>null</code>, register the statistics set 
	 * 		with that server
	 * @return <code>false</code> if there is a statistic set for the given
	 * 	name already in place.
	 */
	public boolean add(String displayName, MBeanServer server) {
		if (displayName == null || stats.containsKey(displayName)) {
			return false;
		}
		FilterStats s = new FilterStats(displayName);
		stats.put(displayName, s);
		if (server != null) {
			try {
				server.registerMBean(s, s.getDefaultName());
			} catch (Exception e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
		return true;
	}
	
	/**
	 * Remove the statistics set associated with the given name.
	 * @param displayName	the display name of the corresponding mail filter
	 * @param server	if not <code>null</code>, unregister the statistics set 
	 * 		with that server
	 */
	public void remove(String displayName, MBeanServer server) {
		FilterStats f = stats.remove(displayName);
		if (f != null && server != null) {
			try {
				server.unregisterMBean(f.getDefaultName());
			} catch (Exception e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
	}
	
	/**
	 * Remove all statistic sets.
	 * @param server	if not <code>null</code>, unregister the statistics sets 
	 * 		with that server before removing them
	 */
	public void removeAll(MBeanServer server) {
		if (server != null) {
			for (String name : stats.keySet()) {
				remove(name, server);
			}
		} else {
			stats.clear();
		}
	}
	
	/**
	 * Increment the counter wrt. to the given parameters.
	 * @param displayName	the display name of the mail filter, whoms counter 
	 * 		should be incremented
	 * @param cmd		row selector
	 * @param reply		column selector
	 */
	public void increment(String displayName, Type cmd, 
		de.ovgu.cs.milter4j.reply.Type reply) 
	{
		FilterStats s = stats.get(displayName);
		if (s != null) {
			s.increment(cmd, reply);
		}
	}
}
