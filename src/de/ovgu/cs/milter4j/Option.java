/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j;

import java.util.EnumSet;

import de.ovgu.cs.milter4j.cmd.Type;


/**
 * Additional capabilities of the milter/MTA (SMFIP_ values).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Option {
	/** MTA understands SMFIS_SKIP, i.e. will not send any further mail body 
	 * chunks to the filter, if requested */
	SKIP(0x000400),
	/** request that the MTA should also send {@link Type#RCPT} commands that 
	 * have been rejected because the user is unknown (or similar reasons), but 
	 * not those which have been rejected because of syntax errors etc. If a 
	 * milter requests this protocol step, then it should check the macro 
	 * <code>{rcpt_mailer}:</code> if that is set to <em>error</em>, then the 
	 * recipient will be rejected by the MTA. Usually the macros 
	 * <code>{rcpt_host}</code> and <code>{rcpt_addr}</code> will contain an 
	 * enhanced status code and an error text in that case, respectively.*/
	RCPT_REJ(0x000800),
	/** MTA should not strip the per RFC required leading space (0x20) of the 
	 * header value, when sending it to the filter. Also if set, it does
	 * not attemp to insert the blank. If requested, then the MTA will also not 
	 * add a leading space to headers when they are added, inserted, or changed.
	 */
	HDR_LEADSPC(0x100000)
	;
	
	private int val;
	
	Option(int val) {
		this.val = val;
	}

	/**
	 * Get the byte code represented by this Option
	 * @return a byte code
	 */
	public int getCode() {
		return val;
	}

	/**
	 * Get the option for the given code
	 * @param value		code to evaluate
	 * @return the code for the given value
	 * @throws IllegalArgumentException if no Option is known for the given code
	 */
	public static final Option get(int value) {
		for (Option o : Option.values()) {
			if (o.val == value) {
				return o;
			}
		}
		throw new IllegalArgumentException ("Unknown milter option code '" 
			+ value + "'");
	}
	
	/**
	 * Get the milter code which correponds to the given set of Options
	 * @param set	set to analyze
	 * @return <code>0</code> if the given set is <code>null</code> or empty,
	 * 		the appropriate milter code otherwise. 
	 */
	public static final int getCode(EnumSet<Option> set) {
		if (set == null || set.isEmpty()) {
			return 0;
		}
		int res = 0;
		for (Option o : set) {
			res |= o.val;
		}
		return res;
	}
}
