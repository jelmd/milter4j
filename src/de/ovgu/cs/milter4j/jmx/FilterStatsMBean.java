/**
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
