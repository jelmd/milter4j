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
import java.util.ArrayList;

import de.ovgu.cs.milter4j.Modification;
import de.ovgu.cs.milter4j.util.Misc;

/**
 * Add a recipient to the message envelope.
 * <p>
 * Requires {@link Modification#ADDRCPT}/{@link Modification#ADDRCPT_PAR} negotiation.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AddRecipientPacket
	extends Packet
{
	private String recipient;
	private String[] args;
	
	/**
	 * Create the packet.
	 * @param recipient		recipient to add to the current message envelope.
	 * @throws IllegalArgumentException if recipient is <code>null</code>
	 */
	public AddRecipientPacket(String recipient) {
		this(recipient, null);
	}
	
	/**
	 * Create the paket.
	 * 
	 * @param recipient		mail-safe recipient to add to the current message 
	 * 		envelope.
	 * @param args			mail-safe ESMTP args to add. Each String denotes one 
	 * 		$ESMTP-NAME=$VALUE pair (see RFC 1869, section 6.).
	 * @throws IllegalArgumentException if recipient and/or args is 
	 * 		<code>null</code>
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 * @see <a href="http://www.rfc-editor.org/rfc/rfc1869.txt">RFC 1869</a>
	 * @see <a href="http://email.about.com/library/weekly/aa082597.htm">RCPT ESMTP args</a>
	 */
	public AddRecipientPacket(String recipient, String[] args) {
		super((args == null || args.length == 0) 
			? Type.ADDRCPT : Type.ADDRCPT_PAR);
		this.recipient = recipient == null ? "" : recipient.trim();
		if (this.recipient.length() == 0) {
			throw new IllegalArgumentException("empty recipient not allowed");
		}
		ArrayList<String> l = new ArrayList<String>(5);
		for (String m : args) {
			if (m != null) {
				String tmp = m.trim();
				if (tmp.length() > 0) {
					l.add(tmp);
				}
			}
		}
		if (l.size() == 0) {
			throw new IllegalArgumentException("no esmtp args found");
		}
		// NOTIFY='never'|comma_separatedlist_of('success'|'failure'|'delay'
		// ORCPT=value_which_contains(';') text
		this.args = l.toArray(new String[l.size()]);
	}

	/**
	 * {@inheritDoc}
	 * @throws IOException 
	 */
	@Override
	public byte[] getData() throws IOException  {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(recipient.length()+1);
		bos.write(Misc.getBytes(recipient));
		bos.write(0);
		if (getType() == Type.ADDRCPT_PAR) {
			for (int i=args.length-1; i > 0; i--) {
				bos.write(Misc.getBytes(args[i]));
				bos.write(' ');
			}
			bos.write(Misc.getBytes(args[0]));
			bos.write(0);
		}
		return bos.toByteArray();
	}
}
