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
 * Inform the MTA, that the message was quarantined for the given reason.
 * <p>
 * Requires {@link Modification#QUARANTINE} negotiation.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class QuarantinePacket
	extends Packet
{
	String reason;

	/**
	 * Create the packet.
	 * @param reason	The mail-safe quarantine reason.
	 * @throws IllegalArgumentException if reason is <code>null</code> or empty
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public QuarantinePacket(String reason) {
		super(Type.QUARANTINE);
		this.reason = reason == null ? "" : reason.trim();
		if (this.reason.length() == 0) {
			throw new IllegalArgumentException("null or empty reason not allowed");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(reason.length() + 1);
		bos.write(Misc.getBytes(reason));
		bos.write(0);
		return bos.toByteArray();
	}
}
