package org.riekr.jloga.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class CharsetUtils {

	public static final List<Charset> PREFERRED_CHARSETS;

	static {
		List<Charset> preferredCharsets = new ArrayList<>();
		preferredCharsets.add(StandardCharsets.UTF_8);
		preferredCharsets.add(StandardCharsets.UTF_16);
		preferredCharsets.add(StandardCharsets.UTF_16BE);
		preferredCharsets.add(StandardCharsets.UTF_16LE);
		preferredCharsets.add(StandardCharsets.ISO_8859_1);
		preferredCharsets.add(StandardCharsets.US_ASCII);
		try {
			preferredCharsets.add(Charset.forName("IBM037"));
		} catch (Exception ignored) {}
		PREFERRED_CHARSETS = Collections.unmodifiableList(preferredCharsets);
	}

	public static boolean isPrintable(char ch) {
		return ch >= 33 && ch < 126;
	}

	public static Charset guessCharset(ByteBuffer byteBuffer) {
		byteBuffer.mark();
		TreeMap<Integer, Set<Charset>> guesses = new TreeMap<>();
		for (final Charset charsetCandidate : Charset.availableCharsets().values()) {
			try {
				byteBuffer.reset();
				CharBuffer charBuffer = charsetCandidate.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT)
						.decode(byteBuffer);
				int confidence = 0;
				while (charBuffer.hasRemaining()) {
					final char ch = charBuffer.get();
					if (isPrintable(ch))
						confidence++;
				}
				if (confidence > 0) {
					// System.out.println("CHARDET:" + charsetCandidate + " " + confidence + " -> " + charBuffer);
					guesses.compute(confidence, (k, prev) -> {
						if (prev == null)
							prev = new LinkedHashSet<>(1);
						prev.add(charsetCandidate);
						return prev;
					});
				}
			} catch (CharacterCodingException e) {
				// System.out.println("CHARDET:" + charsetCandidate + " -> " + e.getMessage());
			}
		}
		if (guesses.isEmpty())
			return null;
		// guesses.forEach((conf, cs) -> System.out.println("CONF: " + conf + " -> " + cs));
		final Set<Charset> charsets = guesses.lastEntry().getValue();
		for (final Charset preferredCharset : PREFERRED_CHARSETS) {
			if (charsets.contains(preferredCharset))
				return preferredCharset;
		}
		return charsets.stream().findFirst().orElse(null);
	}


	private CharsetUtils() {}
}
