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

import com.sendmail.milter.Modification;
/**
 * Replaces the body of the current message. If sent more than once, subsequent 
 * packets result in data being appended to the new body.
 * <p>
 * Filter order is important. Later filters will see the new body contents 
 * created by earlier ones.
 * <p>
 * Since the message body may be very large, setting {@link Modification#CHGBODY}
 * may significantly affect filter performance.
 * <p>
 * Requires {@link Modification#CHGBODY} negotiation.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ReplaceBodyPacket
	extends Packet
{
	private byte[] data;
	
	/**
	 * Create a new packet.
	 * @param data	mail-safe body content. Should be encoded according to 
	 * 		RFC 822/RFC 2047.
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 * @see javax.mail.internet.MimeBodyPart
	 * @see com.sendmail.milter.util.Misc#getBytes(String)
	 */
	public ReplaceBodyPacket(byte[] data) {
		super(Type.REPLBODY);
		this.data = data;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unused")
	@Override
	public byte[] getData() throws IOException {
		return data;
	}
}
