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

import java.util.EnumSet;

/**
 * All command codes a filter may receive from an MTA (SMFIC_ and 
 * corresponding SMFIP_ values).
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Type {
	/** Option negotiation */
	OPTNEG('O',		0, 			0),
	/** provides macros to the filters */
	MACRO('D',		0, 			0),
	/** Connection information */
	CONNECT('C',	0x000001,	0x001000),
	/** HELO/EHLO command */
	HELO('H',		0x000002,	0x002000),
	/** envelope from */
	MAIL('M',		0x000004,	0x004000),
	/** envelope recipient */
	RCPT('R',		0x000008,	0x008000),
	/** DATA 
	 * @since milter version 3 */
	DATA('T',		0x000200,	0x010000),
	/** Header */
	HEADER('L',		0x000020,	0x000080),
	/** End of Header */
	EOH('N',		0x000040,	0x040000),
	/** body chunk */
	BODY('B',		0x000010,	0x080000),
	/** final body chunk (End) */
	BODYEOB('E',	0,			0),
	/** Tells filter to abort current message. I.e. the sending mail client
	 * is still connected to the MTA and may send more messages. So if a milter
	 * needs information obtained in {@link #CONNECT} and/or {@link #HELO}, it
	 * should not discard that information, but everything else related to the 
	 * current message (i.e. obtained since, incl. {@link #MAIL}) */
	ABORT('A',		0,			0),
	/** QUIT but new connection follows */
	QUIT_NC('K',	0,			0),
	/** Close down a single filter, i.e. closes the socket immediately after
	 * sending this comand - doesn't expect any reponse */
	QUIT('Q',		0,			0),
	/** unrecognized or unimplemented aka bad command issued by the mail client 
	 * @since milter version 4 */
	UNKNOWN('U',	0x000100,	0x020000);

	private byte val;
	private int skip;
	private int reply;
	
	/**
	 * Constructor
	 * @param cmd			value sent by the MTA as ID aka cmd in the packet
	 * @param skipflag		flag the filter must set, if it is not interested
	 * 						in this command
	 * @param replyflag		flag the filter must set, if it will not send a
	 * 						response to that command
	 */
	Type(char cmd, int skipflag, int replyflag) {
		this.val = (byte) (0x0FF & cmd);
	}
	
	/**
	 * Get the byte code represented by this command. See SMFIC_* values.
	 * @return a byte code
	 */
	public byte getCode() {
		return val;
	}

	/**
	 * Get the flag to be sent to the MTA, if the filter is not interested in
	 * receiving this command. See SMFIP_NO* values.
	 * @return the flag to set.
	 */
	public int getSkipFlag() {
		return skip;
	}
	
	/**
	 * Get the flag to be sent to the MTA, to indicate, that the filter will not 
	 * send any reply to this command. See SMFIP_NR* values.
	 * 
	 * @return the flag to set.
	 */
	public int getNoReplyFlag() {
		return reply;
	}
	
	/**
	 * Convinence method to calculate the value to be sent to the MTA to inform
	 * it about actions, the MTA should not sent to the filter.
	 * @param set	set of command types to skip
	 * @return the corresponding skip mask.
	 */
	public static int getSkipMask(EnumSet<Type> set) {
		if (set == null) {
			return 0;
		}
		int res = 0;
		for (Type t : set) {
			res |= t.getSkipFlag();
		}
		return res;
	}

	/**
	 * Convinence method to calculate the value to be sent to the MTA to inform
	 * it about actions, to which the filter will not sent a reply.
	 * @param set	set of command types, to whome the filter sends no reply
	 * @return the corresponding no reply mask.
	 */
	public static int getNoReplypMask(EnumSet<Type> set) {
		if (set == null) {
			return 0;
		}
		int res = 0;
		for (Type t : set) {
			res |= t.getNoReplyFlag();
		}
		return res;
	}

	/**
	 * Get the Command for the given byte code.
	 * @param value		value to evaluate
	 * @return the corresponding Command code
	 * @throws IllegalArgumentException if value is unknown
	 */
	public static Type get(byte value) {
		for (Type c : Type.values()) {
			if (c.val == value) {
				return c;
			}
		}
		throw new IllegalArgumentException ("Unknown milter command code '" 
			+ value + "'");
	}
}
