/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package com.sendmail.milter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sendmail.milter.cmd.BodyPacket;
import com.sendmail.milter.cmd.ConnectPacket;
import com.sendmail.milter.cmd.HeaderPacket;
import com.sendmail.milter.cmd.HeloPacket;
import com.sendmail.milter.cmd.MacroPacket;
import com.sendmail.milter.cmd.MailFromPacket;
import com.sendmail.milter.cmd.RecipientToPacket;
import com.sendmail.milter.cmd.Type;
import com.sendmail.milter.cmd.UnknownCmdPacket;
import com.sendmail.milter.reply.AcceptPacket;
import com.sendmail.milter.reply.NegotiationPacket;
import com.sendmail.milter.reply.Packet;
import com.sendmail.milter.reply.SkipPacket;

/**
 * A Worker (mail filter proxy), which handles a single connection initiated by 
 * the MTA and multiplexes requests to plugged in mail filters and vice versa.
 * <p>
 * For transparency reasons, this worker does not support/will not negotiate 
 * {@link Option#HDR_LEADSPC}.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class Worker implements Comparable<Worker> {

	private static final Logger log = LoggerFactory.getLogger(Worker.class);
	private static final AtomicInteger instCounter = new AtomicInteger();
	
	/** the key which will be added to the macro map internally, if the MTA 
	 * currently connected, understands 
	 * {@link com.sendmail.milter.reply.Type#SKIP}
	 */
	public static final String MTA_CAN_SKIP_KEY = "MTA_UNDERSTANDS_SKIP";
	
	/** The currently supported milter API version: {@value #VERSION} */
	public static int VERSION = 4;
	
	private boolean canUse = false;
	private long createTime;
	private String name;

	// stuff to manage filters
	private ExecutorService threadPool;
	ArrayList<MailFilter> filters;
	HashSet<MailFilter> skipList;
	HashSet<MailFilter> acceptList;
	HashSet<Future<?>> runningTasks = new HashSet<Future<?>>();
	ConcurrentLinkedQueue<Packet> sendQueue;
	
	// stuff to manage data
	private EnumSet<Type> cmds2handle;	
	private EnumSet<Modification> mods2handle;
	private boolean mtaShouldSentRejected;
	private HashSet<MailFilter> assembleMessage4;
	private ByteArrayOutputStream body;
	private static final ByteBuffer NULL_BUFFER = ByteBuffer.allocate(0);
	private ByteBuffer buf;
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

	/**
	 * Creates a new worker, which manages the given filters.
	 * @param filters		mail filter to manage
	 * @param threadPool	thread pool to use for execution
	 */
	public Worker(ArrayList<MailFilter> filters, ExecutorService threadPool) {
		this.threadPool = threadPool;
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
	 * Get the queue with the packages to send
	 * @return	a queue, which is usually not empty.
	 */
	public ConcurrentLinkedQueue<Packet> getQueue2send() {
		return sendQueue;
	}

	/**
	 * Re-Initialize this worker.
	 * @param filters	list of mail filters to manage
	 */
	private void reconfigure(ArrayList<MailFilter> filters) {
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
		sendQueue = new ConcurrentLinkedQueue<Packet>();
		buf = header;
		canUse = true;
	}

	/**
	 * Shutdown this worker.
	 */
	public void shutdown() {
		canUse = false;
		sendQueue.clear();
		cleanup(false, null);
		filters.clear();
		cmds2handle.clear();
		mods2handle.clear();
		mtaShouldSentRejected = false;
		assembleMessage4.clear();
		macros2negotiate.clear();
		sendQueue.clear();
		runningTasks.clear();
	}

	/**
	 * Check, whether this worker is ready to accept the next packet
	 * @return <code>false</code> if {@link #shutdown()} has been invoked
	 * 		or some a managed milter is still busy.
	 */
	public boolean isReady() {
		return canUse && isIdle();
	}
	
	/**
	 * Check, whether there are any filters managed by this worker, are still
	 * doing some work.
	 * @return <code>true</code> if idle.
	 */
	public boolean isIdle() {
		if (runningTasks.size() == 0) {
			return true;
		}
		Iterator<Future<?>> i = runningTasks.iterator();
		while(i.hasNext()) {
			Future<?> f = i.next();
			if (f.isDone()) {
				i.remove();
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Fille the buffer from the key's channel
	 * @param key	channel provider
	 * @param buf	buffer
	 * @throws IOException on read error
	 */
	private void fillBuffer(SelectionKey key, ByteBuffer buf) throws IOException {
		if (buf.capacity() == 0) {
			return;
		}
		SocketChannel sc = (SocketChannel) key.channel();
		int count = 0;
		while (buf.hasRemaining() && ((count = sc.read(buf)) > 0)) {
			// read again
		}
		if (count == -1) {
			sc.close();
			log.debug("connection closed");
		}
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
		p.setVersion(VERSION);
		p.setModificationMask(Modification.getCode(mods2handle));
		p.setProtocolMask(skip);
		p.setStageMacros(macros2negotiate);
	}

	class MilterFuture<V extends MilterTask> extends FutureTask<V> {
		private MilterTask callable;
		private Set<?> taskList;

		/**
		 * Just a wrapper around FutureTask, which will execute a MilterTask
		 * @param callable	a MilterTask
		 * @param taskList where to store this task
		 */
		public MilterFuture(Callable<V> callable, Set<Future<?>> taskList) {
			super(callable);
			taskList.add(this);
			this.taskList = taskList;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// avoid sending uneccessary stuff 
			callable.stop = true;
			return super.cancel(mayInterruptIfRunning);
		}

		/**
		 * Removes this task from the list of running tasks 
		 */
		@Override
		protected void done() {
			taskList.remove(this);
			taskList = null;
			callable = null;
		}
	}

	/**
	 * Just puts the packets into a chain and notifies the appropriate selector.
	 * @param key	key to use for notify
	 * @param p		packet to queue
	 */
	void send(SelectionKey key, Packet... p) {
		for (int i=0; i < p.length; i++) {
			log.debug("{} adding {} to write queue", this, p[i]);
			sendQueue.add(p[i]);
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		key.selector().wakeup();
	}

 	abstract class MilterTask implements Callable<MilterTask> {
		private final Logger log = LoggerFactory
			.getLogger(MilterTask.class);
		Type cmd;
		Packet result;
		ArrayList<Packet> results;
		boolean stop;
		ArrayList<MailFilter> todo;
		SelectionKey skey;

		/**
		 * Create a task which calls a milters
		 * @param cmd	current command to handle
		 * @param f 	mail filter to process
		 * @param sc channel to use for sending replies
		 */
		public MilterTask(Type cmd, ArrayList<MailFilter> f, SelectionKey sc) {
			this.cmd = cmd;
			stop = false;
			todo = f;
			this.skey = sc;
		}

		/**
		 * {@inheritDoc}
		 */
		public MilterTask call() {
			result = null;
			boolean abortORquit = cmd == Type.ABORT || cmd == Type.QUIT;
			LinkedList<Packet> tmpRes = new LinkedList<Packet>();
			if (cmd == Type.BODYEOB) {
				boolean quarantined = false;
				if (toSend != null && !toSend.isEmpty()) {
					Iterator<Packet> i = toSend.iterator();
					while (i.hasNext()) {
						if (stop) {
							break;
						}
						Packet p = i.next();
						if (p.getType() == com.sendmail.milter.reply.Type.QUARANTINE)
						{
							if (quarantined) {
								i.remove();
							}
							quarantined = true;
						}
					}
					send(skey, toSend.toArray(new Packet[toSend.size()]));
					toSend.clear();
				}
			}
			for (MailFilter f : todo) {
				tmpRes.clear();
				if (stop && !abortORquit) {
					// always process abort and quit
					break;
				}
				try {
					callMilter(f, tmpRes);
				} catch (Exception e) {
					log.warn(e.getLocalizedMessage());
					if (log.isDebugEnabled()) {
						log.debug("run", e);
					}
				}
				while (!(stop || tmpRes.isEmpty())) {
					Packet p = tmpRes.poll();
					if (p != null) {
						com.sendmail.milter.reply.Type r = p.getType();
						switch (r) {
							case REJECT:
							case DISCARD:
							case TEMPFAIL:
							case REPLYCODE:
								result = p;
								toSend.clear();
								stop = true;
								break;
							case SKIP:
								skipList.add(f);
								break;
							case ACCEPT:
								acceptList.add(f);
								break;
							case OPTNEG:
							case SETSYMLIST:
							case CONN_FAIL:
								log.warn("filter {} replied with illegal packet {}",
									f.getName(), r);
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
									send(skey, p);
								} else {
									toSend.add(p);
								}
								break;
							case CONTINUE:
							case SHUTDOWN:
								break;
							default:
								log.warn("filter {} replied with unknown packet {}",
									f.getName(), r);
						}
					}
				}
			}
			if (abortORquit) {
				toSend.clear();
				if (cmd == Type.QUIT || cmd == Type.QUIT_NC) {
					try { skey.channel().close(); } catch (Exception e) { /* */ }
				}
				return this;
			}
			if (result == null && cmd != Type.MACRO) {
				if (acceptList.containsAll(filters)) {
					result = new AcceptPacket();
				} else if (skipList.containsAll(filters)) {
					result = new SkipPacket();
				} else {
					result = MailFilter.CONTINUE;
				}
			}
			if (result != null && !stop) {
				send(skey, result);
			}
			return this;
		}
		
		abstract void callMilter(MailFilter f, List<Packet> res);
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
	 * @param key	not yet used
	 */
	void cleanup(boolean forNewMessage, SelectionKey key) {
		log.debug("{} cleaning up ...");
		for (Future<?> f : runningTasks) {
			f.cancel(true);
		}
		allMacros.clear();
		if (forNewMessage) {
			allMacros.putAll(connectionMacros);
			if (filters.size() > 0) {
				MilterTask r = new MilterTask(packageType, filters, key) {
					@Override
					void callMilter(MailFilter f, List<Packet> res) {
						f.doAbort();
					}
				};
				threadPool.submit(new MilterFuture<MilterTask>(r, runningTasks));
			}
		} else {
			if (filters.size() > 0) {
				MilterTask r = new MilterTask(packageType, filters, key) {
					@Override
					void callMilter(MailFilter f, List<Packet> res) {
						f.doQuit();
					}
				};
				threadPool.submit(new MilterFuture<MilterTask>(r, runningTasks));
			}
			connectionMacros.clear();
		}
		toSend.clear();
		lastMacros.clear();
		headers.clear();
		acceptList.clear();
		skipList.clear();
		body = null;
		data = null;
		header.clear();
		data.clear();
		buf = header;
		log.debug("{} done ...");
	}

	private void handlePaket(SelectionKey skey) {
		MilterTask r = null;
		ArrayList<MailFilter> todo = needTask(packageType);
		switch (packageType) {
			case MACRO:
				final MacroPacket mp = new MacroPacket(data);
				allMacros.putAll(mp.getMacros());
				lastMacros.putAll(mp.getMacros());
				if (todo.size() > 0) {
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							f.doMacros(allMacros, mp.getMacros());
						}
					};
				}
				// no reply at all
				break;
			case CONNECT:
				connectionMacros.clear();
				connectionMacros.putAll(allMacros);
				if (todo.size() > 0) {
					final ConnectPacket cp = new ConnectPacket(data);
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doConnect(cp.getHostname(), cp.getPort(), 
								cp.getInfo()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case HELO:
				connectionMacros.putAll(lastMacros);
				if (todo.size() > 0) {
					final HeloPacket lp = new HeloPacket(data);
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doHelo(lp.getDomain()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case MAIL:
				lastMacros.clear();
				final MailFromPacket fp = new MailFromPacket(data);
				if (todo.size() > 0) {
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doMailFrom(fp.getFrom()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case RCPT:
				lastMacros.clear();
				final RecipientToPacket tp = new RecipientToPacket(data);
				if (todo.size() > 0) {
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doRecipientTo(tp.getRecipient()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case HEADER:
				lastMacros.clear();
				HeaderPacket hp = new HeaderPacket(data);
				headers.add(new Header(hp.getName(), hp.getValue()));
				if (todo.size() > 0) {
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doHeader(headers));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case EOH:
				lastMacros.clear();
				if (todo.size() > 0) {
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doEndOfHeader(headers, allMacros));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
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
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doBody(bp.getChunk()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case BODYEOB:
				lastMacros.clear();
				if (todo.size() > 0) {
					if (!(assembleMessage4.isEmpty() 
						|| Collections.disjoint(todo, assembleMessage4))) 
					{
						// TODO construct message
					}
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.addAll(f.doEndOfMail(headers, allMacros, null));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case UNKNOWN:
				lastMacros.clear();
				if (todo.size() > 0) {
					final UnknownCmdPacket up = new UnknownCmdPacket(data);
					r = new MilterTask(packageType, todo, skey) {
						@Override
						void callMilter(MailFilter f, List<Packet> res) {
							res.add(f.doBadCommand(up.getCmd()));
						}
					};
				} else {
					send(skey, MailFilter.CONTINUE);
				}
				break;
			case OPTNEG:
				final NegotiationPacket rp = new NegotiationPacket(data);
				negotiate(rp);
				send(skey, rp);
				break;
			case QUIT:
			case QUIT_NC:
				cleanup(false, skey);
				break;
			case ABORT:
				cleanup(true, skey);
				break;
			case DATA:
				lastMacros.clear();
				/* actually the milter will send macros only, but no data */
				log.warn("Ooops - unexpected DATA packet received - ignored");
				send(skey, MailFilter.CONTINUE);
				break;
			default:
				lastMacros.clear();
				send(skey, MailFilter.CONTINUE);
				log.warn("Unknown comand " + packageType + " not handled");
		}
		if (r != null) {
			threadPool.submit(new MilterFuture<MilterTask>(r, runningTasks));
		}
	}
	
	/**
	 * Read available data from the given channel and start a thread processing
	 * data if neccessary. 
	 * @param key	channel selection key
	 * @return <code>true</code> a complete packet has been read.
	 * @throws IOException on I/O error
	 */
	public boolean read(SelectionKey key) throws IOException {
		fillBuffer(key, buf);
		if (buf.hasRemaining()) {
			return false;
		}
		buf.flip();
		if (buf == header) {
			int len = header.getInt();
			try {
				packageType = Type.get(header.get());
			} catch (Exception e) {
				log.warn(e.getLocalizedMessage());
				key.channel().close();
				packageType = null;
			}
			header.clear();
			log.debug("Receiving {} with 1 + {} bytes of data", packageType, len-1);
			data = len < 2 ? NULL_BUFFER : ByteBuffer.allocate(len-1);
			buf = data;
			// try to fill data ASAP
			fillBuffer(key, buf);
			if (data.hasRemaining()) {
				return false;
			}
			data.flip();
			log.debug("{}: {} bytes of data received", packageType, data.limit());
		} else {
			log.debug("{}: {} bytes of data received", packageType, data.limit());
		}
		buf = header;
		if (packageType != null) {
			handlePaket(key);
		}
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
}
