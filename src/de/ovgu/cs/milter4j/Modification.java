/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter;

import java.util.EnumSet;

/**
 * Modifications a filter may request (SMFIF_ values).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Modification {
	/** no changes at all */
	NONE(0),
	/** filter may add headers */
	ADDHDRS(0x0001),
	/** filter may replace body */
	CHGBODY(0x0002),
	/** filter may add recipients */
	ADDRCPT(0x0004),
	/** filter may delete recipients */
	DELRCPT(0x0008),
	/** filter may change/delete headers */
	CHGHDRS(0x0010),
	/** filter may quarantine envelope */
	QUARANTINE(0x0020),
	/** filter may change "from" (envelope sender) */
	CHGFROM(0x40),
	/** add recipients incl. args */
	ADDRCPT_PAR(0x0080),
	/** filter can send set of symbols (macros) that it wants */
	SETSYMLIST(0x0100);

	private int val;
	
	Modification(int val) {
		this.val = val;
	}
	
	/**
	 * Get the byte code represented by this modification type
	 * @return a byte code
	 */
	public int getCode() {
		return val;
	}

	/**
	 * Get the modification type for the given code
	 * @param value		code to evaluate
	 * @return the code for the given value
	 * @throws IllegalArgumentException if no modification type is known for the 
	 * 		given code
	 */
	public static final Modification get(int value) {
		for (Modification o : Modification.values()) {
			if (o.val == value) {
				return o;
			}
		}
		throw new IllegalArgumentException ("Unknown milter modification code '" 
			+ value + "'");
	}
	
	/**
	 * Get the milter modification code which correponds to the given set of 
	 * modifications.
	 * @param set	set to analyze
	 * @return <code>0</code> if the given set is <code>null</code> or empty,
	 * 		the appropriate milter code otherwise. 
	 */
	public static final int getCode(EnumSet<Modification> set) {
		if (set == null || set.isEmpty()) {
			return 0;
		}
		int res = 0;
		for (Modification o : set) {
			res |= o.val;
		}
		return res;
	}
}
