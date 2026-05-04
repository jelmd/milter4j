/**
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package de.ovgu.cs.milter4j.util;

import java.util.ArrayList;
import java.util.List;

import jakarta.mail.Header;
import jakarta.mail.internet.InternetHeaders;

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
		this.headers = new ArrayList<>(8);
		if (headers == null) {
			return;
		}
		for (Header h : headers) {
			addHeader(h.getName(), h.getValue());
		}
	}
}
