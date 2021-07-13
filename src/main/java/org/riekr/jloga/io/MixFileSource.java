package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.misc.PagedIntBag;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNullElse;

public class MixFileSource implements TextSource {

	public static final class Config {
		private final Matcher _matcher;
		private final DateTimeFormatter _formatter;
		private final Duration _offset;

		public Config(Pattern dateExtract, DateTimeFormatter formatter, Duration offset) {
			_matcher = dateExtract.matcher("");
			_formatter = formatter;
			_offset = offset == null ? Duration.ZERO : offset;
		}

		Instant getInstant(String line) {
			_matcher.reset(line);
			if (_matcher.find()) {
				Instant instant = _formatter.parse(_matcher.group(1), Instant::from);
				_offset.addTo(instant);
				return instant;
			}
			return null;
		}
	}

	static final class ScanData implements Comparable<ScanData> {
		final int idx;
		final TextSource src;
		final Config config;
		final int len;
		int pos;
		Instant instant;

		ScanData(int idx, TextSource src, Config config, int len) {
			this.idx = idx;
			this.src = src;
			this.config = config;
			this.len = len;
		}

		boolean hasNext() {
			return pos < len;
		}

		@Override
		public int compareTo(@NotNull MixFileSource.ScanData o) {
			return requireNonNullElse(instant, Instant.EPOCH)
					.compareTo(requireNonNullElse(o.instant, Instant.EPOCH));
		}
	}

	private final TextSource[] _sources;
	private final Future<?> _indexing;
	private int _lineCount;
	private final PagedIntBag _index = new PagedIntBag(2);
	private final int _padding;

	public MixFileSource(Map<TextSource, Config> sources) {
		if (sources == null || sources.size() < 2)
			throw new IllegalArgumentException("Not enough mix sources");
		_sources = new TextSource[sources.size()];
		_padding = (int) (Math.log10(_sources.length)) + 1;
		_indexing = EXECUTOR.submit(() -> {
			try {
				int idx = 0;
				ScanData[] data = new ScanData[_sources.length];
				for (Map.Entry<TextSource, Config> entry : sources.entrySet()) {
					final TextSource textSource = entry.getKey();
					int lineCount = textSource.getLineCount();
					_lineCount += lineCount;
					_sources[idx] = textSource;
					data[idx] = new ScanData(idx, textSource, entry.getValue(), lineCount);
					idx++;
				}
				BooleanSupplier hasNext = () -> {
					for (ScanData sd : data) {
						if (sd.hasNext())
							return true;
					}
					return false;
				};
				while (hasNext.getAsBoolean()) {
					// TODO: correctly handle lines without date
					// fill instants
					for (ScanData sd : data) {
						if (sd.hasNext()) {
							String line = sd.src.getText(sd.pos);
							sd.instant = sd.config.getInstant(line);
						}
					}
					// sort by instant
					Arrays.sort(data);
					// fill index
					for (ScanData sd : data) {
						_index.add(sd.idx, sd.pos);
						if (sd.hasNext())
							sd.pos++;
					}
				}
				System.out.println("Mixed " + _index.size() + " lines of " + _lineCount + " using " + _index.pages() + " pages");
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace(System.err);
			}
		});
	}

	@Override
	public String getText(int line) throws ExecutionException, InterruptedException {
		int[] data = _index.get(line);
		int srcId = data[0];
		String text = _sources[srcId].getText(data[1]);
		return " ".repeat(Math.max(0, _padding - ((int) Math.log10(srcId) + 1))) +
				srcId +
				" | " +
				text;
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_indexing.get();
		return _lineCount;
	}

}
