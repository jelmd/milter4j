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
 * Known sendmail macros with a static name. Just for information right now 
 * - not used anywhere.
 * <p>
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public enum Macro {
	/** envelope,srvrsmtp,usersmtp: SASL */
	AUTH_TYPE,
	/** envelope,srvrsmtp: SASL */
	AUTH_AUTHEN,
	/** envelope,srvrsmtp: SASL */
	AUTH_AUTHOR,
	/** envelope,srvrsmtp: SASL */
	AUTH_SSF,
	/** envelope,tls: START_TLS */
	CERT_ISSUER,
	/** envelope,tls: START_TLS */
	CERT_SUBJECT,
	/** envelope,tls: START_TLS */
	CERT_MD5,
	/** envelope,tls: START_TLS */
	CIPHER_BITS,
	/** envelope,tls: START_TLS */
	CIPHER,
	/** envelope,tls: START_TLS */
	TLS_VERSION,
	/** envelope,tls: START_TLS, deliver */
	VERIFY,
	/** envelope,tls: _FFR_TLS_1 */
	ALG_BITS,
	/** envelope: _FFR_TLS_1 */
	CN_ISSUER,
	/** envelope,tls: _FFR_TLS_1 */
	CN_SUBJECT,
	/** envelope,deliver,milter,parseaddr,queue,srvrsmtp: quarantine message */
	QUARANTINE,
	/** envelope: time in millis */
	TIME,
	/** envelope: time as yyyyMMddHHmm */
	T,
	/** envelope: date 'EEE MMM dd HH:mm:ss yyyy' */
	D,
	/** envelope,main: arpadate 'EEE, dd MMM yyyy HH:mm:ss z'/current time */
	B,
	/** collect,envelope,headers,main,savemail: arpadate/posted date */
	A,
	/** headers,savemail: senders full name/Mail Delivery Subsystem */
	X,
	/** headers,main,parseaddr,queue,recipient,srvrsmtp: address type ({h|e}[ s|r]) aka 
	 * 	header|envelop sender|recipient */
	ADDR_TYPE,
	/** header,savemail: sender */
	F,
	/** alias,deliver,savemail: user home */
	Z,
	/** alias,deliver,headers,parseaddr,savemail: user_name/orig recipient */
	U,
	/** alias,deliver,parseaddr,srvrsmtp: user_host */
	H,
	/** C daemon: socket factory (unspec|local|inet|inet6|iso|ns|x.25) */
	DAEMON_FAMILY,
	/** C daemon: name of the daemon */
	DAEON_NAME,
	/** C daemon,main,readcf: additional flags ("CC f"|"c u")*/
	DAEMON_FLAGS,
	/** C daemon: IP address */
	DAEMON_ADDR,
	/** C daemon: listening port */
	DAEMON_PORT,
	/** C daemon,deliver: result of IP addr resolution of client connection:  
	 * TEMP | FAIL | OK | FORGED */
	CLIENT_RESOLVE,
	/** C daemon: interface used */
	IF_NAME,
	/** C daemon: addr of the interface used (<code>null</code> if on loopback) */
	IF_ADDR,
	/** C daemon: interface socket factory (<code>null</code> if on loopback) */
	IF_FAMILY,
	/** C daemon: client flags */
	CLIENT_FLAGS,
	/** C daemon: IP addr of the outgoing interface */
	IF_ADDR_OUT,
	/** C daemon: socket factory of the outgoing interface */
	IF_FAMILY_OUT,
	/** C daemon: hostname of the outgoing interface */
	IF_NAME_OUT,
	/** deliver: envelope id */
	ENVID,
	/** deliver: type of body */
	BODYTYPE,
	/** deliver,main: client hostname */
	CLIENT_NAME,
	/** deliver,main: client pointer (IP address) */
	CLIENT_PTR,
	/** deliver,main: IP address */
	CLIENT_ADDR,
	/** deliver,main: client port */
	CLIENT_PORT,
	/** deliver,headers,savemail: return path/sender */
	G,
	/** deliver,main,srvrsmtp: DSN notification ({SUCCESS,FAILURE,DELAY}|NEVER) */
	DSN_NOTIFY,
	/** deliver: server's hostname */
	SERVER_NAME,
	/** deliver: server's addr */
	SERVER_ADDR,
	/** headers: header value */
	CURR_HEADER,
	/** headers: header name */
	HDR_NAME,
	/** headers: header value length */
	HDRLEN,
	/** headers: message id */
	MSG_ID,
	/** headers, main: hop count */
	C,
	/** macro: MAILER-DAEMON */
	N,
	/** main: version */
	V,
	/** main,readcf: Canonical name */
	W,
	/** main: Canonical name */
	J,
	/** main: node name */
	M,
	/** main: UUCP node name */
	K,
	/** main,savemail,srvrsmtp: protocol/real hostname/sending hostname */
	S,
	/** main,savemail,srvrsmtp: protocol [options] */
	R,
	/** main,queue,srvrsmtp: what to return */
	DSN_RET,
	/** main,queue,srvrsmtp: set "original" envelope id [from ESMTP] */
	DSN_ENVID,
	/** main,savemail: authinfo */
	_,
	/** main: type of daemon */
	DAEMON_INFO,
	/** main: queue interval */
	QUEUE_INTERVAL,
	/** main,queue,srvrsmtp: number of tries */
	NTRIES,
	/** main,recipient,srvrsmtp: number of recipients */
	NRCPTS,
	/** main,queue,srvrsmtp: message size */
	MSG_SIZE,
	/** queue: envelop ID */
	I,
	/** queue: queue directory */
	QUEUE,
	/** ratectl: client connection rate */
	CLIENT_RATE,
	/** ratectl: total connection rate */
	TOTAL_RATE,
	/** ratectl: client connections up to now */
	CLIENT_CONNECTIONS,
	/** srvrsmtp: number of bad recipients */
	NBADRCPTS,
	/** srvrsmtp: mailer */
	MAIL_MAILER,
	/** srvrsmtp: mail host */
	MAIL_HOST,
	/** srvrsmtp: user addr */
	MAIL_ADDR,
	/** srvrsmtp: "real" sender address available */
	MAIL_FROM,
	/** srvrsmtp: recipient mailer */
	RCPT_MAILER,
	/** srvrsmtp: recipient host nae*/
	RCPT_HOST,
	/** srvrsmtp: recipient addr */
	RCPT_ADDR,
	/** util: delivery mode */
	DELIVERY_MODE,
	/** conf: load average */
	LOAD_AVG
	;
	
	/**
	 * Get the sendmail name of the macro.
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
			default:
				return '{' + name().toLowerCase() + '}';
		}
	}
}
