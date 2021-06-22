package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ProgressListener {

	static ProgressListener limit(long ms, @NotNull ProgressListener listener) {
		return new ProgressListener() {
			private long _next = 0L;

			@Override
			public void onProgressChanged(long pos, long size) {
				long now = System.currentTimeMillis();
				if (_next < now || pos == size) {
					_next = now + ms;
					listener.onProgressChanged(pos, size);
				}
			}
		};
	}

	default ProgressListener atFirst(@NotNull Runnable task) {
		AtomicBoolean first = new AtomicBoolean(false);
		return (pos, size) -> {
			if (first.compareAndSet(false, true))
				task.run();
			onProgressChanged(pos, size);
		};
	}

	ProgressListener NOP = (pos, size) -> {
	};

	void onProgressChanged(long pos, long size);

	default ProgressListener andThen(ProgressListener after) {
		Objects.requireNonNull(after);
		return (l, r) -> {
			onProgressChanged(l, r);
			after.onProgressChanged(l, r);
		};
	}

	default ProgressListener andThen(Runnable afterAll) {
		Objects.requireNonNull(afterAll);
		return (l, r) -> {
			onProgressChanged(l, r);
			if (l == r)
				afterAll.run();
		};
	}
}
