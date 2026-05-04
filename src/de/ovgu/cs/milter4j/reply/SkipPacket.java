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
import de.ovgu.cs.milter4j.Option;

/**
 * Skip further callbacks of the same type in this transaction. Currently this 
 * return value is only allowed between {@link MacroStage#EOH} and {@link MacroStage#EOM}. 
 * It can be used if a milter has received sufficiently many body chunks to 
 * make a decision, but still wants to invoke message modification functions 
 * that are only allowed to be called at {@link MacroStage#EOM}.
 * <p>
 * Note: the milter <em>must</em> negotiate this behavior with the MTA, i.e., 
 * it must check whether the protocol action {@link Option#SKIP} is available 
 * and if so, the milter must request it.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class SkipPacket
	extends SimplePacket
{
	/**
	 * Create the packet.
	 */
	public SkipPacket() {
		super(Type.SKIP);
	}
}
