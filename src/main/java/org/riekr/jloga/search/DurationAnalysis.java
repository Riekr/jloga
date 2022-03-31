package org.riekr.jloga.search;

import static java.lang.Math.max;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

public class DurationAnalysis implements SearchPredicate {

	private class Matchers {
		final Matcher _matDateExtract;
		final Matcher _matFunc;
		final Matcher _matStart;
		final Matcher _matEnd;
		final Matcher _matRestart;

		Matchers() {
			_matDateExtract = _patDateExtract.matcher("");
			_matFunc = _patFunc.matcher("");
			_matStart = _patStart.matcher("");
			_matEnd = _patEnd.matcher("");
			_matRestart = _patRestart == null ? null : _patRestart.matcher("");
		}

		void match(String text, BiConsumer<Instant, String> onStart, BiConsumer<Instant, String> onEnd, Consumer<Instant> onRestart) {
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
			if (_matRestart != null) {
				_matRestart.reset(text);
				if (_matRestart.find()) {
					onRestart.accept(instant);
					return;
				}
			}
			_matFunc.reset(text);
			if (!_matFunc.find())
				return;
			String func = _matFunc.group(1);
			if (func == null || func.isBlank())
				return;
			_matStart.reset(text);
			if (_matStart.find()) {
				onStart.accept(instant, func);
				return;
			}
			_matEnd.reset(text);
			if (_matEnd.find())
				onEnd.accept(instant, func);
		}
	}

	//20 Jun 2021 00:16:49,601 [SYSTEM@ALRA#getStrumentiList] INFO VAM - MSG_VTR_GETSTRUMENTILIST_START: Start getStrumentiListALRA completed.[SAMA/4300_15m] 7 ms.unt:  Date From: null Date To: null Instrument Type: TP_NON_SPECIFICATO Position Status: OPENQTA Fetch prices: false Calc DayRpl: false Need Detail: true Currency code:
	//20 Jun 2021 00:16:49,624 [SYSTEM@ALRA#getStrumentiList] INFO VAM - MSG_VTR_GETSTRUMENTILIST_END: Stop getStrumentiLististALRA completed.[SAMA/4300_15m] 7 ms.unt:  Date From: null Date To: null Instrument Type: TP_NON_SPECIFICATO Position Status: OPENQTA Fetch prices: false Calc DayRpl: false Need Detail: true Currency code:

	private final Pattern _patDateExtract;    // "^(\d+ \w+ \d+ \d+:\d+:\d+,\d+) \["
	private final DateTimeFormatter _patDate; // "dd MMM YYYY HH:mm:ss,SSS"
	private final Pattern _patFunc;           // " \[([^@]+@[^#]+#[^]]+)\] "
	private final Pattern _patStart;          // "] INFO VAM - .+: Start "
	private final Pattern _patEnd;            // "] INFO VAM - .+: Stop "
	private final Pattern _patRestart;
	private final Duration _minDuration;

	private Matchers _matchers;
	private Map<String, Instant> _funcStarts;
	private int _maxFuncLength;
	private int _maxDurationLength;

	private ChildTextSource _dest;
	private final Map<Integer, Duration> _durations = new HashMap<>();

	public DurationAnalysis(Pattern patDateExtract, DateTimeFormatter patDate, Pattern patFunc, Pattern patStart, Pattern patEnd, Pattern patRestart, Duration minDuration) {
		_patDateExtract = patDateExtract;
		_patDate = patDate;
		_patFunc = patFunc;
		_patStart = patStart;
		_patEnd = patEnd;
		_patRestart = patRestart;
		_minDuration = minDuration;
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		// matchers
		_matchers = new Matchers();
		// transient data
		_funcStarts = new HashMap<>();
		_maxFuncLength = 4; // "null"
		_maxDurationLength = 7; // "PT0.00S"
		// destination data
		_dest = new ChildTextSource(master) {
			final Matchers matchers = new Matchers();
			final StringBuilder buf = new StringBuilder();

			@Override
			public synchronized String getText(int line)  {
				String origText = super.getText(line);
				matchers.match(origText,
						(instant, func) -> {
							buf.setLength(0);
							buf.append("ERR#START");
						},
						(instant, func) -> {
							buf.setLength(0);
							buf.append(func);
							buf.append(" ".repeat(_maxFuncLength - func.length()));
							buf.append(" | ");
							String dur = String.valueOf(_durations.get(getSrcLine(line)));
							buf.append(dur);
							buf.append(" ".repeat(_maxDurationLength - dur.length()));
							buf.append(" | ").append(origText);
						},
						(instant) -> {
							buf.setLength(0);
							buf.append(" ".repeat(_maxFuncLength));
							buf.append(" | RESTART");
							buf.append(" ".repeat(_maxDurationLength - 7));
							buf.append(" | ").append(origText);
						}
				);
				return buf.toString();
			}
		};
		_durations.clear();
		return _dest;
	}

	public void end(boolean interrupted) {
		_matchers = null;
		_funcStarts = null;
	}

	@Override
	public void verify(int line, String text) {
		_matchers.match(text,
				(start, func) -> _funcStarts.put(func, start),
				(end, func) -> {
					Instant start = _funcStarts.remove(func);
					if (start != null) {
						Duration dur = Duration.between(start, end);
						if (_minDuration == null || dur.compareTo(_minDuration) >= 0) {
							_durations.put(line, dur);
							_dest.addLine(line);
							_maxFuncLength = max(_maxFuncLength, func.length());
							_maxDurationLength = max(_maxDurationLength, dur.toString().length());
						}
					}
				},
				(instant) -> {
					_dest.addLine(line);
					_funcStarts.clear();
				}
		);
	}

}
