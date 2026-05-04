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
	@Override
	public byte[] getData() throws IOException {
		return null;
	}
}