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
package de.ovgu.cs.milter4j.cmd;

import java.nio.ByteBuffer;

/**
 * Sends any bad aka unkown or un-implemented command, issued by the mail-client 
 * when talking with the MTA.
 * <p>
 * message-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 * 
 * @since Milter API 4
 */
public class UnknownCmdPacket
	extends Command
{
	private String cmd;
	
	/**
	 * Create the packet.
	 * @param data	raw data received
	 */
	public UnknownCmdPacket(ByteBuffer data) {
		super(Type.UNKNOWN);
		cmd = getString(null, data).toString();
	}

	/**
	 * Get the unrecognized SMTP command.
	 * @return the cmd received.
	 */
	public String getCmd() {
		return cmd;
	}
}
