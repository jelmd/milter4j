/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.cmd;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Provides macros to the filters.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class MacroPacket
	extends Command
{
	private HashMap<String,String> macros;

	/**
	 * Create the packet.
	 * @param data	raw data section of the packet. Might be empty.
	 */
	public MacroPacket(ByteBuffer data) {
		super(Type.MACRO);
		macros = new HashMap<String, String>();
		if (!data.hasRemaining()) {
			return;
		}
		StringBuilder name = new StringBuilder(16);
		StringBuilder value = new StringBuilder(64);
		while (data.hasRemaining()) {
			name.setLength(0);
			value.setLength(0);
			getString(name, data);
			getString(value, data);
			if (name.length() > 0) {
				macros.put(name.toString(), value.toString());
			}
		}
		
	}
	
	/**
	 * Get a reference to the macros provided by the MTA.
	 * <p>
	 * A filter must not modify this map, but should store the reference to
	 * it for more efficient use. The managing server will maintain this map
	 * for all filters, and add all new macros as soon as received.
	 * 
	 * @return a map, which might be empty at the moment.
	 */
	public HashMap<String,String> getMacros() {
		return macros;
	}
}
