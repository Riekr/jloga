package org.riekr.jloga.io;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VolatileTextSource implements TextSource {

	private final List<String> _data;

	public VolatileTextSource(String... data) {
		this(Arrays.asList(data));
	}

	public VolatileTextSource(List<String> data) {
		_data = data;
	}

	@Override
	public String getText(int line) {
		if (line < 0 || line >= _data.size())
			return "";
		return _data.get(line);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		return _data.size();
	}
}
