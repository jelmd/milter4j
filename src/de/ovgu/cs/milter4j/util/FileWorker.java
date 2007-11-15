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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamCorruptedException;
import java.util.EnumSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

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
public class FileWorker {
	
	private static final Logger log = LoggerFactory
		.getLogger(FileWorker.class);

	private MailFilter filter;
	private EnumSet<Type> cmds;
	
	/**
	 * Create a "nothing to do" file worker.
	 */
	public FileWorker() {
		this(null);
	}
	
	/**
	 * Create a worker, which uses the given filter
	 * @param filter	filter to use.
	 */
	public FileWorker(MailFilter filter) {
		setFilter(filter);
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
		read(ir);
	}

	/**
	 * Read the given stream and invoke the appropriate methods on the mail 
	 * filter currently set. The stream must point to the first line of an 
	 * mbox file.
	 * 
	 * @param is	stream to use for reading an mbox file
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	public void read(InputStream is) throws IOException, MessagingException {
		if (filter == null) {
			log.info("No filter set - nothing to do");
			return;
		}
		if (is == null) {
			log.info("Nothing to read - nothing to do");
			return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = br.readLine();
		if (line == null || !line.startsWith("From ")) {
			throw new StreamCorruptedException("Not an mbox stream");
		}
		int idx = line.indexOf(' ', 5);
		if (idx == -1) {
			throw new StreamCorruptedException("Not an mbox stream");
		}
		if (cmds.contains(Type.MAIL)) {
			Packet p = filter.doMailFrom(new String[] { line.substring(5, idx) });
			log.info("RESULT: {}", String.valueOf(p));
		}
		MimeMessage msg = new MimeMessage(null, is);
		
	}
}
