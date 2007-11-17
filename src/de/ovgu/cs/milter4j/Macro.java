/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j;

/**
 * An annotated collection of known sendmail macros (OP: is actually the text 
 * found in the "sendmail Install and Operation Guide" version 8.14.2).
 * <p>
 * NOTE: <em>Unstable class</p>, i.e. this list might not be accurate wrt. 
 * the sendmail version in use and may even miss macro names.
 * <p>
 * @see   "sendmail Operation Guide, section 5.2"
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Macro {
	/**
	 * OP:  The mechanism used for SMTP authentication (only set if successful).
	 * <p>
	 * envelope,srvrsmtp,usersmtp: SASL */
	AUTH_TYPE,
	/**
	 * OP:  The client's authentication credentials as determined by 
	 * authentication (only set if successful). The format depends on the 
	 * mechanism used, it might be just 'user', or 'user@realm', or something 
	 * similar (SMTP AUTH only).
	 * <p>
	 * envelope,srvrsmtp: SASL */
	AUTH_AUTHEN,
	/**
	 * OP:  The authorization identity, i.e. the <code>AUTH=</code> parameter 
	 * of the SMTP MAIL command if supplied.
	 * <p>
	 * envelope,srvrsmtp: SASL */
	AUTH_AUTHOR,
	/**
	 * OP:  The keylength (in bits) of the symmetric encryption algorithm used 
	 * for the security layer of a SASL mechanism.
	 * <p>
	 * envelope,srvrsmtp: SASL */
	AUTH_SSF,
	/**
	 * OP:  The DN (distinguished name) of the CA (certificate authority) that 
	 * signed the presented certificate (the cert issuer) (STARTTLS only).
	 * <p>
	 * envelope,tls: START_TLS */
	CERT_ISSUER,
	/**
	 * OP:  The DN of the presented certificate (called the cert subject) 
	 * (STARTTLS only).
	 * <p>
	 * envelope,tls: START_TLS */
	CERT_SUBJECT,
	/**
	 * OP:  The MD5 hash of the presented certificate (STARTTLS only).
	 * <p>
	 * envelope,tls: START_TLS */
	CERT_MD5,
	/**
	 * OP:  The effective keylength (in bits) of the symmetric encryption 
	 * algorithm used for a TLS connection.
	 * <p>
	 * envelope,tls: START_TLS */
	CIPHER_BITS,
	/**
	 * OP:  The cipher suite used for the connection, e.g., 
	 * EDH-DSS-DES-CBC3-SHA, EDH-RSA-DESCBC-SHA, DES-CBC-MD5, DES-CBC3-SHA 
	 * (STARTTLS only).
	 * <p>
	 * envelope,tls: START_TLS */
	CIPHER,
	/**
	 * OP:  The TLS/SSL version used for the connection, e.g., TLSv1, SSLv3, 
	 * SSLv2; defined after <code>STARTTLS</code> has been used.
	 * <p>
	 * envelope,tls: START_TLS */
	TLS_VERSION,
	/**
	 * OP: The result of the verification of the presented cert; only defined 
	 * after <code>STARTTLS</code> has been used. Possible values are:
	 * <pre>
	 * 		<code>OK</code>        verification succeeded.
	 * 		<code>NO</code>        nocert presented.
	 * 		<code>NOT</code>       no cert requested.
	 * 		<code>FAIL</code>      cert presented but could not be verified, 
	 *                    e.g., the signing CA is missing.
	 * 		<code>NONE</code>      STARTTLS has not been performed.
	 * 		<code>TEMP</code>      temporary error occurred.
	 * 		<code>PROTOCOL</code>  some protocol error occurred.
	 * 		<code>SOFTWARE</code>  STARTTLS handshake failed, which is a fatal 
	 *                    error for this session, the e-mail will be queued.
	 * </pre>
	 * There are three types of dates that can be used. The <code>$a</code> and 
	 * <code>$b</code> macros are in RFC 822 format; <code>$a</code> is the 
	 * time as extracted from the "Date:" line of the message (if there was 
	 * one), and <code>$b</code> is the current date and time (used for 
	 * postmarks). If no "Date:" line is found in the incoming message, 
	 * <code>$a</code> is set to the current time also. The <code>$d</code> 
	 * macro is equivalent to the $b macro in UNIX (ctime) format.
	 * <p>
	 * envelope,tls: START_TLS, deliver */
	VERIFY,
	/** 
	 * OP:  The maximum keylength (in bits) of the symmetric encryption 
	 * algorithm used for a TLS connection. This may be less than the effective 
	 * keylength, which is stored in <code>${cipher_bits}</code>, for 
	 * "export controlled" algorithms.
	 * <p>
	 * envelope,tls: _FFR_TLS_1 */
	ALG_BITS,
	/**
	 * OP: The CN (common name) of the CA that signed the presented certificate 
	 * (STARTTLS only).
	 * <p>
	 * envelope: _FFR_TLS_1 */
	CN_ISSUER,
	/**
	 * OP:  The CN (common name) of the presented certificate (STARTTLS only).
	 * <p>
	 * envelope,tls: _FFR_TLS_1 */
	CN_SUBJECT,
	/**
	 * OP:  The quarantine reason for the envelope, if it is quarantined.
	 * <p>
	 * envelope,deliver,milter,parseaddr,queue,srvrsmtp: quarantine message */
	QUARANTINE,
	/**
	 * OP: The output of the time(3) function, i.e., the number of seconds 
	 * since 0 hours, 0 minutes, 0 seconds, January 1, 1970, Coordinated 
	 * Universal Time (UTC).
	 * <p> 
	 * envelope: time in millis */
	TIME,
	/**
	 * OP:  A numeric representation of the current time in the format 
	 * YYYYMMDDHHmm (4 digit year 1900-9999, 2 digit month 01-12, 2 digit day 
	 * 01-31, 2 digit hours 00-23, 2 digit minutes 00-59).
	 * <p>
	 * envelope: time as yyyyMMddHHmm */
	T,
	/**
	 * OP: The current date in UNIX (ctime) format.
	 * <p>
	 * envelope: date 'EEE MMM dd HH:mm:ss yyyy' */
	D,
	/**
	 * OP: The current date in RFC 822 format.
	 * <p>
	 * envelope,main: arpadate 'EEE, dd MMM yyyy HH:mm:ss z'/current time */
	B,
	/**
	 * OP: The origination date in RFC 822 format. 
	 * This is extracted from the <code>Date: </code> line. 
	 * <p>
	 * collect,envelope,headers,main,savemail: arpadate/posted date */
	A,
	/**
	 * OP:  The full name of the sender.
	 * <p>
	 * headers,savemail: senders full name/Mail Delivery Subsystem */
	X,
	/**
	 * OP: The type of the address which is currently being rewritten. This 
	 * macro contains up to three characters, the first is either 'e' or 'h' 
	 * for envelope/header address, the second is a space, and the third is 
	 * either 's' or 'r' for sender/recipient address.
	 * <p>
	 * headers,main,parseaddr,queue,recipient,srvrsmtp: address type 
	 * ({h|e}[ s|r]) aka header|envelop sender|recipient */
	ADDR_TYPE,
	/**
	 * OP: The envelope sender (from) address.
	 * <p>
	 * header,savemail: sender */
	F,
	/**
	 * OP:  The home directory of the recipient.
	 * <p>
	 * alias,deliver,savemail: user home */
	Z,
	/**
	 * OP:  The recipient user.
	 * <p>
	 * alias,deliver,headers,parseaddr,savemail: user_name/orig recipient */
	U,
	/**
	 * OP: The recipient host. This is set in ruleset 0 from the <code>$@</code>
	 * field of a parsed address.
	 * <p>
	 * alias,deliver,parseaddr,srvrsmtp: user_host */
	H,
	/**
	 * OP:  The network family if the daemon is accepting network connections. 
	 * Possible values include "inet", "inet6", "iso", "ns", "x.25"
	 * <p>
	 * C daemon: socket factory (unspec|local|inet|inet6|iso|ns|x.25) */
	DAEMON_FAMILY,
	/**
	 * OP:  The name of the daemon from <code>DaemonPortOptions</code> 
	 * <code>Name=</code> suboption. If this suboption is not set, "Daemon#", 
	 * where # is the daemon number, is used.
	 * <p>
	 * C daemon: name of the daemon */
	DAEON_NAME,
	/**
	 * OP:  The flags for the daemon as specified by the <code>Modifier=</code> 
	 * part of DaemonPortOptions whereby the flags are separated from each other 
	 * by spaces, and upper case flags are doubled. That is, 
	 * <code>Modifier=Ea</code> will be represented as "EE a" in 
	 * <code>${daemon_flags}</code>, which is required for testing the flags in 
	 * rulesets.
	 * <p>
	 * C daemon,main,readcf: additional flags ("CC f"|"c u")*/
	DAEMON_FLAGS,
	/**
	 * OP:  The IP address the daemon is listening on for connections.
	 * <p>
	 * C daemon: IP address */
	DAEMON_ADDR,
	/**
	 * OP:  The port the daemon is accepting connection on. Unless 
	 * <code>DaemonPortOptions</code> is set, this will most likely be "25".
	 * <p>
	 * C daemon: listening port */
	DAEMON_PORT,
	/**
	 * OP: Holds the result of the resolve call for <ocde>${client_name}</code>. 
	 * Possible values are:
	 * <pre>
	 * 		<code>OK</code>     resolved successfully
	 * 		<code>FAIL</code>   permanent lookup failure
	 * 		<code>FORGED</code> forward lookup doesn't match reverse lookup
	 * 		<code>TEMP</code>   temporary lookup failure
	 * </pre>
	 * Defined in the SMTP server only. sendmail performs a hostname lookup on 
	 * the IP address of the connecting client. Next the IP addresses of that 
	 * hostname are looked up. If the client IP address does not appear in that 
	 * list, then the hostname is maybe forged. This is reflected as the value 
	 * <code>FORGED</code> for <code>${client_resolve}</code> and it also shows 
	 * up in <code>$_</code> as "(may be forged)".
	 * <p> 
	 * C daemon,deliver: result of IP addr resolution of client connection:  
	 * TEMP | FAIL | OK | FORGED */
	CLIENT_RESOLVE,
	/**
	 * OP: The hostname associated with the interface of an incoming connection. 
	 * This macro can be used for <code>SmtpGreetingMessage</code> and 
	 * <code>HReceived</code> for virtual hosting. For example:
	 * <pre>
	 * O SmtpGreetingMessage=$?{if_name}${if_name}$|$j$. MTA 
	 * </pre>
	 * <p>
	 * C daemon: interface used */
	IF_NAME,
	/**
	 * OP:  The IP address of the interface of an incoming connection unless 
	 * it is in the loopback net. IPv6 addresses are tagged with "IPv6:" before 
	 * the address.
	 * <p>
	 * C daemon: addr of the interface used (<code>null</code> if on loopback) */
	IF_ADDR,
	/**
	 * OP:  The IP family of the interface of an incoming connection unless it 
	 * is in the loopback net.
	 * <p>
	 * C daemon: interface socket factory (<code>null</code> if on loopback) */
	IF_FAMILY,
	/**
	 * OP: The flags specified by the <code>Modifier=</code> part of 
	 * <code>ClientPortOptions</code> where flags are separated from each other 
	 * by spaces and upper case flags are doubled. That is, 
	 * <code>Modifier=hA</code> will be represented as "h AA" in 
	 * <code>${client_flags}</code>, which is required for testing the flags in 
	 * rulesets.
	 * <p> 
	 * C daemon: client flags */
	CLIENT_FLAGS,
	/**
	 * OP: The IP address of the interface of an outgoing connection unless it 
	 * is in the loopback net. IPv6 addresses are tagged with "IPv6:" before 
	 * the address.
	 * <p> 
	 * C daemon: IP addr of the outgoing interface */
	IF_ADDR_OUT,
	/**
	 * OP:  The IP family of the interface of an outgoing connection unless it 
	 * is in the loopback net.
	 * <p>
	 * C daemon: socket factory of the outgoing interface */
	IF_FAMILY_OUT,
	/**
	 * OP:  The name of the interface of an outgoing connection.
	 * <p>
	 * C daemon: hostname of the outgoing interface */
	IF_NAME_OUT,
	/**
	 * OP:  The envelope id parameter (<code>ENVID=</code>) passed to sendmail 
	 * as part of the envelope.
	 * <p>
	 * deliver: envelope id */
	ENVID,
	/**
	 * OP:  The message body type (7BIT or 8BITMIME), as determined from the 
	 * envelope.
	 * <p>
	 * deliver: type of body */
	BODYTYPE,
	/**
	 * OP:  The host name of the SMTP client. This may be the client's bracketed 
	 * IP address in the form <code>[nnn.nnn.nnn.nnn]</code> for IPv4 and 
	 * <code>[IPv6:nnnn:...:nnnn]</code> for IPv6 if the client's IP address is 
	 * not resolvable, or if it is resolvable but the IP address of the resolved 
	 * hostname doesn't match the original IP address. Defined in the SMTP 
	 * server only. See also <code>${client_resolve}</code>.
	 * <p>
	 * deliver,main: client hostname */
	CLIENT_NAME,
	/**
	 * OP:  The result of the PTR lookup for the client IP address. Note: this 
	 * is the same as <code>${client_name}</code> if and only if 
	 * <code>${client_resolve}</code> is <code>OK</code>. Defined in the SMTP 
	 * server only.
	 * <p>
	 * deliver,main: client pointer (IP address) */
	CLIENT_PTR,
	/**
	 * OP: The IP address of the SMTP client. IPv6 addresses are tagged with 
	 * "IPv6:" before the address. Defined in the SMTP server only.
	 * <p> 
	 * deliver,main: IP address */
	CLIENT_ADDR,
	/**
	 * OP:  The port number of the SMTP client. Defined in the SMTP server only.
	 * <p>
	 * deliver,main: client port */
	CLIENT_PORT,
	/**
	 * OP: The sender address relative to the recipient. For example, if 
	 * <code>$f</code>is "foo", <code>$g</code> will be "host!foo", 
	 * "foo@host.domain", or whatever is appropriate for the receiving mailer. 
	 * <p>
	 * deliver,headers,savemail: return path/sender */
	G,
	/** deliver,main,srvrsmtp: DSN notification ({SUCCESS,FAILURE,DELAY}|NEVER) */
	DSN_NOTIFY,
	/**
	 * OP:  The name of the server of the current outgoing SMTP or LMTP 
	 * connection.
	 * <p>
	 * deliver: server's hostname */
	SERVER_NAME,
	/**
	 * OP:  The address of the server of the current outgoing SMTP connection. 
	 * For LMTP delivery the macro is set to the name of the mailer.
	 * <p>
	 * deliver: server's addr */
	SERVER_ADDR,
	/**
	 * OP:  Header value as quoted string (possibly truncated to 
	 * <code>MAXNAME</code>). This macro is only available in header check 
	 * rulesets.
	 * <p>
	 * headers: header value */
	CURR_HEADER,
	/**
	 * OP:  The name of the header field for which the current header check 
	 * ruleset has been called. This is useful for a default header check 
	 * ruleset to get the name of the header; the macro is only available in 
	 * header check rulesets.
	 * <p>
	 * headers: header name */
	HDR_NAME,
	/**
	 * OP:  The length of the header value which is stored in 
	 * <code>${currHeader}</code> (before possible truncation). If this value 
	 * is greater than or equal to <code>MAXNAME</code> the header has been 
	 * truncated.
	 * <p>
	 * headers: header value length */
	HDRLEN,
	/**
	 * OP:  The value of the <code>Message-Id: </code> header.
	 * <p>
	 * headers: message id */
	MSG_ID,
	/** OP: The hop count. This is a count of the number of 
	 * <code>Received: </code> lines plus the value of the <code>-h</code> 
	 * command line flag.
	 * <p>
	 * headers, main: hop count */
	C,
	/**
	 * OP:  The name of the daemon (for error messages). Defaults to 
	 * "MAILER-DAEMON".
	 * <p>
	 * macro: MAILER-DAEMON */
	N,
	/**
	 * OP:  The version number of the sendmail binary.
	 * <p>
	 * main: version */
	V,
	/**
	 * OP:  The hostname of this site. This is the root name of this host (but 
	 * see below for caveats).
	 * <p>
	 * main,readcf: Canonical name */
	W,
	/**
	 * OP:  The "official" domain name for this site. This is fully qualified 
	 * if the full qualification can be found. It must be redefined to be the 
	 * fully qualified domain name if your system is not configured so that 
	 * information can find it automatically.
	 * <p>
	 * main: Canonical name */
	J,
	/**
	 * OP: The domain part of the gethostname return value. Under normal 
	 * circumstances, <code>$j</code> is equivalent to <code>$w.$m.</code>
	 * <p> 
	 * main: node name */
	M,
	/**
	 * OP:  The UUCP node name (from the uname system call).
	 * <p>
	 * main: UUCP node name */
	K,
	/**
	 * OP:  Sender's host name. Set from the <code>-p</code> command line flag 
	 * or by the SMTP server code (in which case it is set to the EHLO/HELO 
	 * parameter).
	 * <p>
	 * main,savemail,srvrsmtp: protocol/real hostname/sending hostname */
	S,
	/**
	 * OP:  Protocol used to receive the message. Set from the <code>-p</code> 
	 * command line flag or by the SMTP server code.
	 * <p>
	 * main,savemail,srvrsmtp: protocol [options] */
	R,
	/** main,queue,srvrsmtp: what to return */
	DSN_RET,
	/** main,queue,srvrsmtp: set "original" envelope id [from ESMTP] */
	DSN_ENVID,
	/**
	 * OP:  The validated sender address. See also <code>${client_resolve}</code>.
	 * <p>
	 * main,savemail: authinfo */
	_,
	/**
	 * OP:  Some information about a daemon as a text string. For example, 
	 * "SMTP+queueing@00:30:00".
	 * <p>
	 * main: type of daemon */
	DAEMON_INFO,
	/**
	 * OP: The queue run interval given by the <code>-q</code> flag. For 
	 * example, <code>-q30m</code> would set <code>${queue_interval}</code> to 
	 * "00:30:00".
	 * <p> 
	 * main: queue interval */
	QUEUE_INTERVAL,
	/**
	 * OP: The number of delivery attempts.
	 * <p> 
	 * main,queue,srvrsmtp: number of tries */
	NTRIES,
	/**
	 * OP:  The number of validated recipients for a single message. Note: 
	 * since recipient validation happens after <i>check_rcpt</i> has been 
	 * called, the value in this ruleset is one less than what might be expected.
	 * <p>
	 * main,recipient,srvrsmtp: number of recipients */
	NRCPTS,
	/**
	 * OP: The value of the <code>SIZE=</code> parameter, i.e., usually the 
	 * size of the message (in an ESMTP dialogue), before the message has been 
	 * collected, thereafter the message size as computed by sendmail (and can 
	 * be used in check_compat).
	 * <p> 
	 * main,queue,srvrsmtp: message size */
	MSG_SIZE,
	/**
	 * OP:  The queue id, e.g., "f344MXxp018717".
	 * <p>
	 * queue: envelop ID */
	I,
	/** queue: queue directory */
	QUEUE,
	/**
	 * OP:  The number of incoming connections for the client IP address over 
	 * the time interval specified by <code>ConnectionRateWindowSize</code>.
	 * <p>
	 * ratectl: client connection rate */
	CLIENT_RATE,
	/**
	 * OP:  The total number of incoming connections over the time interval 
	 * specified by <code>Connection-RateWindowSize</code>.
	 * <p>
	 * ratectl: total connection rate */
	TOTAL_RATE,
	/**
	 * OP:  The number of open connections in the SMTP server for the client 
	 * IP address.
	 * <p>
	 * ratectl: client connections up to now */
	CLIENT_CONNECTIONS,
	/**
	 * OP:  The number of bad recipients for a single message.
	 * <p>
	 * srvrsmtp: number of bad recipients */
	NBADRCPTS,
	/**
	 * OP:  The mailer from the resolved triple of the address given for the 
	 * SMTP <code>MAIL</code> command. Defined in the SMTP server only.
	 * <p>
	 * srvrsmtp: mailer */
	MAIL_MAILER,
	/**
	 * OP:  The host from the resolved triple of the address given for the 
	 * SMTP <code>MAIL</code> command. Defined in the SMTP server only.
	 * <p>
	 * srvrsmtp: mail host */
	MAIL_HOST,
	/**
	 * OP:  The address part of the resolved triple of the address given for 
	 * the SMTP <code>MAIL</code> command. Defined in the SMTP server only.
	 * <p>
	 * srvrsmtp: user addr */
	MAIL_ADDR,
	/** srvrsmtp: "real" sender address available */
	MAIL_FROM,
	/**
	 * OP: The mailer from the resolved triple of the address given for the SMTP 
	 * <code>RCPT</code> command. Defined in the SMTP server only after a 
	 * <code>RCPT</code> command.
	 * <p> 
	 * srvrsmtp: recipient mailer */
	RCPT_MAILER,
	/**
	 * OP: The host from the resolved triple of the address given for the SMTP 
	 * <code>RCPT</code> command. Defined in the SMTP server only after a 
	 * <code>RCPT</code> command.
	 * <p> 
	 * srvrsmtp: recipient host nae*/
	RCPT_HOST,
	/**
	 * OP:  The address part of the resolved triple of the address given for 
	 * the SMTP <code>RCPT</code> command. Defined in the SMTP server only 
	 * after a <code>RCPT</code> command.
	 * <p>
	 * srvrsmtp: recipient addr */
	RCPT_ADDR,
	/**
	 * OP:  The current delivery mode sendmail is using. It is initially set to 
	 * the value of the Delivery-Mode option.
	 * <p>
	 * util: delivery mode */
	DELIVERY_MODE,
	/**
	 * OP:  The current load average.
	 * <p>
	 * conf: load average */
	LOAD_AVG,
	/** OP: Sendmail's process id. */
	P,
	/** OP: Default format of sender address. The <code>$q</code> macro 
	 * specifies how an address should appear in a message when it is defaulted. 
	 * Defaults to <code>&lt;$g&gt;</code>. It is commonly redefined to be 
	 * <code>$?x$x &lt;$g&gt;$|$g$.</code> or <code>$g$?x ($x)$.</code>, 
	 * corresponding to the following two formats:
	 * <pre>
	 * Eric Allman <eric@CS.Berkeley.EDU>
	 * eric@CS.Berkeley.EDU (Eric Allman)
	 * <p>
	 * Sendmail properly quotes names that have special characters if the first 
	 * form is used.
	 */
	Q,
	/**
	 * OP: The current operation mode (from the <code>-b</code> flag).
	 */
	OPMODE,
	;
	
	/**
	 * Get sendmail's official name of the macro.
	 */
	@Override
	public String toString() {
		if (name().length() == 1) {
			return name().toLowerCase();
		}
		switch (this) {
			case CURR_HEADER:
				return "currHeader";
			case DELIVERY_MODE:
				return "deliveryMode";
			case OPMODE:
				return "opMode";
			default:
				return '{' + name().toLowerCase() + '}';
		}
	}
}
