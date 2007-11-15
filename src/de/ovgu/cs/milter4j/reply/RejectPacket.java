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
