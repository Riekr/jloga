package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.misc.InstantRange;
import org.riekr.jloga.misc.PagedIntBag;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNullElse;

public class MixFileSource implements TextSource {

	public static final class SourceConfig {
		public final @NotNull File file;
		private final Matcher _matcher;
		private final DateTimeFormatter _formatter;
		private final Duration _offset;

		public SourceConfig(@NotNull File file, Pattern dateExtract, DateTimeFormatter formatter, Duration offset) {
			this.file = file;
			_matcher = dateExtract.matcher("");
			_formatter = formatter;
			_offset = offset == null ? Duration.ZERO : offset;
		}

		Instant getInstant(String line) {
			_matcher.reset(line);
			if (_matcher.find()) {
				Instant instant = _formatter.parse(_matcher.group(1), LocalDateTime::from).toInstant(ZoneOffset.UTC);
				_offset.addTo(instant);
				return instant;
			}
			return null;
		}
	}

	public static final class Config {
		public final @NotNull Map<TextSource, SourceConfig> sources;
		public final @Nullable Instant from, to;

		public Config(@NotNull Map<TextSource, SourceConfig> sources, @Nullable Instant from, @Nullable Instant to) {
			this.sources = sources;
			this.from = from;
			this.to = to;
		}
	}

	static final class ScanData implements Comparable<ScanData> {
		final int idx;
		final TextSource src;
		final SourceConfig _sourceConfig;
		final int len;
		int pos;
		Instant instant;
		int pass = 0;

		ScanData(int idx, TextSource src, SourceConfig sourceConfig, int len) {
			this.idx = idx;
			this.src = src;
			this._sourceConfig = sourceConfig;
			this.len = len;
		}

		boolean hasData() {
			return pos < len;
		}

		@Override
		public int compareTo(@NotNull MixFileSource.ScanData o) {
			return requireNonNullElse(instant, hasData() ? Instant.MIN : Instant.MAX)
					.compareTo(requireNonNullElse(o.instant, o.hasData() ? Instant.MIN : Instant.MAX));
		}
	}

	private final TextSource[] _sources;
	private final Future<?> _indexing;
	private final PagedIntBag _index = new PagedIntBag(2);
	private final int _padding;
	private @Nullable ProgressListener _indexingListener;

	public MixFileSource(@NotNull Config config) {
		if (config.sources.size() < 2)
			throw new IllegalArgumentException("Not enough mix sources");
		_sources = new TextSource[config.sources.size()];
		_padding = (int) (Math.log10(_sources.length)) + 1;
		_indexing = EXECUTOR.submit(() -> {
			int totLineCount = 0;
			try {
				int idx = 0;
				ScanData[] data = new ScanData[_sources.length];
				for (Map.Entry<TextSource, SourceConfig> entry : config.sources.entrySet()) {
					final TextSource textSource = entry.getKey();
					int lineCount = textSource.getLineCount();
					totLineCount += lineCount;
					_sources[idx] = textSource;
					data[idx] = new ScanData(idx, textSource, entry.getValue(), lineCount);
					idx++;
				}
				if (_indexingListener != null)
					_indexingListener.onProgressChanged(0, totLineCount);
				final BooleanSupplier hasData = () -> {
					for (ScanData sd : data) {
						if (sd.hasData())
							return true;
					}
					return false;
				};
				int parsedLines = 0;
				final Predicate<Instant> predicate = InstantRange.from(config.from, config.to);
				while (hasData.getAsBoolean()) {
					// fill instants, I may do it only for the 1st entry in "data"
					// but I'm lazy to initialize it before the enclosing while
					for (ScanData sd : data) {
						// collect all lines until we have an instant
						while (sd.instant == null && sd.hasData()) {
							String line = sd.src.getText(sd.pos);
							sd.instant = sd._sourceConfig.getInstant(line);
							if (sd.instant == null) {
								sd.pos++;
								parsedLines++;
							}
						}
					}
					// sort by instant
					Arrays.sort(data);
					ScanData sd = data[0];
					if (sd.hasData()) {
						// fill index
						if (predicate.test(sd.instant)) {
							if (sd.pass == 0 && sd.pos > 0) {
								// recovery of lines without date at the beginning of file
								for (int l = 0; l < sd.pos; l++)
									_index.add(sd.idx, l);
							}
							_index.add(sd.idx, sd.pos);
						}
						sd.pos++;
						parsedLines++;
						// collect adjacent lines without instant
						if (sd.hasData()) {
							do {
								String line = sd.src.getText(sd.pos);
								sd.instant = sd._sourceConfig.getInstant(line);
								if (sd.instant == null) {
									if (predicate.test(null))
										_index.add(sd.idx, sd.pos);
									sd.pos++;
									parsedLines++;
								} else
									break;
							} while (sd.hasData());
						} else {
							// mark for seek
							sd.instant = null;
						}
						sd.pass++;
					}
					if (_indexingListener != null)
						_indexingListener.onProgressChanged(parsedLines, totLineCount);
				}
				_index.seal();
				System.out.println("Mixed " + _index.size() + " lines of " + totLineCount + " using " + _index.pages() + " pages");
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			} finally {
				if (_indexingListener != null) {
					_indexingListener.onProgressChanged(totLineCount, totLineCount);
					_indexingListener = null;
				}
			}
		});
	}

	@Override
	public String getText(int line) throws ExecutionException, InterruptedException {
		if (line >= _index.size())
			return "";
		int[] data = _index.get(line);
		int srcId = data[0];
		int srcLine = data[1];
		String text = _sources[srcId].getText(srcLine);
		return " ".repeat(Math.max(0, _padding - ((int) Math.log10(srcId) + 1))) +
				srcId +
				" | " +
				text;
	}

	@Override
	public void setIndexingListener(@NotNull ProgressListener indexingListener) {
		_indexingListener = indexingListener;
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_indexing.get();
		return (int) _index.size() + 1;
	}

	@Override
	public boolean isIndexing() {
		return !_indexing.isDone();
	}
}
