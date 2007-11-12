/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.reply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.ovgu.cs.milter4j.Modification;
import de.ovgu.cs.milter4j.util.Misc;

/**
 * Change the envelope sender (MAIL From) of the current message.
 * <p>
 * Requires {@link Modification#CHGFROM} negotiation.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ChangeFromPacket
	extends Packet
{
	private String sender;
	private String[] args;

	/**
	 * Create a new packet.
	 * @param sender	sender to set for the current message.
	 */
	public ChangeFromPacket(String sender) {
		this(sender, null);
	}
	
	/**
	 * Create a new packet.
	 * <p>
	 * Even though all ESMTP arguments could be set via this packet, it does not 
	 * make sense to do so for many of them, e.g., SIZE and BODY. Setting those 
	 * may cause problems, proper care must be taken. Moreover, there is no 
	 * feedback from the MTA to the milter whether the call was successful.
	 * 
	 * @param sender	mail-safe sender to set for the current message.
	 * @param args 		mail-safe ESMTP arguments to set. Each String denotes one 
	 * 		$ESMTP-NAME=$VALUE pair.
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public ChangeFromPacket(String sender, String[] args) {
		super(Type.CHGFROM);
		this.sender = sender == null ? "" : sender.trim();
		if (this.sender.length() == 0) {
			throw new IllegalArgumentException("empty sender not allowed");
		}
		this.args = args;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(sender.length()+1);
		bos.write(Misc.getBytes(sender));
		bos.write(0);
		if (args != null) {
			for(int i=0; i < args.length; i++) {
				bos.write(Misc.getBytes(args[i]));
				if (i < args.length-1) {
					bos.write(' ');
				}
			}
			bos.write(0);
		}
		return bos.toByteArray();
	}
}
