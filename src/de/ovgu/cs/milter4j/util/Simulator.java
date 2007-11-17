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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.MailFilter;
import de.ovgu.cs.milter4j.cmd.Type;
import de.ovgu.cs.milter4j.reply.Packet;

/**
 * A class, which might be used to test a mail filter using an mbox file.
 * <p>
 * Since the mail message in question is read from an file, the following
 * methods will never be invoked on the target dueto missing information:
 * <ul> 
 * 	<li>{@link MailFilter#doConnect(String, de.ovgu.cs.milter4j.AddressFamily, int, String)}</li>
 * 	<li>{@link MailFilter#doHelo(String)}</li>
 *	<li>{@link MailFilter#doRecipientTo(String[])}</li>
 *	<li>{@link MailFilter#doMacros(java.util.HashMap, java.util.HashMap)}</li>
 *	<li>{@link MailFilter#doAbort()}</li>
 * </ul>
 * Also the arguments supplied in the {@link MailFilter#doMailFrom(String[])}
 * usually do not contain any ESMTP args, since they are not stored in an
 * mbox file.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Simulator {
	
	private static final Logger log = LoggerFactory
		.getLogger(Simulator.class);

	private MailFilter filter;
	private EnumSet<Type> cmds;
	private int chunkSize;
	
	/**
	 * Create a "nothing to do" file worker.
	 */
	public Simulator() {
		this(null, 4096);
	}

	/**
	 * Create a "nothing to do" file worker.
	 * @param chunkSize size of body chunks to use.
	 */
	public Simulator(int chunkSize) {
		this(null, chunkSize);
	}

	/**
	 * Create a worker, which uses the given filter
	 * @param filter	filter to use.
	 */
	public Simulator(MailFilter filter) {
		this(filter, 4096);
	}
	
	/**
	 * Create a worker, which uses the given filter
	 * @param filter	filter to use.
	 * @param chunkSize size of body chunks to use.
	 */
	public Simulator(MailFilter filter, int chunkSize) {
		setFilter(filter);
		this.chunkSize = chunkSize;
	}

	/**
	 * Get the mail filter used to process messages.
	 * @return <code>null</code> if not set, the mail filter otherwise.
	 */
	public MailFilter getFilter() {
		return filter;
	}

	/**
	 * Set the mail filter to use to process messages.
	 * @param filter <code>null</code> to cleanup the reference to the current
	 * 		mail filter, the mail filter to use otherwise.
	 */
	public void setFilter(MailFilter filter) {
		this.filter = filter;
		cmds = filter.getCommands();
	}
	
	/**
	 * Read the given file and invoke the appropriate methods on the mail filter
	 * currently set.
	 * @param file	mbox file to read
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	public void read(File file) throws IOException, MessagingException {
		if (filter == null) {
			log.info("No filter set - nothing to do");
			return;
		}
		if (file == null) {
			log.info("No mbox file to read - nothing to do");
			return;
		}
		FileInputStream ir = new FileInputStream(file);
		StringBuilder buf = new StringBuilder(64);
		int c = 0;
		while((c = ir.read()) != -1) {
			if (c == '\n') {
				break;
			}
			buf.append((char) c);
		}
		if (ir.available() < 1) {
			throw new StreamCorruptedException("Not an mbox stream");
		}
		ir.mark(4096);
		if (buf.indexOf("From ") == -1) {
			throw new StreamCorruptedException("Not an mbox stream");
		}
		if (cmds.contains(Type.MAIL)) {
			Packet p = filter.doMailFrom(new String[] { 
				buf.substring(5, buf.indexOf(" ", 6)) 
			});
			log.info("RESULT: {}", String.valueOf(p));
		}
		Mail msg = new Mail(ir);
		Enumeration<?> headers = msg.getAllHeaders();
		ArrayList<Header> localHeaders = new ArrayList<Header>();
		HashMap<String,String> macros = new HashMap<String,String>(0);
		if (cmds.contains(Type.HEADER)) {
			while (headers.hasMoreElements()) {
				Header h = (Header) headers.nextElement();
				localHeaders.add(h);
				Packet p = filter.doHeader(h.getName(), h.getValue());
				log.info("RESULT: {}", String.valueOf(p));
			}
		}
		if (cmds.contains(Type.EOH)) {
			Packet p = filter.doEndOfHeader(localHeaders, macros);
			log.info("RESULT: {}", String.valueOf(p));
		}
		if (cmds.contains(Type.DATA)) {
			Packet p = filter.doData();
			log.info("RESULT: {}", String.valueOf(p));
		}
		if (cmds.contains(Type.BODY)) {
			byte[] data = msg.getContentRaw();
			int count = data.length / chunkSize;
			int offset = 0;
			byte[] chunk = new byte[chunkSize];
			for (;count > 0;count--) {
				System.arraycopy(data, offset, chunk, 0, chunkSize);
				offset += chunkSize;
				Packet p = filter.doBody(chunk);
				log.info("RESULT: {}", String.valueOf(p));
			}
			chunk = new byte[data.length-offset];
			System.arraycopy(data, offset, chunk, 0, chunk.length);
			Packet p = filter.doBody(chunk);
			log.info("RESULT: {}", String.valueOf(p));
		}
		if (cmds.contains(Type.BODYEOB)) {
//			Object o = msg.getContent();
//			if (o instanceof MimeMultipart) {
//				MimeMultipart mp = (MimeMultipart) o;
//				int count = mp.getCount();
//				for (int i=count-1; i >= 0; i--) {
//					BodyPart bp = mp.getBodyPart(i);
//					bp.getLineCount();
//				}
//			}
			List<Packet> p = filter.reassembleMail()
				? filter.doEndOfMail(localHeaders, macros, msg)
				: filter.doEndOfMail(localHeaders, macros, null);
			if (p == null) {
				log.info("RESULT: null");
			} else {
				for (Packet t : p) {
					log.info("RESULT: {}", String.valueOf(t));
				}
			}
		}
		filter.doQuit();
	}
	
	/**
	 * Example: Uses {@link RequestDumper} as mail filter and the given file
	 * as the message to process.
	 * 
	 * @param args	an box file
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			File f = new File(args[0]);
			Simulator s = new Simulator(new RequestDumper(null));
			try {
				s.read(f);
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
			log.warn("Missing argument aka mbox file to read");
		}
	}
}
