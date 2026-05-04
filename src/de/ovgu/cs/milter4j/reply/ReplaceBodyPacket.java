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
package de.ovgu.cs.milter4j.reply;

import java.io.IOException;

import de.ovgu.cs.milter4j.Modification;
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
	 * @see jakarta.mail.internet.MimeUtility#encodeText(String, String, String)
	 * @see jakarta.mail.internet.MimeBodyPart
	 * @see de.ovgu.cs.milter4j.util.Misc#getBytes(String)
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
