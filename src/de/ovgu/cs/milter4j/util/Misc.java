/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.mail.internet.MimeUtility;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some utilities to make life easier.
 * 
 * @author Jens Elkner
 * @version $Revision$
 */
public class Misc {
	static final Logger log = LoggerFactory.getLogger(Misc.class);
	/** a byte array, which contains nothing */
	public static final byte[] ZERO_DATA = new byte[0];

	/**
	 * Convert the given value into a sequence of bytes in big endian order.
	 * 
	 * @param val
	 *            value to convert
	 * @return a byte[4] array.
	 */
	public static final byte[] getBytes(int val) {
		byte[] v = new byte[4];
		if (val > 0) {
			v[3] = (byte) (val >> 0);
			v[2] = (byte) (val >> 8);
			v[1] = (byte) (val >> 16);
			v[0] = (byte) (val >> 24);
		}
		return v;
	}

	/**
	 * Blindly copies the given string into an byte array by simply casting each
	 * character into byte. This avoids the encoding overhead of
	 * {@link String#getBytes(String)} and friends. NOTE: It will not produce,
	 * what you want, if the String does not contain 1-byte characters, only!
	 * 
	 * @param s
	 *            String to convert
	 * @return an array of bytes; never <code>null</code>.
	 */
	public static final byte[] getBytes(String s) {
		if (s == null || s.length() == 0) {
			return ZERO_DATA;
		}
		char[] c = s.toCharArray();
		byte[] res = new byte[c.length];
		for (int i = c.length - 1; i >= 0; i-- ) {
			res[i] = (byte) c[i];
		}
		return res;
	}

	/**
	 * Converts the given String into a byte array which corresponds to US-ASCII
	 * characters, only. The following steps are made, to ensure RFC 822/RFC
	 * 2047 compliance:
	 * <ol>
	 * <li>Converts the given String into an byte array using the given charset</li>
	 * <li>applies the encoding rules of RFC 2047 to ensure, that the resulting
	 * string contains US-ASCII characters, only</li>
	 * <li>converts the resulting US-ASCII char array into a byte array</li>
	 * </ol>
	 * 
	 * @param s
	 *            string to convert
	 * @param cs
	 *            charset to use for initial string to byte conversion. If
	 *            <code>null</code>, the platforms default charset will be
	 *            used.
	 * @return a mail-safe string
	 * @throws IOException
	 *             if the given String can not be converted into a byte array
	 *             using the given charset.
	 * @see MimeUtility#encodeText(String, String, String)
	 */
	public static final byte[] getAsciiBytes(String s, String cs)
		throws IOException
	{
		return getBytes(MimeUtility.encodeText(s, cs, null));
	}

	/**
	 * Boilerplate: Get a stream source from the given file.
	 * <p>
	 * The underlying stream is an InputStream. So to finally free the resources
	 * allocated by the source one should call close on
	 * {@link StreamSource#getInputStream()}.
	 * 
	 * @param path
	 *            file to read.
	 * @param gzip
	 *            if <code>true</code> the file is gzipped.
	 * @return a StreamSource for the given path
	 * @throws IOException
	 *             if path does not exist or is not readable or the is not in
	 *             gzipped format when gzip option is set.
	 */
	public static StreamSource getInputSourceByFile(File path, boolean gzip)
		throws IOException
	{
		InputStream is = null;
		try {
			is = new FileInputStream(path);
			if (gzip) {
				is = new GZIPInputStream(is);
			}
			return new StreamSource(is, path.getName());
		} catch (IOException e) {
			if (is != null) {
				try {
					is.close();
				} catch (Exception x) {
					/** ignore */
				}
			}
			throw e;
		}
	}

	private static InputStream NULL_INPUT_STREAM;

	/**
	 * Get an InputStream whoms read method always returns <code>-1</code>.
	 * 
	 * @see #getIgnoringXMLResolver()
	 * @return a reusable InputStream.
	 */
	public static final InputStream getNullInputStream() {
		if (NULL_INPUT_STREAM == null) {
			NULL_INPUT_STREAM = new InputStream() {
				@Override
				public int read() {
					return -1;
				}
			};
		}
		return NULL_INPUT_STREAM;
	}

	private static XMLResolver IGNORING_XML_RESOLVER;

	/**
	 * Get an XMLResolver, which always returns an empty InputStream, to make
	 * braindead Stax Implementations (e.g. from SUN) happy.
	 * 
	 * @see #getNullInputStream()
	 * @return a resolver, which resolves everything to an empty entity.
	 */
	public static final XMLResolver getIgnoringXMLResolver() {
		if (IGNORING_XML_RESOLVER == null) {
			IGNORING_XML_RESOLVER = new XMLResolver() {
				public Object resolveEntity(String publicID, String systemID,
					String baseURI, String namespace)
				{
					log.debug("resolve publicID=" + publicID + ", systemID="
						+ systemID + ", baseURI=" + baseURI + ", ns="
						+ namespace);
					return getNullInputStream();
				}
			};
		}
		return IGNORING_XML_RESOLVER;
	}

