package org.riekr.jloga.utils;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.riekr.jloga.utils.FileUtils.toRealAbsolutePath;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
			group = Thread.currentThread().getThreadGroup();
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

	public static class ByFile {

		private class ExecutorData {
			final ExecutorService        executorService;
			final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

			ExecutorData(String key) {
				executorService = new ThreadPoolExecutor(0, 2,
						60L, TimeUnit.SECONDS,
						new SynchronousQueue<>(),
						new NamedThreadFactory(key, _priority));
			}
		}

		final @NotNull String                        _name;
		final          int                           _priority;
		final          HashMap<String, ExecutorData> _executors = new HashMap<>();

		public ByFile(@NotNull String name, int priority) {
			_name = name;
			_priority = priority;
		}

		protected String getKey(Path path) {
			return path == null ? _name : _name + '-' + toRealAbsolutePath(path);
		}

		public Future<?> submit(@NotNull Runnable task) {
			return submit((Path)null, task, false);
		}

		public Future<?> submit(@Nullable File file, Runnable task, boolean block) {
			return submit(file == null ? null : file.toPath(), task, block);
		}

		public synchronized Future<?> submit(@Nullable Path path, Runnable task, boolean block) {
			ExecutorData ed = _executors.computeIfAbsent(getKey(path), ExecutorData::new);
			Lock lock = block ? ed.lock.writeLock() : ed.lock.readLock();
			System.out.println(block);
			return ed.executorService.submit(() -> {
				lock.lock();
				try {
					task.run();
				} finally {
					lock.unlock();
				}
			});
		}

		public void close(File file, boolean now) {
			if (file != null)
				close(file.toPath(), now);
		}

		public synchronized void close(Path path, boolean now) {
			if (path != null) {
				ExecutorData ed = _executors.remove(getKey(path));
				if (ed != null) {
					if (now)
						ed.executorService.shutdownNow();
					else
						ed.executorService.shutdown();
				}
			}
		}
	}

	public static class ByRoot extends ByFile {
		public ByRoot(@NotNull String name, int priority) {
			super(name, priority);
		}

		@Override
		protected String getKey(Path path) {
			return path == null ? _name : _name + '-' + toRealAbsolutePath(path).getRoot();
		}
	}

	public static final ByFile IO   = new ByFile("io", Thread.NORM_PRIORITY);
	public static final ByRoot SAVE = new ByRoot("save", Thread.MIN_PRIORITY);

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
