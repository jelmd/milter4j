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

/**
 * For a message- or recipient-oriented routine, accept this message, but 
 * silently discard it.
 * <p>
 * Should not be returned by a connection-oriented routine.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class DiscardPacket
	extends SimplePacket
{
	/**
	 * Create a new packet.
	 */
	public DiscardPacket() {
		super(Type.DISCARD);
	}
}
