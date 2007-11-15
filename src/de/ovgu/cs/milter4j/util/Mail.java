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

import java.util.List;

import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * Wrapper around {@link MimeMessage}, which allows us to create it efficiently.
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Mail
	extends MimeMessage
{

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
}
