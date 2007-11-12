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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.sendmail.milter.util.Misc;

/**
 * A paket, which can be send as a reply to a MTA milter command paket.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public abstract class Packet {
	private Type type;

	/** helper to avoid type casting */
	public static final byte ZERO_BYTE = '\0';

	/**
	 * Create a packet of the given type.
	 * @param type	type of the reply packet.
	 */
	protected Packet(Type type) {
		if (type == null) {
			throw new IllegalArgumentException("null not allowed");
		}
		this.type = type;
	}
	
	/** 
	 * Get the raw data section of this packet.
	 * 
	 * @return <code>null</code> if no additional data have to be sent,
	 * 		the data buffer to sent otherwise.
	 * @throws IOException if data could not be coverted into
	 * 		appropriate byte codes
	 */
	public abstract byte[] getData() throws IOException;
	
	/**
	 * Get the type of the packet.
	 * @return the packet type.
	 */
	public Type getType() {
		return type;
	}
	
	private ByteBuffer buf;
	
	/**
	 * Send the paket to the given channel.
	 * 
	 * @param ch		channel to use for sending
	 * @return <code>true</code> if the complete packet could be sent. If not,
	 * 		one should invoke this method, until it returns <code>true</code>.
	 * @throws IOException if an I/O error occurs
	 * @see WritableByteChannel#write(ByteBuffer)
	 */
	public boolean send(WritableByteChannel ch) throws IOException {
		if (buf == null) {
			byte[] data = getData();
			if (data == null || data.length == 0) {
				data = Misc.ZERO_DATA;
			}
			buf = ByteBuffer.allocate(4 + 1 + data.length);
			buf.putInt(1 + data.length);
			buf.put(type.getCode());
			if (data.length > 0) {
				buf.put(data);
			}
			buf.flip();
		}
		ch.write(buf);
		return !buf.hasRemaining();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return type == null ? "null" : type.name();
	}
}
