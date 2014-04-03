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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.mail.Header;
import javax.mail.internet.MimeMultipart;

import de.ovgu.cs.milter4j.cmd.Type;
import de.ovgu.cs.milter4j.reply.ContinuePacket;
import de.ovgu.cs.milter4j.reply.Packet;
import de.ovgu.cs.milter4j.util.Mail;
import de.ovgu.cs.milter4j.util.RequestDumper;

/**
 * An abstract mail filter, which actually handles the MTA requests.
 * <p>
 * Mail filters used by the {@link Server} must always have an constructor,
 * which takes one String as an argument. When the filter's constructor gets 
 * invoked, the server uses the value of the <code>conf</code> attribute of the 
 * filter as argument.
 * <p>
 * Once the constructor was executed sucessfully, the server obtains additional 
 * instances of the filter (if required), just by calling the 
 * {@link #getInstance()} method.
 * <p>
 * Per contract, no method is allowed to modify data in passed references. Doing
 * it possibly makes other filters misbehaving. The only one exception is the
 * {@code allMacros} map, passed to all <em>do*</em> methods - see below.
 * <p>
 * All <em>do*</em> methods are directly related to commands send by the MTA and
 * are called right after receiption of the command with the obtained
 * information. All filters are processed in sequence (back-to-back) according 
 * to their order in the milter configuration file (see {@link Configuration}) 
 * by a Worker, which owns the connection to the MTA and all filter instances.
 * The handling of a connection might be expressed very simplified as follows:
 * <pre>
 *	Worker worker = getFreeWorker();
 *	worker.handle(connection) {
 * 		List<MailFilter> filters = getFilters();
 *		while (command != QUIT) {
 *			command = con.getCommand();
 *			preprocess(command);
 *			for (MailFilter filter : filters) {
 *				if (filterWants(command, filter)) {
 *					results = filter.do$command(...);
 *					if (!process(results)) {
 *						break;
 *					}
 *				}
 *			}
 *		}
 *	}
 * </pre>
 * NOTE: This handling is different than explained in the API ControlFlow 
 * overview of the C Milter library!
 * <p>
 * For convinience each <em>do*</em> method gets passed the map with all macros
 * and values received from the MTA, even if the MailFilter did not subscribe
 * to the {@link Type#MACRO} notification. However, since the value of certain
 * macros may change depending on the processing state of it might be still
 * necessary to subscribe to the {@link Type#MACRO} notification (use the 
 * {@link RequestDumper} to find out). NOTE: You may also add your own key,value
 * pairs to the map to "communicate" with other MailFilters managed by the sae
 * milter4j server, but always make sure, that you never overwrite the macros, 
 * sent by the MTA!
 * <p>
 * NOTE: According to Milter API one may send messages, which modify the current 
 * mail only when the end of the mail has been reached 
 * ({@link #doEndOfMail(List, HashMap, Mail)}). For convinience reasons
 * however, this implementation allows filter to return those packets immediately.
 * The managing server will queue these packets and send them as soon as it gets
 * the end of mail notification from the MTA (before the {@link 
 * #doEndOfMail(List, HashMap, Mail)} of all managed filters still active gets called.
 * <p>
 * If a <code>do*</code> method returns <code>null</code>, the result will be 
 * interpreted as {@link ContinuePacket}.
 * <p>
 * If a milter returns one of the following packet types, the managing server
 * sends it back to the MTA immediately.
 * <ul>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#TEMPFAIL}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#REJECT}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#DISCARD}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#REPLYCODE}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#CONN_FAIL} if supported by 
 * 			the MTA (most do not)</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#SKIP} if returned by 
 * 			{@link #doBody(byte[], HashMap)}, supported by the MTA and no other 
 * 			managed filter needs further body chunks. Anyway, the managing 
 * 			server makes sure, that the filter, which sent that packet, will 
 * 			not receive any further body chunks.</li>
 * </ul>
 * The following commands are reserved for use by the managing server and thus
 * are ignored.
 * <ul>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#OPTNEG}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#SETSYMLIST}</li>
 * 		<li>{@link de.ovgu.cs.milter4j.reply.Type#SHUTDOWN}</li>
 * </ul>
 * 
 * @see Configuration
 * @see Server
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public abstract class MailFilter {

	/** a descriptive short name of the filter, to be used for logging, etc. 
	 * @return always a none <code>null</code> value */
	public abstract String getName();

	/**
	 * Get the name to be used for collection mail filter statistics. It should
	 * be a constant value, no matter, on which instance of this filter it gets
	 * called.
	 *  
	 * @return	the stats display name
	 */
	public String getStatName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * Get a new instance of this filter, which is already configured and 
	 * can be used for filtering.
	 * <p>
	 * The purpose of this method is to avoid the configuration overhead (e.g.
	 * reading and parsing config files, etc.).
	 * 
	 * @return a ready to work instance.
	 */
	public abstract MailFilter getInstance();
	
	/**
	 * Reconfigure this instance using the given parameter.
	 * @param param	the value of the <code>conf</code> filter element attribute
	 * 		of this managing servers configuration file.
	 * @return <code>true</code> if configuration was successful.
	 * @see Configuration
	 */
	public abstract boolean reconfigure(String param);
	
	/**
	 * Get all actions, the filter may request from the MTA.
	 * @return <code>null</code> (default) or an empty set, if it is just an 
	 * 		observer, all possible actions otherwise.
	 */
	public EnumSet<Modification> getModifications() {
		return null;
	}
	
	/**
	 * Tells the managing server, in which commands/information this filter 
	 * is interested in.
	 * <p>
	 * However, a filter must always process the follwoing commands correctly:
	 * <ul>
	 * 		<li>{@link Type#ABORT}</li>
	 * 		<li>{@link Type#QUIT}</li>
	 * </ul>
	 * On the other side, the following commands are handled by the managing
	 * server, i.e. are ignored if included in the result and the client will 
	 * never see those commands:
	 * <ul>
	 * 		<li>{@link Type#OPTNEG}</li>
	 * </ul>
	 * 
	 * @return a set of commands (default: all available commands). Might be 
	 * 		<code>null</code>, if the instance is misconfigured, etc.
	 */
	public EnumSet<Type> getCommands() {
		return EnumSet.allOf(Type.class);
	}

	/**
	 * Get a list of macros, which should be sent by the MTA, when entering the
	 * given stage of mail processing.
	 * <p>
	 * NOTE: If this method returns a none-<code>null</code> value, at least 
	 * sendmail ignores all unknown macro names and will not populate/send the 
	 * standard macros (see 
	 * <code>O Milter.macros.{connect|helo|envfrom|envrcpt|data|eoh|eom}</code>), 
	 * but the known macros returned by this method, only!
	 * 
	 * @param stage		stage in question
	 * @return <code>null</code> (default) if standard macros are sufficient, 
	 * 		the list of macro names otherwise. E.g. 
	 * 		"{rcpt_mailer}", "{rcpt_host}", ...
	 */
	public Set<String> getRequiredMacros(MacroStage stage) {
		return null;
	}

	/**
	 * Indicate, whether this filter want to have the MTA to sent all rejected 
	 * recipients.
	 * @return <code>false</code> (default), if the MTA should not be asked to 
	 * 		sent rejected recipients.
	 * @see Option#RCPT_REJ
	 */
	public boolean wantsRejectedRecipients() {
		return false;
	}
	
	/**
	 * Tell the managing server, that it should collect all data packets 
	 * received by the server and reassembles it to a complete mail.
	 * <p>
	 * This may save system resources in the case, more than one filter is 
	 * interested in reconstructing mime parts sent by the client, since this
	 * reconstructed mail would be shared by all filters, which are managed by 
	 * the same server, this filter is managed. Furthermore it frees this filter
	 * from doing the reassembling by itself.
	 * <p>
	 * The filter may access the reconstructed mail in its {@link 
	 * #doEndOfMail(List, HashMap, Mail)} method.
	 *   
	 * @return	if <code>true</code> managing server reassembles the mail by
	 * 	collecting all data packets.
	 */
	public boolean reassembleMail() {
		return false;
	}
	
	// command handling

	/**
	 * Abort the handling of the current message, i.e. release all collected
	 * information/resources associated with the current message. Since the
	 * mail client has still a connection to the MTA, it may send further
	 * messages. So a filter should not discard CONNECT/HELO information, if 
	 * they are required for filtering further messages. 
	 * <p>
	 * Make sure, that this method can be called out-of-band (from another 
	 * thread), i.e. beeing able to tell the instance to stop message processing 
	 * immediately (e.g. by setting a flag to stop the running scan engine).
	 * <p>
	 * Type: message-oriented
	 */
	public abstract void doAbort();

	/**
	 * Abort the handling of the current message, clean up completely and 
	 * prepare for handling a new connection/message.
	 * <p>
	 * Make sure, that this method can be called out-of-band (from another 
	 * thread), i.e. beeing able to tell the instance to stop message processing 
	 * immediately (e.g. by setting a flag to stop the running scan engine).
	 * <p>
	 * Type: connection-oriented
	 */
	public abstract void doQuit();
	
	/**
	 * Handle Macros sent by the MTA. Key of the maps is the name of the macro,
	 * value is the value of the macro (might be an empty String). A filter is 
	 * per contract not allowed to modify these maps, since used by other 
	 * filters as well.
	 * <p>
	 * NOTE: Depending on the macro, sendmail may prepend the single-character 
	 * name of the command, for whome the macro was prepared for (e.g.
	 * if "{msg_size}" has been requested, sendmail will sent N{msg_size}=1234
	 * right before the {@link Type#EOH} command, T{msg_size}=1234 right before 
	 * the {@link Type#DATA} command and so on).
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#MACRO}.
	 * <p>
	 * Type: none (may occure on any stage)
	 * 
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @param newMacros	new macros sent to for the current stage of mail cycle.
	 * 
	 * @see  Type#getCmd()
	 * @see  "sendmail Operation Guide, section 5.2"
	 */
	public void doMacros(HashMap<String,String> allMacros, 
		HashMap<String,String> newMacros) 
	{
		return;
	}

	/**
	 * Handle the DATA startup notification sent by the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#DATA}.
	 * <p>
	 * Type: message-oriented
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * 
	 * @return the answer to send back to the MTA.
	 */
	public Packet doData(HashMap<String,String> allMacros) {
		return new ContinuePacket();
	}

	/**
	 * Handle a single header submitted by the MTA. The header sent is the last
	 * entry in the given list. A filter is not allowed to change the given
	 * list nor to change any entries in it, since shared by other filters as 
	 * well.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#HEADER}.
	 * <p>
	 * Type: message-oriented
	 * 
	 * @param name	the header name
	 * @param value	the value of the header field (might be an empty String)
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to send back to the MTA.
	 */
	public Packet doHeader(String name, String value, 
		HashMap<String,String> allMacros) 
	{
		return new ContinuePacket();
	}
	
	/**
	 * Handle a connection request sent to the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#CONNECT}.
	 * <p>
	 * Type: connection-oriented
	 * 
	 * @param hostname the hostname of the remote mail-client
	 * @param family the address family of mail-client to MTA connection 
	 * @param port 	the port of the remote mail-client connection (-1 if
	 * 		not available)
	 * @param info 	IP address of the remote mail-client or UNIX-Path,
	 * 		<code>null</code> if not available. 
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doConnect(String hostname, AddressFamily family, int port, 
		String info, HashMap<String,String> allMacros) 
	{
		return new ContinuePacket();
	}

	/**
	 * Handle a helo sent to the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#HELO}.
	 * <p>
	 * Type: connection-oriented
	 * 
	 * @param domain	the domain or whatever the mail-client submitted via 
	 * 		HELO/EHLO
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doHelo(String domain, HashMap<String,String> allMacros) {
		return new ContinuePacket();
	}

	/**
	 * Handle a 'MAIL FROM' command sent to the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#MAIL}.
	 * <p>
	 * Type: message-oriented
	 * 
	 * @param from		'MAIL FROM:' values sent by the mail-client
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doMailFrom(String[] from, HashMap<String,String> allMacros) {
		return new ContinuePacket();
	}

	/**
	 * Handle a 'RCPT TO' command sent to the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#RCPT}.
	 * <p>
	 * Type: recipient-oriented
	 * 
	 * @param recipient		'RCPT TO:' values sent by the mail-client
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doRecipientTo(String[] recipient, 
		HashMap<String,String> allMacros) 
	{
		return new ContinuePacket();
	}

	/**
	 * Handle one piece of body chunk received from the MTA.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#BODY}.
	 * <p>
	 * If this or another filter returns <code>true</code> for 
	 * {@link #reassembleMail()}, this chunk gets collected by the managing 
	 * server and will be passed as the complete mail body in 
	 * {@link #doEndOfMail(List, HashMap, Mail)}. So if the filter does
	 * not process the data immediately, there is no need to copy/store data
	 * for later use.
	 * <p>
	 * Per contract, no filter is allowed to modify the content of the given 
	 * chunk.
	 * <p>
	 * Type: message-oriented
	 * 
	 * @param chunk		raw data received. It might be a part or the whole 
	 * 		mail body, depending on its length.
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doBody(byte[] chunk, HashMap<String,String> allMacros) {
		return new ContinuePacket();
	}

	/**
	 * Handle the end of header packet received from MTA.
	 * A filter is not allowed to change the given lists nor to change any 
	 * entries in it, since shared by other filters as well.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#EOH}.
	 * <p>
	 * Type: message-oriented
	 * 
	 * @param headers the list of headers sent by the mail client and
	 * 		added by other mail filters. It does not contain the headers added
	 * 		by the MTA itself. Also other filter may still add new ones.
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doEndOfHeader(List<Header> headers, 
		HashMap<String,String> allMacros) 
	{
		return new ContinuePacket();
	}

	/**
	 * Handle the end of mail packet received from MTA.
	 * A filter is not allowed to change the given lists nor to change any 
	 * entries in it, since shared by other filters as well.
	 * <p>
	 * The managing server will reconstruct the complete mail (i.e. collecting
	 * all neccessary body chunks and headers), if and only
	 * if {@link #reassembleMail()} returns <code>true</code> and this filter
	 * returns {@link Type#BODY} as well as {@link Type#BODYEOB} in its 
	 * {@link #getCommands()}. However, it is not required to overwrite
	 * {@link #doBody(byte[], HashMap)}.
	 * <p>
	 * If mail gets reconstructed, {@link Mail#getContent()} usually returns a 
	 * String, if it is a simple text message, and {@link MimeMultipart} if it 
	 * contains multiple MIME parts.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#BODYEOB}.
	 * <p>
	 * Type: message-oriented
	 * 
	 * @param headers the list of headers sent by the mail client and
	 * 		added by other mail filters up to now. It does not contain the 
	 * 		headers added by the MTA itself. Also other filter may still add 
	 * 		new ones.
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @param message the complete message. Might be <code>null</code> if not 
	 * 		requested.
	 * 
	 * @return a list of answers to this packet. Per default <code>null</code>.
	 * @see MimeMultipart
	 */
	public List<Packet> doEndOfMail(List<Header> headers, 
		HashMap<String,String> allMacros, Mail message) 
	{
		return null;
	}
	
	/**
	 * Handle bad/unknown SMTP commands issued by the mail-client.
	 * <p>
	 * Only called, if {@link #getCommands()} contains {@link Type#UNKNOWN}
	 * and the MTA supports this feature.
	 * <p>
	 * Type: none (may occure in any stage)
	 * 
	 * @param cmd	SMTP command issued by the client
	 * @param allMacros	all macros already sent by the MTA for the current 
	 * 		connection and message. 
	 * @return the answer to this packet. Per default a new {@link ContinuePacket}
	 */
	public Packet doBadCommand(String cmd, HashMap<String,String> allMacros) {
		return new ContinuePacket();
	}
}
