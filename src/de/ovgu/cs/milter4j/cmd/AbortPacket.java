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
 * Instruction to abort the current message. More messages from the same mail 
 * client may followe, so don't discard CONNECT/HELO information, if required
 * for processing.
 * <p>
 * message-oriented
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AbortPacket
	extends Command
{
	/** Create the paket */
	public AbortPacket() {
		super(Type.ABORT);
	}
}
