package org.riekr.jloga.utils;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.ProgressListener;

public class AsyncOperations {

	private static class NamedThreadFactory implements ThreadFactory {
		private final ThreadGroup   group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String        namePrefix;
		private final int           priority;

		NamedThreadFactory(@NotNull String name, int prio) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = name + "-thread-";
			priority = prio;
		}

		public Thread newThread(@NotNull Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(true);
			t.setPriority(priority);
			return t;
		}
	}

	public static class ByRoot {
		private final @NotNull String                           _name;
		private final          int                              _priority;
		private final          HashMap<String, ExecutorService> _executors = new HashMap<>();

		public ByRoot(@NotNull String name, int priority) {
			_name = name;
			_priority = priority;
		}

		public Future<?> submit(@NotNull Runnable task) {
			return submit((Path)null, task);
		}

		public Future<?> submit(@Nullable File file, Runnable task) {
			return submit(file == null ? null : file.toPath(), task);
		}

		public Future<?> submit(@Nullable Path path, Runnable task) {
			return _executors.computeIfAbsent(
					path == null ? _name : _name + '-' + path.getRoot(),
					(k) -> newSingleThreadExecutor(new NamedThreadFactory(k, _priority))
			).submit(task);
		}
	}

	public static final ByRoot IO    = new ByRoot("io", Thread.NORM_PRIORITY);
	public static final ByRoot INDEX = new ByRoot("index", Thread.MIN_PRIORITY);

	private static final ScheduledExecutorService _MONITOR_EXECUTOR = newSingleThreadScheduledExecutor(new NamedThreadFactory("monitor", Thread.NORM_PRIORITY));
	private static final ExecutorService          _AUX_EXECUTOR     = newCachedThreadPool(new NamedThreadFactory("aux", Thread.NORM_PRIORITY));

	public static ScheduledFuture<?> monitorProgress(LongSupplier current, long total, ProgressListener progressListener) {
		return monitorProgress(current, () -> total, progressListener);
	}

	public static ScheduledFuture<?> monitorProgress(LongSupplier current, LongSupplier total, ProgressListener progressListener) {
		return _MONITOR_EXECUTOR.scheduleWithFixedDelay(
				() -> progressListener.onIntermediate(current.getAsLong(), total.getAsLong()),
				200, 200, TimeUnit.MILLISECONDS);
	}

	public static Future<?> asyncTask(Runnable task) {
		return _AUX_EXECUTOR.submit(task);
	}

	public static <T> Future<T> asyncTask(Callable<T> task) {
		return _AUX_EXECUTOR.submit(task);
	}

}
