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
 * Inform the filter, that the client mail session is closed, i.e. no further
 * messages and thus commands need to be processed.
 * <p>
 * connection-oriented
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class QuitPacket
	extends Command
{
	/**
	 * Create the package.
	 * @param nc	whether to quit and prepare for a new connection.
	 */
	public QuitPacket(boolean nc) {
		super(nc ? Type.QUIT_NC : Type.QUIT);
	}
}
