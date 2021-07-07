package org.riekr.jloga.search;

import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;

public class FrequencyAnalysis implements SearchPredicate {

	private static class Count {
		int startLine;
		Instant startInstant;
		int count = 1;

		Count(int startLine, Instant startInstant) {
			this.startLine = startLine;
			this.startInstant = startInstant;
		}

		void reset(int line, Instant instant) {
			startLine = line;
			startInstant = instant;
			count = 1;
		}
	}

	private class Matchers {
		final Matcher _matDateExtract;
		final Matcher _matFunc;

		Matchers() {
			_matDateExtract = _patDateExtract.matcher("");
			_matFunc = _patFunc.matcher("");
		}

		void match(String text, BiConsumer<Instant, String> onFreq) {
			_matDateExtract.reset(text);
			if (!_matDateExtract.find())
				return;
			String date = _matDateExtract.group(1);
			if (date == null || date.isBlank())
				return;
			Instant instant;
			try {
				instant = _patDate.parse(date, Instant::from);
			} catch (DateTimeParseException e) {
				throw new SearchException("Unable to complete duration analysis", e);
			}
			_matFunc.reset(text);
			if (!_matFunc.find())
				return;
			String func = _matFunc.groupCount() > 0 ? _matFunc.group(1) : "";
			onFreq.accept(instant, func);
		}
	}

	private final Pattern _patDateExtract;    // "^(\d+ \w+ \d+ \d+:\d+:\d+,\d+) \["
	private final DateTimeFormatter _patDate; // "dd MMM YYYY HH:mm:ss,SSS"
	private final Pattern _patFunc;           // " \[([^@]+@[^#]+#[^]]+)\] "
	private final Duration _window;

	private Matchers _matchers;
	private Map<String, Count> _counters;
	private int _maxFuncLength;
	private int _maxFreqLength;

	private ChildTextSource _dest;
	private final Map<Integer, Integer> _results = new HashMap<>();

	public FrequencyAnalysis(Pattern patDateExtract, DateTimeFormatter patDate, Pattern patFunc, Duration measureWindow) {
		_patDateExtract = patDateExtract;
		_patDate = patDate;
		_patFunc = patFunc;
		_window = measureWindow == null ? Duration.of(1, ChronoUnit.MINUTES) : measureWindow;
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		// matchers
		_matchers = new Matchers();
		// transient data
		_counters = new HashMap<>();
		_maxFuncLength = 0; // ""
		_maxFreqLength = 1; // "0"
		// destination data
		_dest = new ChildTextSource(master) {
			final Matchers matchers = new Matchers();
			final StringBuilder buf = new StringBuilder();

			@Override
			public synchronized String getText(int line) throws ExecutionException, InterruptedException {
				String origText = super.getText(line);
				matchers.match(origText,
						(instant, func) -> {
							buf.setLength(0);
							if (_maxFuncLength > 0) {
								buf.append(func);
								buf.append(" ".repeat(_maxFuncLength - func.length()));
								buf.append(" | ");
							}
							String count = String.valueOf(_results.get(getSrcLine(line)));
							buf.append(count);
							buf.append(" ".repeat(_maxFreqLength - count.length()));
							buf.append(" | ").append(origText);
						}
				);
				return buf.toString();
			}
		};
		_results.clear();
		return _dest;
	}

	public void end() {
		_counters.forEach((func, count) -> {
			_dest.addLine(count.startLine);
			_results.put(count.startLine, count.count);
		});
		_matchers = null;
		_counters = null;
	}

	@Override
	public void verify(int line, String text) {
		_matchers.match(text,
				(instant, func) -> {
					_maxFuncLength = max(_maxFuncLength, func.length());
					Count count = _counters.get(func);
					if (count == null) {
						_counters.put(func, new Count(line, instant));
						return;
					}
					if (Duration.between(count.startInstant, instant).compareTo(_window) >= 0) {
						_dest.addLine(count.startLine);
						_results.put(count.startLine, count.count);
						count.reset(line, instant);
					} else {
						count.count++;
						_maxFreqLength = max(_maxFreqLength, ((int) Math.log10(count.count)) + 1);
					}
				}
		);
	}

}
