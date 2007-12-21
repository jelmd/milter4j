/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.util;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import de.ovgu.cs.milter4j.MailFilter;
import de.ovgu.cs.milter4j.StatsCollector;
import de.ovgu.cs.milter4j.cmd.Type;
import de.ovgu.cs.milter4j.jmx.FilterStats;

/**
 * A Filter which does nothing. Might be used to collect basic mail statistics
 * very easy.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 * 
 * @see FilterStats
 * @see StatsCollector
 * @see "jconsole or jManage"
 */
public class NullFilter extends MailFilter {
	
	private static final AtomicInteger instCounter = new AtomicInteger();
	private String name;

	/**
	 * Create the filter, which replies with {@link 
	 * de.ovgu.cs.milter4j.reply.Type#CONTINUE} to all commands.
	 * @param params ignored
	 */
	public NullFilter(String params) {
		name = "RequestDumper " + instCounter.getAndIncrement();
	}
	
	/**
	 * Count all commands except {@link Type#DATA} and {@link Type#MACRO}.
	 * @return {@inheritDoc}
	 */
	@Override
	public EnumSet<Type> getCommands() {
		EnumSet<Type> cmds = EnumSet.allOf(Type.class);
		cmds.remove(Type.MACRO);
		return cmds;
	}

	/**
	 * Does nothing
	 */
	@Override
	public void doAbort() {
		// nothing to do
	}

	/**
	 * Does nothing
	 */
	@Override
	public void doQuit() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MailFilter getInstance() {
		return new NullFilter(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean reconfigure(String param) {
		return true;
	}
}
