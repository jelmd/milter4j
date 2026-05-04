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

import de.ovgu.cs.milter4j.MacroStage;

/**
 * For a connection-oriented Stage, reject this connection.  For a 
 * message-oriented Stage (except for {@link MacroStage#EOM} and if the message 
 * was already aborted) reject this message.  For a recipient-oriented routine, 
 * reject the current recipient (but continue processing the current message).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class RejectPacket
	extends SimplePacket
{
	/**
	 * Create the packet.
	 */
	public RejectPacket() {
		super(Type.REJECT);
	}
}
