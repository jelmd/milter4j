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
 * Continue processing the current connection, message, or recipient.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ContinuePacket
	extends SimplePacket
{
	/**
	 * Cretae a new packet.
	 */
	public ContinuePacket() {
		super(Type.CONTINUE);
	}

}
