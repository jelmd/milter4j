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

import javax.management.openmbean.TabularData;

/**
 * JMX interface for filter stats.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public interface FilterStatsMBean {
	
	/**
	 * Get the statistics for all filter commands received.
	 * @return the stats sorted by the ordinal value of the reply commands.
	 */
	public TabularData getStats();

	/**
	 * Get the name of the filter.
	 * @return the filter's display name
	 */
	public String getName();
}
