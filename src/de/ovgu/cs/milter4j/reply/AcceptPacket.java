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
 * For a connection-oriented routine, accept this connection without further 
 * filter processing.  For a message- or recipient-oriented routine, accept 
 * this message without further filtering.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AcceptPacket
	extends SimplePacket
{
	private boolean force;
	
	/**
	 * Create the paket.
	 * @param force	if {@code true}, instruct the processor to assign
	 * 		the highest priority to this packet.
	 */
	public AcceptPacket(boolean force) {
		super(Type.ACCEPT);
		this.force = force;
	}

	/**
	 * Check, whether to instruct the processor to assign the highst priority
	 * to this packet (e.g. do not ask other filters for mail processing or 
	 * ignore their results). This might be useful to avoid uneccessary 
	 * processing of mails and to tell the milter, that this decision is final,
	 * no matter, whether other filters would return a result that would
	 * normally cause the rejection of the processed message.
	 * 
	 * @return if {@code true}, make this decision final.
	 */
	public boolean isFinal() {
		return force;
	}

	/**
	 * Set the final decision state of this packet.
	 * @param force if {@code true}, make this decision final.
	 * @see #isFinal()
	 */
	public void setFinal(boolean force) {
		this.force = force;
	}
}
