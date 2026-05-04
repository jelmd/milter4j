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
import java.util.ArrayList;

/**
 * Sends SMTP MAIL command info to milter filters
 * <p>
 * message-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class MailFromPacket
	extends Command
{
	private String[] from;
	
	/**
	 * Create the packet.
	 * @param data	raw data received
	 */
	public MailFromPacket(ByteBuffer data) {
		super(Type.MAIL);
		ArrayList<String> args = new ArrayList<>();
		StringBuilder name = new StringBuilder(32);
		while (data.hasRemaining()) {
			name.setLength(0);
			getString(name, data);
			if (name.length() > 0) {
				args.add(name.toString());
			}
		}
		from = args.toArray(new String[args.size()]);
	}
	
	/**
	 * Get the content of the 'MAIL FROM:' command, sent by a mail-client.
	 * @return the from value of the envelope.
	 */
	public String[] getFrom() {
		return from;
	}
}
