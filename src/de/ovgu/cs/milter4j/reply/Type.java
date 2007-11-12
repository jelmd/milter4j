/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.reply;

/**
 * All reply codes a filter may send to an MTA (SMFIR_ values).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Type {
	/** Option negotiation */
	OPTNEG('O'),
	/** add recipient */
	ADDRCPT('+'),
	/** remove recipient */
	DELRCPT('-'),
	/** add recipient (incl. ESMTP args) */
	ADDRCPT_PAR('2'),
	/** 421: shutdown (internal to MTA) */
	SHUTDOWN('4'),
	/** accept */
	ACCEPT('a'),
	/** replace body (chunk) */
	REPLBODY('b'),
	/** continue */
	CONTINUE('c'),
	/** discard */
	DISCARD('d'),
	/** change envelope sender (from) */
	CHGFROM('e'),
	/** cause a connection failure */
	CONN_FAIL('f'),
	/** add header */
	ADDHEADER('h'),
	/** insert header */
	INSHEADER('i'),
	/** set list of symbols (macros) */
	SETSYMLIST('l'),
	/** change header */
	CHGHEADER('m'),
	/** progress */
	PROGRESS('p'),
	/** quarantine */
	QUARANTINE('q'),
	/** reject */
	REJECT('r'),
	/** skip */
	SKIP('s'),
	/** tempfail */
	TEMPFAIL('t'),
	/** reply code etc */
	REPLYCODE('y');
	
	private byte val;
	
	Type(char val) {
		this.val = (byte) (0x0FF & val);
	}
	
	/**
	 * Get the byte code represented by this Reply. See SMFIR_* values.
	 * @return a byte code
	 */
	public byte getCode() {
		return val;
	}
	
	/**
	 * Get the Reply for the given byte value.
	 * @param value		value to evaluate
	 * @return the corresponding Reply
	 * @throws IllegalArgumentException if value is unknown
	 */
	public static final Type get(byte value) {
		for (Type r : Type.values()) {
			if (r.val == value) {
				return r;
			}
		}
		throw new IllegalArgumentException ("Unknown milter reply paket code '" 
			+ value + "'");
	}
}
