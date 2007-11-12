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

import java.nio.ByteBuffer;

/**
 * Sends connection info to milter filters
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class ConnectPacket
	extends Command
{
	private String hostname;
	private int port;
	private String info;
	
	/**
	 * Create the packet
	 * @param data	raw data received
	 */
	public ConnectPacket(ByteBuffer data) {
		super(Type.CONNECT);
		StringBuilder dst = getString(null, data);
		hostname = dst.toString();
		if (data.hasRemaining()) {
			port = data.getShort();
			dst.setLength(0);
			getString(dst, data);
			info = dst.indexOf("IPv6:") == 0 ? dst.substring(5) : dst.toString();
		} else {
			port = -1;
		}
	}

	/**
	 * Get hostname of remote mail-client machine.
	 * @return the possibly empty hostname.
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Get the port of the remote mail-client connection.
	 * @return <code>-1</code> if not available, the port otherwise.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get additional info for the remote mail-client connection.
	 * This might be an IPv4 or IPv6 address, or a UNIX socket path.
	 * 
	 * @return <code>null</code> if not available, the info otherwise.
	 */
	public String getInfo() {
		return info;
	}
}
