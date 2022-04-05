package org.riekr.jloga.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.ProgressListener;

public class AsyncOperations {

	private static final HashMap<Path, ExecutorService> _IO_EXECUTORS     = new HashMap<>();
	private static final ScheduledExecutorService       _MONITOR_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	private static final ExecutorService                _AUX_EXECUTOR     = Executors.newCachedThreadPool();

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
		Path root = path == null ? null : path.getRoot();
		ExecutorService executorService = _IO_EXECUTORS.computeIfAbsent(root, (k) -> Executors.newSingleThreadExecutor());
		return executorService.submit(task);
	}

	public static Future<?> asyncTask(Runnable task) {
		return _AUX_EXECUTOR.submit(task);
	}

	public static <T> Future<T> asyncTask(Callable<T> task) {
		return _AUX_EXECUTOR.submit(task);
	}

}
