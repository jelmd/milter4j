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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A ThreadPoolExecutor, which will exectue FutureTasks directly,
 * without wrapping them into another FutureTask.
 * 
 * @author 	Jens Elkner
 * @version	$Revision$
 */
public class FutureTaskExecutor
	extends ThreadPoolExecutor
{
	/**
	 * Create the thread.
	 * 
	 * @param corePoolSize		core pool size
	 * @param maximumPoolSize	max. pool size
	 * @param keepAliveTime		keep alive tie
	 * @param unit				time unit
	 * @param workQueue			work queue
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue)
	 */
	public FutureTaskExecutor(int corePoolSize, int maximumPoolSize,
		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	/**
	 * Tries to execute the given task as described in 
	 * {@link #execute(Runnable)}.
	 * 
	 * @param <T>	result type
	 * @param task	task to execute
	 * @return the task itself
	 */
	public <T> FutureTask<T> submit(FutureTask<T> task) {
        execute(task);
        return task;
	}
}
