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
 * Notifies the MTA that the filter is still working on a message, causing the 
 * MTA to re-start its timeouts.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ProgressPacket
	extends SimplePacket
{
	/**
	 * Create the packet.
	 */
	public ProgressPacket() {
		super(Type.PROGRESS);
	}
}
