package org.riekr.jloga.io;

import java.lang.ref.WeakReference;

public final class IndexData {
	final long startPos;
	WeakReference<String[]> data;

	public IndexData(long startPos) {
		this.startPos = startPos;
	}
}
