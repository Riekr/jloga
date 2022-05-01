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

		NamedThreadFactory(@NotNull String name) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() :
					Thread.currentThread().getThreadGroup();
			namePrefix = name + "-thread-";
		}

		public Thread newThread(@NotNull Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

	private static final HashMap<String, ExecutorService> _IO_EXECUTORS     = new HashMap<>();
	private static final ScheduledExecutorService         _MONITOR_EXECUTOR = newSingleThreadScheduledExecutor(new NamedThreadFactory("monitor"));
	private static final ExecutorService                  _AUX_EXECUTOR     = newCachedThreadPool(new NamedThreadFactory("aux"));

	public static ScheduledFuture<?> monitorProgress(LongSupplier current, long total, ProgressListener progressListener) {
		return monitorProgress(current, () -> total, progressListener);
	}

	public static ScheduledFuture<?> monitorProgress(LongSupplier current, LongSupplier total, ProgressListener progressListener) {
		return _MONITOR_EXECUTOR.scheduleWithFixedDelay(
				() -> progressListener.onIntermediate(current.getAsLong(), total.getAsLong()),
				200, 200, TimeUnit.MILLISECONDS);
	}

	public static Future<?> asyncIO(@NotNull Runnable task) {
		return asyncIO((Path)null, task);
	}

	public static Future<?> asyncIO(@Nullable File file, Runnable task) {
		return asyncIO(file == null ? null : file.toPath(), task);
	}

	public static Future<?> asyncIO(@Nullable Path path, Runnable task) {
		String name = path == null ? "io" : "io-" + path.getRoot();
		ExecutorService executorService = _IO_EXECUTORS.computeIfAbsent(name, (k) -> newSingleThreadExecutor(new NamedThreadFactory(k)));
		return executorService.submit(task);
	}

	public static Future<?> asyncTask(Runnable task) {
		return _AUX_EXECUTOR.submit(task);
	}

	public static <T> Future<T> asyncTask(Callable<T> task) {
		return _AUX_EXECUTOR.submit(task);
	}

}
