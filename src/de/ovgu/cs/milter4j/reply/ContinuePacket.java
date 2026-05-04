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
 * Continue processing the current connection, message, or recipient.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ContinuePacket
	extends SimplePacket
{
	/**
	 * Cretae a new packet.
	 */
	public ContinuePacket() {
		super(Type.CONTINUE);
	}

}
