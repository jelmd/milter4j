/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.cmd;

import java.nio.ByteBuffer;

/**
 * A single header cmd packet.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class HeaderPacket
	extends Command
{
	private String name;
	private String value;

	/**
	 * Create the packet.
	 * @param data	raw data received.
	 */
	public HeaderPacket(ByteBuffer data) {
		super(Type.HEADER);
		if (data.hasRemaining()) {
			StringBuilder dst = getString(null, data);
			name = dst.toString();
			dst.setLength(0);
			value = getString(dst, data).toString();
		}
	}

	/**
	 * Get the header name
	 * @return the name of the header.
	 */
	public String getName() {
		return name;
	}

	/**
	 * The value of the header
	 * @return the value, which might be an empty String.
	 */
	public String getValue() {
		return value;
	}

}
