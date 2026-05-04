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
 * Info, that all available headers have been sent by the MTA.
 * <p>
 * message-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class EndOfHeaderPacket
	extends Command
{
	/**
	 * Create the packet.
	 */
	public EndOfHeaderPacket() {
		super(Type.EOH);
	}
}
