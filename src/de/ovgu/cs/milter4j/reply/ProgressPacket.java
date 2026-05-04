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
 * Notifies the MTA that the filter is still working on a message, causing the 
 * MTA to re-start its timeouts.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ProgressPacket
	extends SimplePacket
{
	/**
	 * Create the packet.
	 */
	public ProgressPacket() {
		super(Type.PROGRESS);
	}
}
