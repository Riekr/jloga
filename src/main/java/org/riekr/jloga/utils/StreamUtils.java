package org.riekr.jloga.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class StreamUtils {

	public static <T> Stream<T> arrayAsStreamOfLength(T[] data, int length, T filler) {
		if (data.length >= length)
			return Arrays.stream(data);
		return Stream.concat(
				Arrays.stream(data),
				Collections.nCopies(length - data.length, filler).stream()
		);
	}

	private StreamUtils() {}
}
