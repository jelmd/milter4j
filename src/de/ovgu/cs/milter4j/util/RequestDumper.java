/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Header;
import javax.mail.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.MailFilter;
import de.ovgu.cs.milter4j.reply.ContinuePacket;
import de.ovgu.cs.milter4j.reply.Packet;

/**
 * A Simple Mail Filter, which just dumps requests fro the MTA.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class RequestDumper
	extends MailFilter
{
	private static final Logger log = LoggerFactory
		.getLogger(RequestDumper.class);
	private static final AtomicInteger instCounter = new AtomicInteger();
	private static String eol = System.getProperty("line.separator");

	private String name;

	/**
	 * Constructor as described in the {@link de.ovgu.cs.milter4j.Configuration} 
	 * contract.
	 * @param param		ignored
	 */
	public RequestDumper(String param) {
		name = "RequestDumper " + instCounter.getAndIncrement();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doAbort() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doQuit() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MailFilter getInstance() {
		return new RequestDumper(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean reconfigure(String param) {
		return true;
	}
	
	// MTA stuff
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doMacros(HashMap<String,String> allMacros, 
		HashMap<String,String> newMacros) 
	{
		if (newMacros == null) {
			return;
		}
		StringBuilder buf = new StringBuilder("doMacros:").append(eol);
		for (Entry<String,String> e : newMacros.entrySet()) {
			buf.append(e.getKey()).append("=").append(e.getValue()).append(eol);
		}
		log.info(buf.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doData(byte[] data) {
		StringBuilder buf = Misc.hexdump(data);
		log.info("doData():" + eol + buf.toString());
		return new ContinuePacket();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doHeader(List<Header> headers) {
		StringBuilder buf = new StringBuilder("doHeader:").append(eol);
		for (Header h : headers) {
			buf.append(h.getName()).append(": ").append(h.getValue()).append(eol);
		}
		log.info(buf.toString());
		return new ContinuePacket();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doConnect(String hostname, int port, String info) {
		log.info("doConnect:" + eol + "hostname=" + hostname + "  port=" + port
			+ "  info=" + info);
		return new ContinuePacket();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doHelo(String domain) {
		log.info("doHelo:" + eol + "domain=" + domain);
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doMailFrom(String from) {
		log.info("doMailFrom:" + eol + "MAIL FROM: " + from);
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doRecipientTo(String recipient) {
		log.info("doRecipientTo:" + eol + "RCPT TO: " + recipient);
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doBody(byte[] chunk) {
		log.info("doBody:" + eol + new String(chunk));
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doEndOfHeader(List<Header> headers, 
		HashMap<String,String> macros) 
	{
		StringBuilder buf = new StringBuilder("doEndOfHeader:").append(eol);
		buf.append("all Headers:").append(eol);
		for (Header h : headers) {
			buf.append(h.getName()).append(": ").append(h.getValue()).append(eol);
		}
		buf.append("all Macros:").append(eol);
		for (Entry<String,String> e : macros.entrySet()) {
			buf.append(e.getKey()).append("=").append(e.getValue()).append(eol);
		}
		log.info(buf.toString());
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Packet> doEndOfMail(List<Header> headers, 
		HashMap<String,String> macros, Message message) 
	{
		log.info("doEndOfMail:" + eol + ".");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doBadCommand(String cmd) {
		log.info("doBadCommand:" + eol + cmd);
		return new ContinuePacket();
	}

}
