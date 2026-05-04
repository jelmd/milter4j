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

import de.ovgu.cs.milter4j.util.Misc;

/**
 * Sends the body to a filter
 * <p>
 * message-oriented
 *  
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class BodyPacket
	extends Command
{
	byte[] chunk;

	/**
	 * Create the packet
	 * @param data	raw data received.
	 */
	public BodyPacket(ByteBuffer data) {
		super(Type.BODY);
		int count = data.remaining();
		if (count != 0) {
			chunk = new byte[count];
			data.get(chunk);
		} else {
			chunk = Misc.ZERO_DATA;
		}
	}

	/**
	 * Get the chunk of body received.
	 * @return the chunk, which might be the whole body.
	 */
	public byte[] getChunk() {
		return chunk;
	}
}
