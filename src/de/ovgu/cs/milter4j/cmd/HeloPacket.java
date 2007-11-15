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
 * Sends SMTP HELO/EHLO command info to milter filters
 * <p>
 * HELO/EHLO can come at any point
 * <p>
 * connection-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class HeloPacket
	extends Command
{
	String domain;
	
	/**
	 * Create the packet
	 * @param data	raw data received
	 */
	public HeloPacket(ByteBuffer data) {
		super(Type.HELO);
		domain = getString(null, data).toString();
	}

	/**
	 * Get the domain (or whatever the mail-client sent) via HELO/EHLO
	 * @return the domain string.
	 */
	public String getDomain() {
		return domain;
	}
}
