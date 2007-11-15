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
	/** connect (connection-oriented) */
	CONNECT,
	/** HELO/EHLO (connection-oriented) */
	HELO,
	/** MAIL From (message-oriented) */
	ENVFROM,
	/** RCPT To (recipient-oriented) */
	ENVRCPT,
	/** DATA  (message-oriented) */
	DATA,
	/** end of message (final dot)  (message-oriented) */
	EOM,
	/** end of header  (message-oriented) */
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
