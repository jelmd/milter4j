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
 * A simple command received from an MTA.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Command {
	private Type cmd;
	
	/**
	 * Construct the packet.
	 * @param cmd	type of the packet.
	 */
	public Command(Type cmd) {
		if (cmd == null) {
			throw new IllegalArgumentException("null not allowed");
		}
		this.cmd = cmd;
	}
	
	/**
	 * Get the type of the command.
	 * @return never <code>null</code>.
	 */
	public Type getType() {
		return cmd;
	}
	
	/**
	 * Reads remaining bytes from the data buffer and appends it as characters
	 * to the specified destination. Stops at '\0' or limit of the provided data
	 * buffer, whatever is seen first and adjusts the buffer position accordingly. 
	 * @param dst		where to append the result
	 * @param data		where to read from
	 * @return if <var>dst</var> is <code>null</code>, a new allocated 
	 * 		StringBuilder, <var>dst</var> otherwise
	 */
	@SuppressWarnings("static-method")
	protected StringBuilder getString(StringBuilder dst, ByteBuffer data) {
		byte b;
		if (dst == null) {
			dst = new StringBuilder(32);
		}
		while(data.hasRemaining() && (b = data.get()) != 0) {
			dst.append((char) b);
		}
		return dst;
	}
}
