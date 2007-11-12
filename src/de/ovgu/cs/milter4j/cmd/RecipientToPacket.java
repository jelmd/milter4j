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

import java.nio.ByteBuffer;

/**
 * Sends SMTP RCPT command info to milter filters
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class RecipientToPacket
	extends Command
{
	private String recipient;

	/**
	 * Create the packet.
	 * @param data	raw data received
	 */
	public RecipientToPacket(ByteBuffer data) {
		super(Type.RCPT);
		recipient = getString(null, data).toString();
	}
	
	/**
	 * Get the value of the 'RCPT TO:' command, the mail-client issued
	 * @return the recipient value of the envelope
	 */
	public String getRecipient() {
		return recipient;
	}
}
