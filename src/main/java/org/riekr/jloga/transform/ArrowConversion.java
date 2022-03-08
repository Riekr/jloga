package org.riekr.jloga.transform;

import java.util.ArrayList;
import java.util.Iterator;

public class ArrowConversion {

	private final StringBuilder       _buffer  = new StringBuilder(256);
	private final ArrayList<String[]> _records = new ArrayList<>();

	private String escapeQuotes(String val) {
		return val.replace("\"", "\\\"");
	}

	public final String toArrowChunk(String[] header, Iterator<String[]> data) {
		_buffer.setLength(0);
		_buffer.append('{');
		_records.clear();
		while (data.hasNext() && _records.size() < 10000)
			_records.add(data.next());
		for (int i = 0; i < header.length; i++) {
			if (i != 0)
				_buffer.append(',');
			final String col = header[i];
			_buffer.append('"').append(escapeQuotes(col)).append("\":[");
			for (int j = 0; j < _records.size(); j++) {
				if (j != 0)
					_buffer.append(',');
				_buffer.append('"').append(escapeQuotes(_records.get(j)[i])).append('"');
			}
			_buffer.append(']');
		}
		return _buffer.append('}').toString();
	}
}
