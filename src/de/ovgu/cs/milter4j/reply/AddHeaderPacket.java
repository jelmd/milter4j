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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sendmail.milter.Modification;
import com.sendmail.milter.util.Misc;

/**
 * Adds a header to the current message.
 * <p>
 * Neither the name nor the value of the header is checked for standards 
 * compliance. However, each line of the header must be under 2048 characters 
 * and should be under 998 characters. If longer headers are needed, make them 
 * multi-line. To make a multi-line header, insert a line feed (ASCII 0x0a, or 
 * \n in C) followed by at least one whitespace character such as a space 
 * (ASCII 0x20) or tab (ASCII 0x09, or \t in C). The line feed should NOT be 
 * preceded by a carriage return (ASCII 0x0d); the MTA will add this 
 * automatically. <em>It is the filter writer's responsibility to ensure that no 
 * standards are violated.</em>
 * <p>
 * The MTA adds a leading space to an added header value.
 * <p>
 * Filter order is important. Later filters will see the header changes made by 
 * earlier ones.
 * <p>
 * Requires {@link Modification#ADDHDRS} negotiation.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AddHeaderPacket
	extends Packet
{
	private String name;
	private String value;
	
	/**
	 * Create the packet with an empty header value.
	 * @param name 	The header name.
	 * @throws IllegalArgumentException if the name parameter is <code>null</code>
	 * 		or an empty string
	 */
	public AddHeaderPacket(String name) {
		this(name, "");
	}
	
	/**
	 * Create the packet.
	 * @param name 	The mail-safe header name
	 * @param value The mail-safe header value to be added. If <code>null</code>, 
	 * 		it is automatically set to an empty String.
	 * @throws IllegalArgumentException if the name parameter is <code>null</code>
	 * 		or an empty string
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public AddHeaderPacket(String name, String value) {
		super(Type.ADDHEADER);
		this.name = name == null ? "" : name.trim();
		if (this.name.length() == 0) {
			throw new IllegalArgumentException("empty header name not allowed");
		}
		this.value = value == null ? "" : value.trim();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(name.length()+ value.length() + 2);
		bos.write(Misc.getBytes(name));
		bos.write(0);
		bos.write(Misc.getBytes(value));
		bos.write(0);
		return bos.toByteArray();
	}
}
