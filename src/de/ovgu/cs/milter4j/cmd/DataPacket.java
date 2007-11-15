/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.cmd;

/**
 * Sends SMTP DATA command info to milter filters
 * <p>
 * message-oriented
 *  
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class DataPacket
	extends Command
{
	/**
	 * Create the packet
	 */
	public DataPacket() {
		super(Type.DATA);
	}
}