	/**
	 * Create an none-validating XMLStreamReader for the given InputStream and
	 * position the cursor at the first root element with the given name. The
	 * reader ignores external entities.
	 * 
	 * @param src
	 *            xml stream source to read from
	 * @param root
	 *            name of the document root element. If <code>null</code>,
	 *            the cursor gets positioned at the start of the first element
	 *            encountered in the stream.
	 * @param namespaceAware
	 *            if <code>true</code> the returned reader is XML Namespace
	 *            aware, otherwise not.
	 * @return <code>null</code> if one of the parameters are
	 *         <code>null</code> or the given root element could not be found,
	 *         the reader otherwise.
	 */
	public static final XMLStreamReader getReader(StreamSource src,
		String root, boolean namespaceAware)
	{
		if (src == null) {
			return null;
		}
		XMLInputFactory xif = XMLInputFactory.newInstance();
		xif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
			Boolean.TRUE);
		xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean
			.valueOf(namespaceAware));
		xif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
		xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
			Boolean.FALSE);
		if (xif.isPropertySupported("javax.xml.stream.supportDTD")) {
			// the brack sun StAX implementation does not ignore DTDs in
			// DOCTYPE,
			// even if it is not validating
			xif.setXMLResolver(getIgnoringXMLResolver());
		}
		try {
			XMLStreamReader reader = xif.createXMLStreamReader(src);
			while (reader.hasNext()) {
				int e = reader.next();
				if (e == XMLStreamConstants.START_ELEMENT) {
					if (root == null || reader.getLocalName().equals(root)) {
						return reader;
					}
					fastForwardToEndOfElement(reader);
				}
			}
		} catch (XMLStreamException e) {
			if (e.getNestedException() != null) {
				log.warn(e.getNestedException().getLocalizedMessage());
			} else {
				log.warn(e.getLocalizedMessage());
			}
			if (log.isDebugEnabled()) {
				log.debug("getReader", e);
			}
		} catch (Exception e) {
			log.warn(e.getLocalizedMessage());
			if (log.isDebugEnabled()) {
				log.debug("getReader", e);
			}
		}
		return null;
	}

	/**
	 * Helper to map a Stream Location into a String in a standardized way.
	 * 
	 * @param loc
	 *            stream location to convert
	 * @return <code>???</code> if location is <code>null</code>, a String
	 *         which describes the location otherwise.
	 */
	public static final String xmlLocation2string(Location loc) {
		return loc == null ? "???" : loc.getLineNumber() + ","
			+ loc.getColumnNumber() + " (" + loc.getSystemId() + ")";
	}

	/**
	 * Forward the cursor of the stream to the end of the current start element. *
	 * 
	 * @param reader
	 *            a reader, whoms cursor points to an start element
	 * @throws XMLStreamException
	 */
	public static final void fastForwardToEndOfElement(XMLStreamReader reader)
		throws XMLStreamException
	{
		if (reader == null) {
			return;
		}
		int starts = 1;
		while (reader.hasNext()) {
			int i = reader.next();
			if (i == XMLStreamConstants.END_ELEMENT) {
				starts-- ;
				if (starts == 0) {
					return;
				}
			} else if (i == XMLStreamConstants.START_ELEMENT) {
				starts++ ;
			}
		}
	}

	/**
	 * Get the given data as hexdump.
	 * @param data	data to format
	 * @return the hexdump
	 */
	public static StringBuilder hexdump(byte[] data) {
		if (data == null || data.length == 0) {
			return new StringBuilder();
		}
		String eol = System.getProperty("line.separator");
		int rows = data.length / 16;
		StringBuilder buf = new StringBuilder();
		StringBuilder buf2 = new StringBuilder();
		int offset = 0;
		for (int i = 0; i < rows; i++ ) {
			buf.setLength(0);
			buf2.setLength(0);
			buf.append("0x");
			String tmp = Integer.toHexString(i * 16);
			for (int k = 4 - tmp.length(); k > 0; k-- ) {
				buf.append('0');
			}
			buf.append(tmp).append(":  ");
			if (i == 15) {
				tmp = "";
			}
			int cnt = 0;
			int cnt2 = 0;
			for (int k = 0; k < 16; k++ , cnt++ , offset++ ) {
				if (cnt == 2) {
					buf.append(' ');
					cnt = 0;
					cnt2++ ;
					if (cnt2 == 4) {
						buf.append(' ');
					}
				}
				int val = 0x00ff & data[offset];
				if (val < 0x10) {
					buf.append('0');
				}
				buf.append(Integer.toHexString(val));
				char c = (char) val;
				if (val > 31 && val < 128) {
					buf2.append(c);
				} else {
					buf2.append('.');
				}
			}
			buf.append("   ").append(buf2).append(eol);
		}
		if (rows * 16 == data.length) {
			return buf;
		}
		buf.setLength(0);
		buf2.setLength(0);
		buf.append("0x");
		String tmp = Integer.toHexString(rows * 16);
		for (int k = 4 - tmp.length(); k > 0; k-- ) {
			buf.append('0');
		}
		buf.append(tmp).append(":  ");
		int cnt = 0;
		int cnt2 = 0;
		for (int k = rows * 16; k < data.length; k++ , cnt++ ) {
			if (cnt == 2) {
				buf.append(' ');
				cnt = 0;
				cnt2++ ;
				if (cnt2 == 4) {
					buf.append(' ');
				}
			}
			int val = 0x00ff & data[k];
			if (val < 0x10) {
				buf.append('0');
			}
			buf.append(Integer.toHexString(val));
			char c = (char) val;
			if (val > 31 && val < 128) {
				buf2.append(c);
			} else {
				buf2.append('.');
			}
		}
		cnt = 16 - buf2.length();
		for (int k = 0; k < cnt; k++ ) {
			buf.append("  ");
		}
		cnt /= 2;
		for (int k = 0; k < cnt; k++ ) {
			buf.append(" ");
		}
		if (cnt2 < 4) {
			buf.append(' ');
		}
		buf.append("   ").append(buf2).append(eol);
		return buf;
	}
}
