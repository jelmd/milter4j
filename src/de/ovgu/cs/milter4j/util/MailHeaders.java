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

import java.util.ArrayList;
import java.util.List;

import javax.mail.Header;
import javax.mail.internet.InternetHeaders;

/**
 * A wrapper around {@link InternetHeaders}, which allows us to add the headers
 * <em>we</em> want.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class MailHeaders
	extends InternetHeaders
{
	/**
	 * Create a list of {@link InternetHeaders} using the given plain headers.
	 * @param headers	headers to assimilate
	 */
	public MailHeaders(List<Header> headers) {
		this.headers = new ArrayList<InternetHeader>(8);
		if (headers == null) {
			return;
		}
		for (Header h : headers) {
			addHeader(h.getName(), h.getValue());
		}
	}
}
