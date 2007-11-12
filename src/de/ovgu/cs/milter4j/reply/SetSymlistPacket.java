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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.ovgu.cs.milter4j.MacroStage;
import de.ovgu.cs.milter4j.Modification;
import de.ovgu.cs.milter4j.util.Misc;

/**
 * Set the list of macros that the milter wants to receive from the MTA for a 
 * certain protocol stage.
 * <p>
 * There is an internal limit on the number of macros that can be set 
 * (currently 5), however, this limit is not enforced by libmilter, only by the 
 * MTA, but a possible violation of this restriction is not communicated back 
 * to the milter.
 * <p>
 * Requires {@link Modification#SETSYMLIST} negotiation.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class SetSymlistPacket
	extends Packet
{
	private MacroStage stage;
	private String[] macros;
	/**
	 * Create the new packet.
	 * @param stage		the protocol stage during which the macro list should 
	 * 		be used.
	 * @param macro 	list of macros, e.g. "{rcpt_mailer}", "{rcpt_host}", ...
	 * @throws IllegalArgumentException if stage is <code>null</code> or the 
	 * 		macro list is empty
	 */
	public SetSymlistPacket(MacroStage stage, String... macro) {
		super(Type.SETSYMLIST);
		if (stage == null || macro == null) {
			throw new IllegalArgumentException("stage and macro cannot be null");
		}
		this.stage = stage;
		ArrayList<String> l = new ArrayList<String>(5);
		for (String m : macro) {
			if (m != null) {
				String tmp = m.trim();
				if (tmp.length() > 0) {
					l.add(tmp);
				}
			}
		}
		if (l.size() == 0) {
			throw new IllegalArgumentException("no macros found");
		}
		macros = l.toArray(new String[l.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = 
			new ByteArrayOutputStream(32);
		bos.write(Misc.getBytes(stage.getCode()));
		for (int i=macros.length-1; i > 0; i--) {
			bos.write(Misc.getBytes(macros[i]));
			bos.write(' ');
		}
		bos.write(Misc.getBytes(macros[0]));
		bos.write(0);
		return bos.toByteArray();
	}
}
