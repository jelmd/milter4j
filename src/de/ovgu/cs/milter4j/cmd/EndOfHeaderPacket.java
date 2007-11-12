/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.cmd;

/**
 * Info, that all available headers have been sent by the MTA.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class EndOfHeaderPacket
	extends Command
{
	/**
	 * Create the packet.
	 */
	public EndOfHeaderPacket() {
		super(Type.EOH);
	}
}
