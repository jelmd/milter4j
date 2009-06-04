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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around {@link MimeMessage}, which allows us to create it efficiently.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Mail
	extends MimeMessage
{
	private static final Logger log = LoggerFactory.getLogger(Mail.class);
	/**
	 * Create a new mail using the given content directly (no copy).
	 * @param headers	headers to set
	 * @param content	raw RFC 822 compliant mail body
	 */
	public Mail(List<Header> headers, byte[] content) {
		super((Session) null);
		this.headers = new MailHeaders(headers);
		this.content = content;
		this.modified = false;
	}
	
	/**
	 * Construct a mail using a <code>null</code> session.
	 * @param is	where to ready the raw message from
	 * @throws MessagingException
	 */
	public Mail(InputStream is) throws MessagingException {
		super(null, is);
	}
	
	/**
	 * Get the body of the Mail as a raw byte array.
	 * <p>
	 * If the mail was constructed using an input stream, this method creates
	 * a copy of the body in memory, which might be critical wrt. resource
	 * usage on the running system!
	 *  
	 * @return the mail body  
	 */
	public byte[] getContentRaw() {
		if (this.content != null) {
			return content;
		}
		ByteArrayOutputStream bos = null;
		try {
			InputStream is = getContentStream();
			bos = new ByteArrayOutputStream();
			byte[] dst = new byte[4096]; 
			int count = 0;
			while((count = is.read(dst)) != -1) {
				bos.write(dst, 0, count);
			}
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("method()", e);
			}
			return new byte[0];
		}
		return bos.toByteArray();
	}
}
