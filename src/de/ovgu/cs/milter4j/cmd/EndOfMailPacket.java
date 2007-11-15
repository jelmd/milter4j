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
 * Signals, the end of mail aka no more body chunks.
 * <p>
 * message-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class EndOfMailPacket
	extends Command
{
	/**
	 * Create the packet.
	 */
	public EndOfMailPacket() {
		super(Type.BODYEOB);
	}
}
