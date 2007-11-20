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
	 * Get a history collection about number of connections since start time
	 * as time;value pair.
	 * 
	 * @param idx	the idx of the collection to retrieve
	 * @return <code>null</code> if not available, the collection otherwise.
	 * 
	 * @see StatsCollector#getHistory(int)
	 */
	public Long[][] getHistory(int idx);

	/**
	 * Shutdown the server gracefully
	 */
	public void shutdown();
}
