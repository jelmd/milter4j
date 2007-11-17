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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Header;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.AddressFamily;
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
	public boolean reassembleMail() {
		return true;
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
	
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public Set<String> getRequiredMacros(MacroStage stage) {
//		HashSet<String> macros = new HashSet<String>();
//		macros.add(Macro.QUEUE.toString());
//		macros.add(Macro.MSG_SIZE.toString());
//		return macros;
//	}
	
	// MTA stuff
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doAbort() {
		log.info("ABORT");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doQuit() {
		log.info("QUIT");
	}

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
	public Packet doData() {
		log.info("doData:");
		return new ContinuePacket();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doHeader(String name, String value) {
		log.info("doHeader:" + eol + name + ": " + value);
		return new ContinuePacket();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doConnect(String hostname, AddressFamily family, int port, 
		String info) 
	{
		log.info("doConnect:" + eol + "hostname=" + hostname 
			+ "  addrFamily=" + family.name() + "  port=" + port
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
	public Packet doMailFrom(String[] from) {
		StringBuilder buf = new StringBuilder("doMailFrom:").append(eol);
		for (String arg : from) {
			buf.append("MAIL FROM: ").append(arg).append(eol);
		}
		log.info(buf.toString());
		return new ContinuePacket();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Packet doRecipientTo(String[] recipient) {
		StringBuilder buf = new StringBuilder("doRecipientTo:").append(eol);
		for (String arg : recipient) {
			buf.append("RCPT TO: ").append(arg).append(eol);
		}
		log.info(buf.toString());
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
		buf.append("ALL HEADERS:").append(eol);
		for (Header h : headers) {
			buf.append(h.getName()).append(": ").append(h.getValue()).append(eol);
		}
		buf.append(eol).append("ALL MACROS:").append(eol);
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
		HashMap<String,String> macros, Mail msg) 
	{
		StringBuilder buf = new StringBuilder("doEndOfMail:").append(eol);
		if (msg != null) {
			try {
				buf.append("  content type=").append(msg.getContentType()).append(eol);
				buf.append("  description =").append(msg.getDescription()).append(eol);
				buf.append("  disposition =").append(msg.getDisposition()).append(eol);
				buf.append("  filename    =").append(msg.getFileName()).append(eol);
				buf.append("  encoding    =").append(msg.getEncoding()).append(eol);
				buf.append("  content     =").append(msg.getContent()).append(eol);
			} catch (IOException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			} catch (MessagingException e) {
				log.warn(e.getLocalizedMessage());
				if (log.isDebugEnabled()) {
					log.debug("method()", e);
				}
			}
		} else {
			buf.append(".");
		}
		log.info(buf.toString());
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
