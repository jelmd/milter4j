/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
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
