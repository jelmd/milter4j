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

import java.util.Date;

import javax.management.openmbean.TabularData;

import de.ovgu.cs.milter4j.StatsCollector;



/**
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public interface ServerMBean {
	
	/**
	 * Get the number of instantiated workers
	 * @return the number of pooled workers
	 */
	public int getWorkers();

	/**
	 * Get a list of all configured filters
	 * @return a possible empty list
	 */
	public String[] getFilterNames();

	/**
	 * Get the time, when this server has ben started.
	 * @return	always a none-null value
	 */
	public Date getStartTime();

	/**
	 * Get the statistic sample rates currently in action.
	 * @return all sample rates in milliseconds
	 */
	public long[] getSampleRates();

	/**
	 * Convinience method for {@link #getHistory(int, boolean)} with a value of 
	 * <code>0</code> and <code>true</code>.
	 * @return the connections history collection with the smallest sample rate
	 */
	public TabularData getHistory();

	/**
	 * Get a history collection about number of connections since start time
	 * as time;value pair.
	 * 
	 * @param idx	the idx of the collection to retrieve
	 * @param relative if <code>true</code> of the start of the intervall as 
	 * 		base for the returned corresponding value, the start time of
	 * 		the server otherwise.
	 * @return <code>null</code> if not available, the collection otherwise.
	 * 
	 * @see StatsCollector#getHistory(int, boolean)
	 */
	public TabularData getHistory(int idx, boolean relative);

	/**
	 * Get the version of this product.
	 * @return a multi-lined, human-readable version info
	 */
	public String getVersion();
	/**
	 * Get the number of connections since the start of this collector.
	 * @return	number of connections
	 */
	public long getConnections();

	/**
	 * Shutdown the server gracefully
	 */
	public void shutdown();
	
	/**
	 * Lsit the current config file in use.
	 * @return the content of the config file as plain text
	 */
	public String listConfig();
	
	/**
	 * Reconfigure the service by re-reading its config file
	 */
	public void reconfigure();
}
