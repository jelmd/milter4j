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

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.management.ObjectName;
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

/**
 * Class to collect statistics about mail filters.
 * <p>
 * It's a matrix of {@link de.ovgu.cs.milter4j.cmd.Type#values()} x
 * {@link de.ovgu.cs.milter4j.reply.Type#values()} of atomic Integers (so
 * ~ 1.2 KB of data).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class FilterStats implements FilterStatsMBean {
	private static final Logger log = LoggerFactory
		.getLogger(FilterStats.class);
	
	private AtomicIntegerArray[] stats;
	private String name;
	
	private static TabularType FILTER_CMD_TYPE;
	private static CompositeType FILTER_RES_TYPE;
	private static String[] RES_NAMES;
	private static String[] RES_DESC;
	private static OpenType<?>[] RES_TYPES;
	
	private static String[] CMD_NAMES;

	static {
		// rows layout
		de.ovgu.cs.milter4j.reply.Type[] values = 
			de.ovgu.cs.milter4j.reply.Type.values();
		RES_NAMES = new String[values.length+1];
		RES_DESC = new String[values.length+1];
		RES_TYPES = new SimpleType[values.length+1];
		RES_NAMES[0] = "Command";
		RES_DESC[0] = "The name of the comand, for which the given results apply";
		RES_TYPES[0] = SimpleType.STRING;
		for (int i=0; i < values.length; i++) {
			RES_NAMES[i+1] = values[i].name();
			RES_DESC[i+1] = "" + (char) values[i].getCode();
			RES_TYPES[i+1] = SimpleType.INTEGER;
		}
		try {
			FILTER_RES_TYPE = new CompositeType("FilterResult", 
				"Result counter for an mail filter", 
				RES_NAMES, RES_DESC, RES_TYPES);
		} catch (OpenDataException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("constructor", e);
			}
		}

		// table definition		
		try {
			FILTER_CMD_TYPE = new TabularType("FilterResults",
				"Result counters for an mail filter wrt. the command",
				FILTER_RES_TYPE, new String[] { RES_NAMES[0] });
		} catch (OpenDataException e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", e);
			}
		}
		
		Type[] cmds = Type.values();
		CMD_NAMES = new String[cmds.length];
		for (int i=0; i < cmds.length; i++) {
			CMD_NAMES[i] = cmds[i].name();
		}
	}
	

	/**
	 * Setup a stisctics collection
	 * @param name	the name of the filter
	 */
	public FilterStats(String name) {
		if (name == null) {
			throw new IllegalArgumentException("null name not allowed");
		}
		this.name = name;
		Type[] cmds = Type.values();
		de.ovgu.cs.milter4j.reply.Type[] replies = 
			de.ovgu.cs.milter4j.reply.Type.values();
		stats = new AtomicIntegerArray[cmds.length];
		for (int i=cmds.length-1; i >= 0; i--) {
			stats[i] = new AtomicIntegerArray(replies.length);
		}
	}
	
	/**
	 * Get the default JMX object name for this class.
	 * @return an object name
	 */
	public ObjectName getDefaultName() {
		try {
			return new ObjectName("Milter4J:type=FilterStats,name=" + name);
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("getDefaultName", e);
			}
		}
		return null;
	}
	
	/**
	 * Increment the result count for the given command (thread-safe).
	 * 
	 * @param cmd		the command (aka row), for which to increment the result
	 * 					count
	 * @param result	the result count (aka column) to increment
	 * @return the new value of the specified destination (aka cell).
	 */
	public int increment(Type cmd, de.ovgu.cs.milter4j.reply.Type result) {
		if (cmd == null) {
			return 0;
		}
		if (result == null) {
			result = de.ovgu.cs.milter4j.reply.Type.CONTINUE;
		}
		return stats[cmd.ordinal()].incrementAndGet(result.ordinal());
	}

	/**
	 * {@inheritDoc}
	 */
	public TabularData getStats() {
		TabularData data = new TabularDataSupport(FILTER_CMD_TYPE);
		for (int i=0; i < stats.length; i++) {
			HashMap<String, Object> items = new HashMap<String, Object>();
			items.put(RES_NAMES[0], CMD_NAMES[i]);
			AtomicIntegerArray vals = stats[i];
			for (int k=0; k < vals.length(); k++) {
				items.put(RES_NAMES[k+1], Integer.valueOf(vals.get(k)));
			}
			CompositeData cd;
			try {
				cd = new CompositeDataSupport(FILTER_RES_TYPE, items);
				data.put(cd);
			} catch (OpenDataException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		}
		return data;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}
}
