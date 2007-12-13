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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

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
	
	private final ReentrantLock lock = new ReentrantLock();
	private ArrayList<ArrayDeque<Long>> history;
	private ArrayList<ArrayDeque<Long>> lastTime;
	private long[] intervall;
	
	private long startTime;
	private int connections;
	private Timer timer;
	private TimerTask timerTask;
	private int limit;
	
	private static String[] HIST_NAMES = new String[] { 
		"Time", 
		"Connections"
	};
	private static String[] HIST_DESC = new String[] {
		"Time in milliseconds since 01.01.1970 0:00 UTC", 
		"Number of Connections established since application start" 
	};
	private static OpenType<?>[] HIST_TYPES = new OpenType[] { 
		SimpleType.DATE, 
		SimpleType.LONG
	};
	private static CompositeType HIST_TYPE;
	private static TabularType HIST_TABLE_TYPE;
	
	static {
		try {
			HIST_TYPE = new CompositeType(
				"HistoryEntry",
				"A time,value pair for connection history",
				HIST_NAMES, HIST_DESC, HIST_TYPES);
			HIST_TABLE_TYPE = new TabularType("History", "a history table", 
				HIST_TYPE, new String[] { HIST_NAMES[0] });
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("enclosing_method", e);
			}
		}
	}
	
	/**
	 * Default constructor.
	 * <p>
	 * If you care about memory - roughly the allocation rules for history:
	 * {@code <var>collectionTimes.length</var> x <var>samples</var> x 4 x 4 x4} 
	 * whereby <var>sample</var> gets rounded to the next power of two value
	 * minus 1, if it doesn't already represent a 2<sup>n</sup> value. 
	 * 
	 * @param collectionTimes history collection intervalls wrt. connections
	 * @param samples	hint, how much samples to keep for each intervall 
	 * 		(should be 2<sup>n</sup>-1)
	 */
	public StatsCollector(int[] collectionTimes, int samples) {
		// no API to retrieve the internal capacity of deque. But since element
		// pointers are allocated anyway, we use samples just as a hint
		limit = samples < 8 
			? 7 
			: (samples < Integer.MAX_VALUE ? samples : Integer.MAX_VALUE-1);
		limit |= (limit >>>  1);
		limit |= (limit >>>  2);
		limit |= (limit >>>  4);
		limit |= (limit >>>  8);
		limit |= (limit >>> 16);
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
			int intervalls = collectionTimes.length - count;
			if (intervalls < 1) {
				intervalls = 1;
				collectionTimes = new int[] { Integer.MAX_VALUE };
			}	
			intervall = new long[intervalls];
			history = new ArrayList<ArrayDeque<Long>>(intervalls);
			lastTime = new ArrayList<ArrayDeque<Long>>(intervalls);
			Long now = new Long(System.currentTimeMillis());
			Long zero = Long.valueOf(0);
			for (int i=0; i < intervalls; i++, count++) {
				intervall[i] = collectionTimes[count] * 1000;
				ArrayDeque<Long> q = new ArrayDeque<Long>(limit);
				history.add(q);
				q.push(zero);
				ArrayDeque<Long> q2 = new ArrayDeque<Long>(limit);
				lastTime.add(q2);
				q2.push(now);
			}
			timerTask = new TimerTask() {
				@Override
				public void run() {
					doStats(false);
				}
			};
			timer = new Timer("HistoryCollector");
			timer.schedule(timerTask, 0, intervall[0]);
		}
	}
	
	void doStats(boolean all) {
		long now = System.currentTimeMillis();
		Long cons = new Long(connections);
		Long nowL = new Long(now);
		// always need to update at least two queues at a time in sync
		lock.lock();
		try {
			for (int i=0; i < lastTime.size(); i++) {
				ArrayDeque<Long> tq = lastTime.get(i);
				if (!all && now - tq.peekFirst().longValue() < intervall[i]) {
					break;
				}
				ArrayDeque<Long> hq = history.get(i);
				if (hq.size() == limit) {
					hq.pollLast();
					tq.pollLast();
				}
				hq.addFirst(cons);
				tq.addFirst(nowL);
			}
		} finally {
			lock.unlock();
		}
		
	}
	
	/**
	 * Shutdown this instance, i.e. stop the collecting thread and add a 
	 * stats for all intervalls a last time, not matter, whether the intervall
	 * limit has been reached.
	 */
	public void shutdown() {
		timerTask.cancel();
		timer.cancel();
		doStats(true);
	}
	
	/**
	 * Increment the counter for connections.
	 */
	public void addConnection() {
		connections++;
	}
	
	/**
	 * Get the number of connections since the start of this collector.
	 * @return	number of connections
	 */
	public long getConnections() {
		return connections;
	}
	
	/**
	 * Get the connection history values for the given intervall
	 * @param idx the index of the history collection to return, <code>0</code> 
	 * 		is corresponds to the collection associated with the first aka 
	 * 		smallest intervall. 
	 * @param relative	if <code>false</code>, return absolute values (number
	 * 		of connections since start time) instead of relative values (number 
	 * 		of connections since the start of the intervall). 
	 * @return	<code>null</code> if <var>idx</var> is out of range, the
	 * 		related collection otherwise.
	 */
	public TabularData getHistory(int idx, boolean relative) {
		if (idx < 0 || idx >= intervall.length) {
			return null;
		}
		Long[] h = null;
		Long[] t = null;
		lock.lock();
		try {
			int size = history.get(idx).size();
			h = history.get(idx).toArray(new Long[size]);
			t = lastTime.get(idx).toArray(new Long[size]);
		} finally {
			lock.unlock();
		}
		TabularData data = new TabularDataSupport(HIST_TABLE_TYPE);
		CompositeData[] cd = new CompositeData[h.length];
		long prev = 0;
		for (int i=h.length-1; i >= 0; i--) {
			Object[] vals = new Object[2];
			vals[0] = new Date(t[i].longValue());
			if (relative) {
				vals[1] = Long.valueOf(h[i].longValue()-prev);
				prev = h[i].longValue();
			} else {
				vals[1] = h[i];
			}
			try {
				cd[i] = new CompositeDataSupport(HIST_TYPE, HIST_NAMES, vals);
			} catch (OpenDataException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
		data.putAll(cd);
		return data;
	}
	
	/**
	 * Get a list of all sample rates currently in action
	 * @return all sample rates in milliseconds
	 */
	public long[] getSampleRates() {
		return Arrays.copyOf(intervall, intervall.length);
	}
	
	/**
	 * Get the time, when this collector has ben started.
	 * @return	always a none-null value
	 */
	public Date getStartTime() {
		return new Date(startTime);
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
			String[] names = stats.keySet().toArray(new String[0]);
			for (int i=names.length-1; i >= 0; i--) {
				remove(names[i], server);
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
