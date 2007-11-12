/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.reply;

/**
 * For a message- or recipient-oriented routine, accept this message, but 
 * silently discard it.
 * <p>
 * Should not be returned by a connection-oriented routine.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class DiscardPacket
	extends SimplePacket
{
	/**
	 * Create a new packet.
	 */
	public DiscardPacket() {
		super(Type.DISCARD);
	}
}
