package org.riekr.jloga.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class StreamUtils {

	public static <T> Stream<T> arrayAsStreamOfLength(int from, int len, T[] data, int count, T filler) {
		if (len >= count)
			return Arrays.stream(data).skip(from).limit(len);
		return Stream.concat(
				Arrays.stream(data).skip(from).limit(len),
				Collections.nCopies(count - len, filler).stream()
		);
	}

	public static <T> Stream<T> arrayAsStreamOfLength(T[] data, int count, T filler) {
		if (data.length >= count)
			return Arrays.stream(data);
		return Stream.concat(
				Arrays.stream(data),
				Collections.nCopies(count - data.length, filler).stream()
		);
	}

	private StreamUtils() {}
}
