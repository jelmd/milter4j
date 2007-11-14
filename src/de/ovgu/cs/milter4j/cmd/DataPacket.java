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

import de.ovgu.cs.milter4j.util.Misc;

/**
 * Sends the data start notification to a filter
 *  
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class DataPacket
	extends Command
{
	byte[] chunk;

	/**
	 * Create the packet
	 * @param data	raw data received.
	 */
	public DataPacket(ByteBuffer data) {
		super(Type.DATA);
		int count = data.remaining();
		if (count != 0) {
			chunk = new byte[count];
			data.get(chunk);
		} else {
			chunk = Misc.ZERO_DATA;
		}
	}

	/**
	 * Get the data received.
	 * @return the received data, which is usually an empty array.
	 */
	public byte[] getData() {
		return chunk;
	}
}
