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

/**
 * Address family
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum AddressFamily {
	/** unknown */
	UNKNOWN('U'),
	/** unix/local */
	UNIX('L'),
	/** IPv4 */
	INET('4'),
	/** IPv6 */
	INET6('6');
	
	private char val;
	
	private AddressFamily(char c) {
		val = c;
	}
	
	/**
	 * Get the byte code of this family
	 * @return a byte code
	 */
	public byte getCode() {
		return (byte) val;
	}
	
	/**
	 * Get the address family for the given byte code.
	 * @param value		value to evaluate
	 * @return the corresponding Address family code
	 * @throws IllegalArgumentException if value is unknown
	 */
	public static AddressFamily get(byte value) {
		for (AddressFamily c : AddressFamily.values()) {
			if (c.val == value) {
				return c;
			}
		}
		throw new IllegalArgumentException ("Unknown address family code '" 
			+ value + "'");
	}
}
