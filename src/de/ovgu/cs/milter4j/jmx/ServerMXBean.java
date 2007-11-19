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



/**
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public interface ServerMXBean {
	
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

	public Integer[] getHistory();

	/**
	 * Shutdown the server gracefully
	 */
	public void shutdown();
}
