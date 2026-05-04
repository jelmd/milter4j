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


/**
 * Instruction to abort the current message. More messages from the same mail 
 * client may followe, so don't discard CONNECT/HELO information, if required
 * for processing.
 * <p>
 * message-oriented
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class AbortPacket
	extends Command
{
	/** Create the paket */
	public AbortPacket() {
		super(Type.ABORT);
	}
}
