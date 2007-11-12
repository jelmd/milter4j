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

/**
 * Macro stages, i.e. when the MTA usually offers new or stage related
 * macros to mail filters (SMFIM_ values)
 * <p>
 * Per default the sequence is:
 * <ol>
 * 		<li>{@link #CONNECT}</li>
 * 		<li>{@link #HELO}</li>
 * 		<li>{@link #ENVFROM}</li>
 * 		<li>{@link #ENVRCPT}</li>
 * 		<li>{@link #DATA}</li>
 * 		<li>{@link #EOH}</li>
 * 		<li>{@link #EOM}</li>
 * </ol>
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum MacroStage {
	/** connect */
	CONNECT,
	/** HELO/EHLO */
	HELO,
	/** MAIL From */
	ENVFROM,
	/** RCPT To */
	ENVRCPT,
	/** DATA */
	DATA,
	/** end of message (final dot) */
	EOM,
	/** end of header */
	EOH;
	
	/**
	 * Get the code represented by this Macro
	 * @return a byte code
	 */
	public int getCode() {
		return ordinal();
	}

	/**
	 * Get the stage for the given code
	 * @param value		code to evaluate
	 * @return the code for the given value
	 * @throws IllegalArgumentException if no Stage is known for the given code
	 */
	public static final MacroStage get(int value) {
		MacroStage[] m = MacroStage.values();
		if (0 <= value && value < m.length) {
			return m[value];
		}
		throw new IllegalArgumentException ("Unknown milter stage code '" 
			+ value + "'");
	}

}
