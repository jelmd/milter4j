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

import de.ovgu.cs.milter4j.util.Misc;

/**
 * Directly set the SMTP error reply code. Only 4XX and 5XX replies are accepted.
 * This code will be used on subsequent error replies resulting from actions 
 * taken by this filter.
 * <p>
 * 
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ReplyPacket
	extends Packet
{
	/** max. length in bytes of a text line */
	public static final int MAX_LEN = 980;
	/** max. number of allowed text lines */
	public static final int MAX_MSGS = 32;
	
	private int reply;
	private String xcode;
	private String[] messages;

	// From RFC 2034 Section 4 status-code ::= class "." subject "." detail
	// class ::= "2" / "4" / "5" subject ::= 1*3digit detail ::= 1*3digit
	// and class must match reply
	private String checkXcode(String code, int reply) {
		String tmp[] = code.split("\\.");
		int res[] = new int[3];
		try {
			for (int i=0; i < 3; i++) {
				res[i] = Integer.parseInt(tmp[i], 10);
			}
		} catch (Exception e) {
			return null;
		}
		int x = reply / 100;
		return (res[0] == 2 || res[0] == 4 || res[0] == 5) && res[0] == x
			? "" + res[0] + "." + res[1] + "." + res[2] + " "
			: null;
	}

	/**
	 * @param reply 	The three-digit (RFC 821/2821) SMTP reply code. Must be 
	 * 		a valid 4XX or 5XX reply code.
	 * @param xcode 	The extended (RFC 1893/2034) reply code. If 
	 * 		<code>null</code>, no extended code is used. Otherwise, xcode must 
	 * 		conform to RFC 1893/2034.
	 * @param messages 	single mail-safe lines of text, which will be used as the 
	 * 		text part of the SMTP reply. If <code>null</code>, an empty message 
	 * 		will be used. Only the first {@value #MAX_MSGS} messages are honored. 
	 * 		All others are silently ignored. Lines longer than {@link #MAX_LEN} 
	 * 		({@value #MAX_LEN}) are silently truncated.
	 * 		Must not contain none-printable characters or '%'.
	 * @see de.ovgu.cs.milter4j.util.Misc#getBytes(String)
	 */
	public ReplyPacket(int reply, String xcode, String... messages) {
		super(Type.REPLYCODE);
		if (reply < 400 || reply > 599) {
			throw new IllegalArgumentException("reply code '" + reply 
				+ "' out of range");
		}
		this.reply = reply;
		if (xcode != null) {
			this.xcode = checkXcode(xcode, reply);
			if (this.xcode == null) {
				throw new IllegalArgumentException("invalid xcode '" + xcode
					+ "' - see RFC 2034, Section 4");
			}
		}
		if (messages != null) {
			ArrayList<String> l = new ArrayList<String>(32);
			for (int i=0; i < messages.length && i < MAX_MSGS; i++) {
				String tmp = messages[i];
				if (tmp != null) {
					tmp = tmp.trim();
					if (tmp.length() > 0) {
						if (tmp.length() < MAX_LEN) {
							l.add(tmp);
						} else {
							l.add(tmp.substring(0, MAX_LEN));
						}
					}
				}
			}
			if (l.size() > 0) {
				this.messages = l.toArray(new String[l.size()]);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
		byte[] replyCode = Integer.toString(reply, 10).getBytes();
		byte[] xCode = xcode == null ? Misc.ZERO_DATA : Misc.getBytes(xcode);
		if (messages == null) {
			bos.write(replyCode);
			bos.write(' ');
			bos.write(xCode);
		} else {
			for (int i=0; i < messages.length-1; i++) {
				bos.write(replyCode);
				bos.write('-');
				bos.write(xCode);
				bos.write(Misc.getBytes(messages[i]));
				bos.write('\r');
				bos.write('\n');
			}
			bos.write(replyCode);
			bos.write(' ');
			bos.write(xCode);
			bos.write(Misc.getBytes(messages[messages.length-1]));
		}
		bos.write(0);
		return bos.toByteArray();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString())
			.append(" '").append(reply).append(" ").append(xcode).append(" ");
		if (messages == null) {
			buf.append("rejecting command");
		} else {
			buf.append(messages[0]);
			if (messages.length > 1) {
				buf.append(" ... ");
			}
		}
		buf.append("'");
		return buf.toString();
	}
}
