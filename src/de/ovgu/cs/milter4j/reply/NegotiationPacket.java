/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.reply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.sendmail.milter.MacroStage;
import com.sendmail.milter.Option;
import com.sendmail.milter.util.Misc;

/**
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class NegotiationPacket
	extends Packet
{
	private int version;
	private int protoFlags;
	private int modFlags;
	private HashMap<MacroStage, HashSet<String>> macros;

	/**
	 * Create the packet.
	 * @param data	raw data received.
	 */
	public NegotiationPacket(ByteBuffer data) {
		super(Type.OPTNEG);
		if (data == null) {
			return;
		}
		version = data.getInt();
		modFlags = data.getInt();
		protoFlags = data.getInt();
	}

	/**
	 * Create the packet.
	 * @param version		the milter version
	 * @param modMask		the mask of modifications a filter may do
	 * @param protoMask		supported protocol feature mask.
	 */
	public NegotiationPacket(int version, int modMask, int protoMask) {
		super(Type.OPTNEG);
		this.version = version;
		this.modFlags = modMask;
		this.protoFlags = protoMask;
	}
	
	/**
	 * Get the mask describing the modification filter may do.
	 * @return the modification mask.
	 */
	public int getModificationMask() {
		return modFlags;
	}
	
	/**
	 * Get the mask describing the MTA offered/filter requested protocol 
	 * features. It may not only represent {@link com.sendmail.milter.cmd.Type}
	 * but {@link Option} flags as well.
	 * 
	 * @return the protocol mask.
	 */
	public int getProtocolMask() {
		return protoFlags;
	}
	
	/**
	 * Get the version of the mail filter [library], which sent this packet. 
	 * @return the milter version of the sender.
	 */
	public int getVersion() {
		return version;
	}
	
	/**
	 * Set the milter compat version of the mail filter. 
	 * @param version 	version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}
	
	/**
	 * Set the mask describing the modification filter may do.
	 * @param mask 	the modification mask to set.
	 */
	public void setModificationMask(int mask) {
		this.modFlags = mask;
	}
	
	/**
	 * Get the mask, which describes requested protocol features. It may not 
	 * only represent {@link com.sendmail.milter.cmd.Type} but {@link Option} 
	 * flags as well.
	 * 
	 * @param mask 	protocol mask to set.
	 */
	public void setProtocolMask(int mask) {
		this.protoFlags = mask;
	}
	
	/**
	 * Set the list of macro names to negotiate with the MTA for the given stage. 
	 * @param s			target macro stage
	 * @param macros	a list of macro names. <code>null</code> deletes the 
	 * 		list of macro names for the given stage. 
	 */
	public void setMacros(MacroStage s, Set<String> macros) {
		if (s == null || macros == null || macros.isEmpty()) {
			if (this.macros != null) {
				this.macros.remove(s);
			}
			return;
		}
		HashSet<String> res = new HashSet<String>();
		for (String t : macros) {
			if (t == null) {
				continue;
			}
			t = t.trim();
			if (t.length() == 0) {
				continue;
			}
			res.add(t);
		}
		if (res.size() > 0) {
			if (this.macros == null) {
				this.macros = new HashMap<MacroStage, HashSet<String>>(7);
			}
			this.macros.put(s, res);
		}
	}
	
	/**
	 * Set the list of macro names to negotiate with the MTA for the given stage.
	 * @param stage the stage in question
	 * @return <code>null</code> if no macros are set, the list of macro names
	 * 		otherwise.
	 */
	public Set<String> getMacros(MacroStage stage) {
		if (macros == null) {
			return null;
		}
		HashSet<String> l = macros.get(stage);
		return l == null ? null : Collections.unmodifiableSet(l);
	}

	/**
	 * Get a list of stage macros to negotiate.
	 * Convinience method to avoid plausibility tests for each connection.
	 * @return <code>null</code> if none are set, the list otherwise.
	 */
	public HashMap<MacroStage, HashSet<String>> getStageMacros() {
		return macros;
	}
	
	/**
	 * Set the list of stage macros to negotiate.
	 * Convinience method to avoid plausibility tests for each connection.
	 * It is taken as and MUST NOT contain any <code>null</code> values,
	 * neither in the key list, nor in the value lists.
	 * @param macros map to set.
	 */
	public void setStageMacros(HashMap<MacroStage, HashSet<String>> macros) {
		this.macros = macros;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getData() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(3 * 4);
		bos.write(Misc.getBytes(version));
		bos.write(Misc.getBytes(modFlags));
		bos.write(Misc.getBytes(protoFlags));
		if (! (macros == null || macros.isEmpty())) {
			for (MacroStage s : macros.keySet()) {
				bos.write(Misc.getBytes(s.getCode()));
				for (String name : macros.get(s)) {
					bos.write(Misc.getBytes(name));
					bos.write(' ');
				}
				bos.write(0);
			}
		}
		return bos.toByteArray();
	}
	
}
