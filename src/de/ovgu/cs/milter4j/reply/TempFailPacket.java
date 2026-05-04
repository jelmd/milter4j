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
 * Return a temporary failure, i.e., the corresponding SMTP command will return 
 * an appropriate 4xx status code. For a message-oriented Stage (except 
 * {@link MacroStage#ENVFROM}, fail for this message.
 * For a connection-oriented Stage, fail for this connection.
 * For a recipient-oriented Stage, only fail for the current recipient; 
 * continue message processing.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class TempFailPacket
	extends SimplePacket
{
	/**
	 * Create the Paket.
	 */
	public TempFailPacket() {
		super(Type.TEMPFAIL);
	}
}
