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

/**
 * A simple paket, which carries no data.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
class SimplePacket
	extends Packet
{
	/**
	 * Create the packet.
	 * @param type	type of the packet.
	 */
	public SimplePacket(Type type) {
		super(type);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unused")
	@Override
	public byte[] getData() throws IOException {
		return null;
	}
}