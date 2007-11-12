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
 * Removes the named recipient from the current message's envelope.
 * <p>
 * Requires {@link Modification#DELRCPT} negotiation.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class DeleteRecipientPacket
	extends Packet
{
	String recipient;
	
	/**
	 * Create the packet.
	 * @param recipient		mail-safe recipient to remove from the message's envelope. The 
	 * 	addresses to be removed must match exactly. For example, an address and 
	 * 	its expanded form do not match.
	 * @throws IllegalArgumentException if recipient is <code>null</code>
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public DeleteRecipientPacket(String recipient) {
		super(Type.DELRCPT);
		this.recipient = recipient == null ? "" : recipient;
		if (this.recipient.length() == 0) {
			throw new IllegalArgumentException("empty recipient not allowed");
		}
		this.recipient = recipient;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(recipient.length()+1);
		bos.write(Misc.getBytes(recipient));
		bos.write(0);
		return bos.toByteArray();
	}
}
