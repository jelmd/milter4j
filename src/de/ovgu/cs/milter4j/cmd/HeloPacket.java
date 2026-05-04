/**
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
