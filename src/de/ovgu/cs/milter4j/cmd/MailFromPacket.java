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

import java.nio.ByteBuffer;

/**
 * Sends SMTP MAIL command info to milter filters
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class MailFromPacket
	extends Command
{
	private String from;
	
	/**
	 * Create the packet.
	 * @param data	raw data received
	 */
	public MailFromPacket(ByteBuffer data) {
		super(Type.MAIL);
		from = getString(null, data).toString();
	}
	
	/**
	 * Get the content of the 'MAIL FROM:' command, sent by a mail-client.
	 * @return the from value of the envelope.
	 */
	public String getFrom() {
		return from;
	}
}
