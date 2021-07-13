package org.riekr.jloga.io;

import java.util.Objects;

public interface ProgressListener {

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

	default ProgressListener afterAll(Runnable afterAll) {
		Objects.requireNonNull(afterAll);
		return (l, r) -> {
			onProgressChanged(l, r);
			if (l == r)
				afterAll.run();
		};
	}
}
