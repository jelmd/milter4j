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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.mail.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ovgu.cs.milter4j.cmd.BodyPacket;
import de.ovgu.cs.milter4j.cmd.ConnectPacket;
import de.ovgu.cs.milter4j.cmd.HeaderPacket;
import de.ovgu.cs.milter4j.cmd.HeloPacket;
import de.ovgu.cs.milter4j.cmd.MacroPacket;
import de.ovgu.cs.milter4j.cmd.MailFromPacket;
import de.ovgu.cs.milter4j.cmd.RecipientToPacket;
import de.ovgu.cs.milter4j.cmd.Type;
import de.ovgu.cs.milter4j.cmd.UnknownCmdPacket;
import de.ovgu.cs.milter4j.reply.AcceptPacket;
import de.ovgu.cs.milter4j.reply.ContinuePacket;
import de.ovgu.cs.milter4j.reply.NegotiationPacket;
import de.ovgu.cs.milter4j.reply.Packet;
import de.ovgu.cs.milter4j.reply.SkipPacket;
import de.ovgu.cs.milter4j.util.Mail;

/**
 * A Worker (mail filter proxy), which handles a single connection initiated by 
 * the MTA and multiplexes requests to plugged in mail filters and vice versa.
 * <p>
 * For transparency reasons, this worker does not support/will not negotiate 
 * {@link Option#HDR_LEADSPC}.
 * <p>
 * This worker honors the the {@link AcceptPacket#isFinal()} property, i.e. if a
 * mail filter returns an accept packet, where the {@code isFinal()} returns
 * {@code true}, it stops processing the message immediately (doesn't ask any 
 * other configured filters to process the current command) and returns the
 * accept message to the MTA.
 * <p>
 * <b>NOTE</b>: The order of the filters in the configuration file is important!
 * So if a filter returns a reject answer, other filters are not asked for command
 * processing and the filter returns the reject message to the MTA immediately.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Worker implements Comparable<Worker>, Callable<Object> {

	private static final Logger log = LoggerFactory.getLogger(Worker.class);
	private static final AtomicInteger instCounter = new AtomicInteger();
	
	/** max. allowed size of the data section of a packet */
	public static final int MAX_DATASIZE = 64 * 1024;
	
	/** the key which will be added to the macro map internally, if the MTA 
	 * currently connected, understands 
	 * {@link de.ovgu.cs.milter4j.reply.Type#SKIP}
	 */
	public static final String MTA_CAN_SKIP_KEY = "MTA_UNDERSTANDS_SKIP";
	
	/** The currently supported milter API version: {@value Worker#VERSION} */
	public static int VERSION = 6;
	
	private long createTime;
	private String name;
	private StatsCollector stats;
	private static final String GLOB_STAT_NAME = "NullFilter";
	
	// stuff to manage filters
	ArrayList<MailFilter> filters;
	HashSet<MailFilter> skipList;
	HashSet<MailFilter> acceptList;
	boolean quarantined;
	private SocketChannel channel;
	
	// stuff to manage data
	private EnumSet<Type> cmds2handle;	
	private EnumSet<Modification> mods2handle;
	private boolean mtaShouldSentRejected;
	private HashSet<MailFilter> assembleMessage4;
	private ByteArrayOutputStream body;
	private static final ByteBuffer NULL_BUFFER = ByteBuffer.allocate(0);
	private ByteBuffer header = ByteBuffer.allocateDirect(5);
	private ByteBuffer data;
	private Type packageType;
	HashMap<String,String> allMacros = new HashMap<String,String>();
	private HashMap<String,String> lastMacros = 
		new HashMap<String,String>();
	private HashMap<String,String> connectionMacros = 
		new HashMap<String,String>();
	ArrayList<Header> headers = new ArrayList<Header>();
	ArrayList<Packet> toSend = new ArrayList<Packet>();
	HashMap<MacroStage,HashSet<String>> macros2negotiate;
	private ReentrantLock configLock;
	
	/**
	 * Creates a new worker, which manages the given filters.
	 * @param filters		mail filter to manage
	 * @param stats where to collect statistics
	 */
	public Worker(ArrayList<MailFilter> filters, StatsCollector stats) {
		configLock = new ReentrantLock();
		this.stats = stats;
		createTime = System.currentTimeMillis();
		acceptList = new HashSet<MailFilter>(filters.size());
		skipList = new HashSet<MailFilter>(filters.size());
		name = "Mail-Worker-" + instCounter.getAndIncrement();
		reconfigure(filters);
	}
	
	/**
	 * Get the name of the worker
	 * @return the worker's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Prepare this worker to handle MTA essages related to a single mail client
	 * @param channel	channel to use for reading and writing (always a 
	 * 		blocking channel)
	 */
	public void prepare(SocketChannel channel) {
		if (this.channel != null) {
			log.warn("Old socket not cleaned up");
		}
		this.channel = channel;
		try {
			channel.socket().setSoTimeout(10*60*1000); // make sure, we get it back
		} catch (SocketException e) {
			log.warn(e.getLocalizedMessage());
		}
	}

	/**
	 * Re-Initialize this worker.
	 * @param filters	list of mail filters to manage
	 */
	private void reconfigure(ArrayList<MailFilter> filters) {
		configLock.lock();
		try {
			this.filters = filters;
			this.filters.trimToSize();
			cmds2handle = EnumSet.noneOf(Type.class);
			mods2handle = EnumSet.noneOf(Modification.class);
			mtaShouldSentRejected = false;
			assembleMessage4 = new HashSet<MailFilter>();
			macros2negotiate = new HashMap<MacroStage, HashSet<String>>(7);
			MacroStage[] stages = MacroStage.values();
			for (int i=stages.length-1; i >= 0; i--) {
				macros2negotiate.put(stages[i], new HashSet<String>());
			}
			for (MailFilter f : filters) {
				EnumSet<Type> t = f.getCommands();
				if (t != null) {
					cmds2handle.addAll(t);
				}
				EnumSet<Modification> m = f.getModifications();
				if (m != null) {
					mods2handle.addAll(m);
				}
				mtaShouldSentRejected |= f.wantsRejectedRecipients();
				if (f.reassembleMail() && t.contains(Type.BODY) 
					&& t.contains(Type.BODYEOB)) 
				{
					assembleMessage4.add(f);
				}
				for (int i=stages.length-1; i >= 0; i--) {
					Set<String> s = f.getRequiredMacros(stages[i]);
					if (s != null) {
						macros2negotiate.get(stages[i]).addAll(s);
					}
				}
			}
			NegotiationPacket p = new NegotiationPacket(null);
			// let the package normalize and fetch the normalized map
			for (int i=stages.length-1; i >= 0; i--) {
				p.setMacros(stages[i], macros2negotiate.get(stages[i]));
			}
			macros2negotiate = p.getStageMacros();
		} finally {
			configLock.unlock();
		}
	}

	/**
	 * Shutdown this worker.
	 */
	public void shutdown() {
		// synced to avoid reconfig and shutdown at the same time
		configLock.lock();
		try {
			packageType = Type.QUIT;
			cleanup(false);
			filters.clear();
			cmds2handle.clear();
			mods2handle.clear();
			mtaShouldSentRejected = false;
			assembleMessage4.clear();
			if (macros2negotiate != null) {
				macros2negotiate.clear();
			}
			stats = null;
		} finally {
			configLock.unlock();
		}
	}

	private void send(Packet p, Type cmd) throws IOException {
		if (channel != null && channel.isOpen()) {
			log.debug("Sending packet {}", p);
			if (p.getType() != de.ovgu.cs.milter4j.reply.Type.CONTINUE) {
				stats.increment(GLOB_STAT_NAME, cmd, p.getType());
			}
			p.send(channel);
		}
	}

	/**
	 * Check, whether this worker is ready to accept the next packet
	 * @return <code>false</code> if {@link #shutdown()} has been invoked
	 * 		or some a managed milter is still busy.
	 */
	public boolean isReady() {
		return channel == null;
	}
	
	/**
	 * Fille the buffer from the key's channel
	 * @param key	channel provider
	 * @param buf	buffer
	 * @return <code>true</code> if channel was closed by MTA
	 * @throws IOException on read error
	 */
	private boolean fillBuffer(ByteBuffer buf) throws IOException {
		if (buf.capacity() == 0) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping read of zero {} buffer", 
					(buf == header ? "header" : "data"));
			}
			return false;
		}
		int count = 0;
		if (log.isDebugEnabled()) {
			log.debug("Trying to read {} buffer ({} byte)", 
				(buf == header ? "header" : "data"), buf.remaining());
		}
		while (buf.hasRemaining() && ((count = channel.read(buf)) != -1)) {
			// read again
		}
		if (count == -1) {
			channel.close();
			log.debug("{} connection closed by MTA", this);
			return true;
		}
		return false;
	}

	private void negotiate(NegotiationPacket p) {
		int proto = p.getProtocolMask();
		if ((proto & Option.SKIP.getCode()) > 0) {
			allMacros.put(MTA_CAN_SKIP_KEY, "true");
		} else {
			allMacros.remove(MTA_CAN_SKIP_KEY);
		}
		EnumSet<Type> t = EnumSet.complementOf(cmds2handle);
		int skip = Type.getSkipMask(t);
		if (mtaShouldSentRejected) {
			skip |= Option.RCPT_REJ.getCode();
		}
		// for transparence reasons we never set HDR_LEADSPC
		int version = p.getVersion();
		if (version < 2 || version > VERSION) {
			p.setVersion(VERSION);
		}
		p.setModificationMask(Modification.getCode(mods2handle));
		p.setProtocolMask(skip);
		p.setStageMacros(macros2negotiate);
	}

	/**
 	 * Check, whether we need to create a new task process the given command
 	 * @param cmd	comand to act on
 	 * @return a possibly empty list of filters, which need to be run
 	 */
	private ArrayList<MailFilter> needTask(Type cmd) {
		ArrayList<MailFilter> f = new ArrayList<MailFilter>();
		if (filters.isEmpty()) {
			return f;
		}
		for (MailFilter mf : filters) {
			if (mf.getCommands().contains(cmd)) {
				if (!acceptList.contains(mf)
					&& (cmd != Type.BODY || !skipList.contains(mf))) 
				{
					f.add(mf);
				}
			}
		}
		return f;
	}
	
	/**
	 * Clean up the stack and prepare to handle a new mail connection/message
	 * @param forNewMessage	if <code>false</code>, drop meta information from
	 * 		helo/connect as well
	 */
	void cleanup(boolean forNewMessage) {
		if (channel == null) {
			// avoid multiple invocations
			return;
		}
		log.debug("{} cleaning up ...", this);
		allMacros.clear();
		if (forNewMessage) {
			allMacros.putAll(connectionMacros);
			if (filters.size() > 0) {
				for (MailFilter f : filters) {
					f.doAbort();
				}
			}
		} else {
			if (filters.size() > 0) {
				for (MailFilter f : filters) {
					f.doQuit();
				}
			}
			connectionMacros.clear();
			try { channel.close(); } catch (IOException e) { 
				/* ignore */ 
			}
			log.debug("channel closed");
			channel = null;
		}
		toSend.clear();
		lastMacros.clear();
		headers.clear();
		acceptList.clear();
		skipList.clear();
		body = null;
		data = null;
		header.clear();
		log.debug("{} done.", this);
	}

	/**
	 * Handle the results of a filter call
	 * @param filter	the filter, which produced the given packets
	 * @param cmd		the command, that was used for filter invocation 
	 * @param res	answer packets produced by a mail filter
	 * @return <code>true</code> if the final decision has been made and no
	 * 		further filter invocations should occure.
	 * @throws IOException on I/O error
	 */
	private boolean handleResult(MailFilter filter, Type cmd, Packet... res) 
		throws IOException 
	{
		Packet result = null;
		boolean stop = false;
		for (Packet p : res) {
			if (p == null) {
				stats.increment(filter.getStatName(), cmd, 
					de.ovgu.cs.milter4j.reply.Type.CONTINUE);
			}
			if (result == null && p != null && !stop) {
				de.ovgu.cs.milter4j.reply.Type r = p.getType();
				stats.increment(filter.getStatName(), cmd, r);
				switch (r) {
					case REJECT:
					case TEMPFAIL:
						if (cmd == Type.RCPT) {
							result = p;
							break;
						}
						// else fall through, i.e. reject connection|message
					case DISCARD:
					case REPLYCODE:
						result = p;
						toSend.clear();
						stop = true;
						break;
					case SKIP:
						skipList.add(filter);
						if (skipList.containsAll(filters)) {
							result = new SkipPacket();
							stop = true;
						}
						break;
					case ACCEPT:
						acceptList.add(filter);
						if (acceptList.containsAll(filters)
							|| ((AcceptPacket) p).isFinal()) 
						{
							result = new AcceptPacket(false);
							stop = true;
						}
						break;
					case OPTNEG:
					case SETSYMLIST:
					case CONN_FAIL:
						log.warn("filter {} replied with illegal packet {}",
							filter.getName(), r);
						break;
					case ADDHEADER:		// EOM
					case ADDRCPT:		// EOM
					case ADDRCPT_PAR:	// EOM
					case CHGFROM:		// EOM
					case CHGHEADER:		// EOM
					case DELRCPT:		// EOM
					case INSHEADER:		// EOM
					case REPLBODY:		// EOM
					case QUARANTINE:	// EOM
					case PROGRESS:		// EOM
						if (cmd == Type.BODYEOB) {
							send(p, cmd);
						} else {
							toSend.add(p);
						}
						break;
					case CONTINUE:
					case SHUTDOWN:
						break;
					default:
						log.warn("filter {} replied with unknown packet {}",
							filter.getName(), r);
				}
			}
			if (stop) {
				break;
			}
		}
		if (result != null) {
			send(result, cmd);
		}
		return stop;
	}

	/**
	 * Handle Packets
	 * @param cmd	the command name
	 * @param data	the data (payload) of the command package received
	 * @return {@code true} if last packet has been sent, i.e. connection 
	 * 		can be closed (which usually happens for {@code QUIT*} commands, 
	 * 		only).
	 * @throws IOException 
	 */
	private boolean handlePaket(Type cmd, ByteBuffer data) throws IOException {
		ArrayList<MailFilter> todo = needTask(cmd);
		switch (cmd) {
			case MACRO:
				final MacroPacket mp = new MacroPacket(data);
				allMacros.putAll(mp.getMacros());
				lastMacros.putAll(mp.getMacros());
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						stats.increment(f.getStatName(), cmd, 
							de.ovgu.cs.milter4j.reply.Type.CONTINUE);
						f.doMacros(allMacros, mp.getMacros());
					}
				}
				// no reply at all
				break;
			case CONNECT:
				connectionMacros.clear();
				connectionMacros.putAll(allMacros);
				if (todo.size() > 0) {
					final ConnectPacket cp = new ConnectPacket(data);
					for (MailFilter f : todo) {
						Packet p = f.doConnect(cp.getHostname(), 
							cp.getAddressFaily(), cp.getPort(), cp.getInfo());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case HELO:
				connectionMacros.putAll(lastMacros);
				if (todo.size() > 0) {
					final HeloPacket lp = new HeloPacket(data);
					for (MailFilter f : todo) {
						Packet p = f.doHelo(lp.getDomain());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case MAIL:
				lastMacros.clear();
				final MailFromPacket fp = new MailFromPacket(data);
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						Packet p = f.doMailFrom(fp.getFrom());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case RCPT:
				lastMacros.clear();
				final RecipientToPacket tp = new RecipientToPacket(data);
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						Packet p = f.doRecipientTo(tp.getRecipient());
						if (handleResult(f, packageType, p)) {
							return false;
						}
						if (p.getType() == de.ovgu.cs.milter4j.reply.Type.REJECT
							|| p.getType() == de.ovgu.cs.milter4j.reply.Type.TEMPFAIL) 
						{
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case DATA:
				lastMacros.clear();
				/* right now the milter will send macros only, but no data */
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						Packet p = f.doData();
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case HEADER:
				lastMacros.clear();
				HeaderPacket hp = new HeaderPacket(data);
				headers.add(new Header(hp.getName(), hp.getValue()));
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						Packet p = f.doHeader(hp.getName(), hp.getValue());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case EOH:
				lastMacros.clear();
				if (todo.size() > 0) {
					for (MailFilter f : todo) {
						Packet p = f.doEndOfHeader(headers, allMacros);
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case BODY:
				lastMacros.clear();
				final BodyPacket bp = new BodyPacket(data);
				if (todo.size() > 0) {
					// don't re-assemble, if nobody needs it
					if (!(assembleMessage4.isEmpty() 
						|| Collections.disjoint(todo, assembleMessage4))) 
					{
						if (body == null) {
							body = new ByteArrayOutputStream(4096);
						}
						try {
							body.write(bp.getChunk());
						} catch (IOException e) {
							log.warn(e.getLocalizedMessage());
							if (log.isDebugEnabled()) {
								log.debug("method()", e);
							}
						}
					}
					for (MailFilter f : todo) {
						Packet p = f.doBody(bp.getChunk());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case BODYEOB:
				lastMacros.clear();
				boolean quarantined = false;
				if (toSend != null && !toSend.isEmpty()) {
					for (Packet p : toSend) {
						if (p.getType() == de.ovgu.cs.milter4j.reply.Type.QUARANTINE)
						{
							if (quarantined) {
								continue;
							}
							quarantined = true;
						}
						send(p, cmd);
					}
					toSend.clear();
				}
				if (todo.size() > 0) {
					Mail msg = null;
					if (!(assembleMessage4.isEmpty() || body == null
						|| Collections.disjoint(todo, assembleMessage4))) 
					{
						msg = new Mail(headers, body.toByteArray());
						body = null;
					}
					for (MailFilter f : todo) {
						List<Packet> p = f.doEndOfMail(headers, allMacros, msg);
						if (p != null) {
							if (handleResult(f, packageType, 
								p.toArray(new Packet[p.size()]))) 
							{
								return false;
							}
						} else {
							stats.increment(f.getStatName(), packageType,
								de.ovgu.cs.milter4j.reply.Type.CONTINUE);
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case UNKNOWN:
				lastMacros.clear();
				if (todo.size() > 0) {
					final UnknownCmdPacket up = new UnknownCmdPacket(data);
					for (MailFilter f : todo) {
						Packet p = f.doBadCommand(up.getCmd());
						if (handleResult(f, packageType, p)) {
							return false;
						}
					}
				}
				send(new ContinuePacket(), cmd);
				break;
			case OPTNEG:
				final NegotiationPacket rp = new NegotiationPacket(data);
				stats.increment(GLOB_STAT_NAME, packageType, 
						de.ovgu.cs.milter4j.reply.Type.CONTINUE);
				negotiate(rp);
				send(rp, cmd);
				break;
			case QUIT:
			case QUIT_NC:
				for (MailFilter f : filters) {
					stats.increment(f.getStatName(), packageType, 
						de.ovgu.cs.milter4j.reply.Type.CONTINUE);
				}
				cleanup(false);
				return true;
			case ABORT:
				for (MailFilter f : filters) {
					stats.increment(f.getStatName(), packageType, 
						de.ovgu.cs.milter4j.reply.Type.CONTINUE);
				}
				cleanup(true);
				break;
			default:
				lastMacros.clear();
				send(new ContinuePacket(), cmd);
				log.warn("Unknown comand " + packageType + " not handled");
		}
		return false;
	}
	
	/**
	 * Read available data from the given channel and start a thread processing
	 * data if neccessary.
	 * <p>
	 * Ready for non-blocking I/O.
	 *  
	 * @return {@code true} if a complete packet has been read.
	 * @throws IOException on I/O error
	 */
	private boolean readPacket() throws IOException {
		if (header.hasRemaining()) {
			fillBuffer(header);
			if (header.hasRemaining()) {
				return false;
			}
			header.flip();
			int len = header.getInt();
			try {
				packageType = Type.get(header.get());
			} catch (Exception e) {
				throw new IOException(e.getLocalizedMessage());
			}
			if (len < 0 || len > MAX_DATASIZE) {
				throw new IOException("Invalid packet size encountered");
			}
			data = len < 2 ? NULL_BUFFER : ByteBuffer.allocate(len-1);
		}
		if (data.hasRemaining()) {
			if (log.isDebugEnabled()) {
				log.debug("Receiving {} with 1 + {} bytes of data", 
					packageType, data.remaining());
			}
			fillBuffer(data);
		}
		if (data.hasRemaining()) {
			return false;
		}
		if (log.isDebugEnabled()) {
			log.debug("{}: {} bytes of data received", packageType, data.limit());
		}
		data.flip();
		header.clear();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Worker o) {
		if (o == null) {
			return -1;
		}
		if (createTime != o.createTime) {
			return createTime > o.createTime ? 1 : -1;
		}
		return hashCode() > o.hashCode() ? 1 : -1;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object call() {
		boolean last = false;
		while(!last) {
			try {
				while(channel.isOpen() && !readPacket()) {
					// try again
				}
				last = channel != null && channel.isOpen() && data != null
					? handlePaket(packageType, data) 
					: true;
			} catch (AsynchronousCloseException e1) {
				// that's ok - may occure, if shutdown gets called
			} catch (Exception e) {
				if (channel != null) {
					log.warn(e.getClass().getSimpleName() 
						+ " " + e.getLocalizedMessage());
					if (log.isInfoEnabled()) {
						log.info("method()", e);
					}
				}
				last = true;
			}
		}
		cleanup(false);
		log.debug("{} task finished", this);
		return null;
	}
}
