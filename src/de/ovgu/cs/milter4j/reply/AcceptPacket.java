/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.reply;

/**
 * For a connection-oriented routine, accept this connection without further 
 * filter processing.  For a message- or recipient-oriented routine, accept 
 * this message without further filtering.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AcceptPacket
	extends SimplePacket
{
	/**
	 * Create the paket.
	 */
	public AcceptPacket() {
		super(Type.ACCEPT);
	}
}
