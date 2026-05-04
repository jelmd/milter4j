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
 * A single header cmd packet.
 * <p>
 * message-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class HeaderPacket
	extends Command
{
	private String name;
	private String value;

	/**
	 * Create the packet.
	 * @param data	raw data received.
	 */
	public HeaderPacket(ByteBuffer data) {
		super(Type.HEADER);
		// per RFC 2822, 2.2.  ASCII chars only, so casting byte -> char is OK
		if (data.hasRemaining()) {
			StringBuilder dst = getString(null, data);
			name = dst.toString();
			dst.setLength(0);
			value = getString(dst, data).toString();
		}
	}

	/**
	 * Get the header name
	 * @return the name of the header.
	 */
	public String getName() {
		return name;
	}

	/**
	 * The value of the header
	 * @return the value, which might be an empty String.
	 */
	public String getValue() {
		return value;
	}

}
