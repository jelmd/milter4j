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
 * Change or delete a message header.
 * <p>
 * While this packet ight be used to add new headers, it is more efficient and 
 * far safer to use {@link AddHeaderPacket}.
 * <p>
 * Requires {@link Modification#CHGHDRS} negotiation.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ChangeHeaderPacket
	extends Packet
{
	String name;
	int ordinal;
	String value;

	/**
	 * Create the packet.
	 * @param ordinal Header index value (1-based). A ordinal value of 1 will 
	 * 		modify the first occurrence of a header with the given name. If 
	 * 		ordinal is greater than the number of times the header with the
	 * 		given name appears, a new header will be added.
	 * @param name The mail-safe header name.
	 * @param value The new mail-safe value of the given header. <code>null</code>
	 * 		implies that the header should be deleted.
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public ChangeHeaderPacket(int ordinal, String name, String value) {
		super(Type.CHGHEADER);
		this.name = name == null ? "" : name.trim();
		if (name.length() == 0) {
			throw new IllegalArgumentException("header name cannot be empty");
		}
		if (ordinal < 1) {
			throw new IllegalArgumentException("ordinal must be > 0");
		}
		this.ordinal = ordinal;
		this.value = value == null ? null : value.trim();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
		bos.write(Misc.getBytes(ordinal));
		bos.write(Misc.getBytes(name));
		bos.write(0);
		if (value != null) {
			bos.write(Misc.getBytes(value));
			bos.write(0);
		}
		return bos.toByteArray();
	}
}
