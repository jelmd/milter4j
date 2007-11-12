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
 * Prepends a header to the current message.
 * <p>
 * A filter will receive <i>only</i> headers that have been sent by the SMTP 
 * client and those header modifications by earlier filters. It will not 
 * receive the headers that are inserted by sendmail itself. This makes the 
 * header insertion position highly dependent on the headers that exist in the 
 * incoming message and those that are configured to be added by sendmail.
 * <p>
 * For example, sendmail will always add a <code>Received:</code> header to the 
 * beginning of the headers. Setting header position to <code>0</code> will 
 * actually insert the header before this <code>Received:</code> header. 
 * However, later filters can be easily confused as they receive the added 
 * header, but not the <code>Received:</code> header, thus making it hard 
 * to insert a header at a fixed position.
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
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class InsertHeaderPacket
	extends Packet
{
	private int idx;
	private String name;
	private String value;

	/**
	 * Create the packet with an empty header value.
	 * 
	 * @param idx   The location in the internal header list where this header 
	 * 		should be inserted; 0 makes it the topmost header. If it is larger 
	 * 		than the number of headers in the message, the header will simply 
	 * 		be append.
	 * @param name	The mail-safe header name.
	 * @throws IllegalArgumentException if the name parameter is <code>null</code>
	 * 		or an empty string of if the idx is &lt; 0
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public InsertHeaderPacket(int idx, String name) {
		this(idx, name, null);
	}
	
	/**
	 * Create the packet.
	 * 
	 * @param idx   The location in the internal header list where this header 
	 * 		should be inserted; 0 makes it the topmost header. If it is larger 
	 * 		than the number of headers in the message, the header will simply 
	 * 		be append.
	 * @param name	The mail-safe header name.
	 * @param value The mail-safe header value to be added. If <code>null</code>, it is 
	 * 		automatically set to an empty String.
	 * @throws IllegalArgumentException if the name parameter is <code>null</code>
	 * 		or an empty string of if the idx is &lt; 0
	 * @see javax.mail.internet.MimeUtility#encodeText(String, String, String)
	 */
	public InsertHeaderPacket(int idx, String name, String value) {
		super(Type.INSHEADER);
		this.name = name == null ? "" : name.trim();
		if (idx < 0 || this.name.length() == 0) {
			throw new IllegalArgumentException("Index must be > 0 and "
				+ "name is not allowed to be empty");
		}
		this.idx = idx;
		this.value = value == null ? "" : value.trim();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(name.length()+ value.length() + 2);
		bos.write(Misc.getBytes(idx));
		bos.write(Misc.getBytes(name));
		bos.write(0);
		bos.write(Misc.getBytes(value));
		bos.write(0);
		return bos.toByteArray();
	}
}
