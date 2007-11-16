/**
 * $Id$ 
 * 
 * Copyright (c) 2005-2007 Jens Elkner.
 * All Rights Reserved.
 *
 * This software is the proprietary information of Jens Elkner.
 * Use is subject to license terms.
 */
package de.ovgu.cs.milter4j.jmx;

import java.util.concurrent.Future;

/**
 * Get stats about the executor.
 * 
 * @author Jens Elkner
 * @version $Revision$
 */
public interface FutureTaskExecutorMXBean {
	/**
	 * Returns the approximate total number of tasks that have ever been
	 * scheduled for execution. Because the states of tasks and threads may
	 * change dynamically during computation, the returned value is only an
	 * approximation.
	 * 
	 * @return the number of tasks
	 */
	public long getTaskCount();

	/**
	 * Returns the approximate total number of tasks that have completed
	 * execution. Because the states of tasks and threads may change dynamically
	 * during computation, the returned value is only an approximation, but one
	 * that does not ever decrease across successive calls.
	 * 
	 * @return the number of tasks
	 */
	public long getCompletedTaskCount();

	/**
	 * Returns the approximate number of threads that are actively executing
	 * tasks.
	 * 
	 * @return the number of threads
	 */
	public int getActiveCount();

	/**
	 * Returns the current number of threads in the pool.
	 * 
	 * @return the number of threads
	 */
	public int getPoolSize();

	/**
	 * Returns the largest number of threads that have ever simultaneously been
	 * in the pool.
	 * 
	 * @return the number of threads
	 */
	public int getLargestPoolSize();

	/**
	 * Returns the core number of threads.
	 * 
	 * @return the core number of threads
	 */
	public int getCorePoolSize();

	/**
	 * Returns the maximum allowed number of threads.
	 * 
	 * @return the maximum allowed number of threads
	 */
	public int getMaximumPoolSize();

	/**
	 * Returns true if this executor is in the process of terminating after
	 * <tt>shutdown</tt> or <tt>shutdownNow</tt> but has not completely
	 * terminated. This method may be useful for debugging. A return of
	 * <tt>true</tt> reported a sufficient period after shutdown may indicate
	 * that submitted tasks have ignored or suppressed interruption, causing
	 * this executor not to properly terminate.
	 * 
	 * @return true if terminating but not yet terminated
	 */
	public boolean isTerminating();

	/**
	 * Check, whether this executir has been terminated.
	 * 
	 * @return <code>true</code> if terminated.
	 */
	public boolean isTerminated();

	/**
	 * Tries to remove from the work queue all {@link Future} tasks that have
	 * been cancelled. This method can be useful as a storage reclamation
	 * operation, that has no other impact on functionality. Cancelled tasks are
	 * never executed, but may accumulate in work queues until worker threads
	 * can actively remove them. Invoking this method instead tries to remove
	 * them now. However, this method may fail to remove tasks in the presence
	 * of interference by other threads.
	 */
	public void purge();

	/**
	 * Returns the thread keep-alive time in seconds, which is the amount of
	 * time that threads in excess of the core pool size may remain idle before
	 * being terminated.
	 * 
	 * @return the time limit
	 */
	public long getKeepAliveTime();
}
