package org.riekr.jloga.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Charsets {

	private static final List<Charset> charsets = Arrays.asList(
			StandardCharsets.UTF_8,
			StandardCharsets.ISO_8859_1,
			StandardCharsets.UTF_16,
			StandardCharsets.US_ASCII
	);

	private static final int size   = charsets.size();
	private static final int cycles = size - 1;

	public static List<Charset> nextOf(Charset charset) {
		int pos = charsets.indexOf(charset);
		if (pos == -1)
			return charsets;
		if (pos == cycles)
			return charsets.subList(0, pos);
		ArrayList<Charset> res = new ArrayList<>(cycles);
		res.addAll(charsets.subList(pos + 1, size));
		res.addAll(charsets.subList(0, pos));
		return res;
	}

	public static Stream<Charset> stream() {
		return charsets.stream();
	}
}
