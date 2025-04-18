package org.riekr.jloga.transform;

import static java.lang.Math.round;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import org.riekr.jloga.misc.AutoDetect;

public class ArrowConversion {

	private static final int _CHUNK_SIZE = 1024 * 1024 * 4; // 4Mb

	private final ZoneId _utcZoneId = ZoneId.of("UTC");

	private final LinkedList<DateTimeFormatter> _dateTimeFormatters = new LinkedList<>();

	{
		ZoneId _localZoneId = ZoneId.systemDefault();
		_dateTimeFormatters.add(ISO_DATE_TIME.withZone(_localZoneId));
		_dateTimeFormatters.add(ISO_LOCAL_DATE_TIME.withZone(_localZoneId));
		_dateTimeFormatters.add(RFC_1123_DATE_TIME.withZone(_localZoneId));
		_dateTimeFormatters.add(ISO_ZONED_DATE_TIME.withZone(_localZoneId));
		// "3/9/2022 12:30:00 PM"
		_dateTimeFormatters.add(new DateTimeFormatterBuilder()
				.appendPattern("M/d/u h:m:s a")
				.toFormatter().withZone(_localZoneId));
		final LocalDate now = LocalDate.now();
		_dateTimeFormatters.add(new DateTimeFormatterBuilder()
				.appendPattern("HH:mm:ss[.SSS]")
				.parseDefaulting(ChronoField.EPOCH_DAY, now.toEpochDay())
				.toFormatter().withZone(_localZoneId));
		AutoDetect.getDateTimeFormatters(_dateTimeFormatters);
	}

	private final String[]            _header;
	private final StringBuilder       _buffer       = new StringBuilder(256);
	private final ArrayList<String[]> _recordBuffer = new ArrayList<>();
	private final Transformer[]       _colTransformers;
	private       int                 _recSize      = 10000;

	public ArrowConversion(String[] header) {
		_header = Transformer.UNWRAP_QUOTES.apply(header);
		_colTransformers = new Transformer[header.length];
		Arrays.fill(_colTransformers, (Transformer)this::fixAndDetectColumn);
	}

	private String fixAndDetectColumn(int col, String val) {
		String unwrapped = Transformer.UNWRAP_QUOTES.apply(col, val);
		Transformer def;
		if (unwrapped.equals(val))
			def = Transformer.IDENTITY;
		else
			def = Transformer.UNWRAP_QUOTES;
		String fixedDate = fixDate(col, unwrapped);
		if (fixedDate == null) {
			_colTransformers[col] = def;
			return unwrapped;
		}
		_colTransformers[col] = def == Transformer.IDENTITY ? this::fixDate : def.andThen(this::fixDate);
		return fixedDate;
	}

	/**
	 * Finos perspective works in UTC only
	 * <a href="https://github.com/finos/perspective/issues/1700">https://github.com/finos/perspective/issues/1700</a>
	 */
	protected String fixDate(int col, String dateString) {
		for (int i = 0, dateTimeFormattersSize = _dateTimeFormatters.size(); i < dateTimeFormattersSize; i++) {
			try {
				final DateTimeFormatter formatter = _dateTimeFormatters.getFirst();
				ZonedDateTime localDateTime = formatter.parse(dateString, ZonedDateTime::from);
				return ISO_LOCAL_DATE_TIME.withZone(_utcZoneId).format(localDateTime.toOffsetDateTime());
			} catch (DateTimeParseException e) {
				onDateTimeParseException(e);
			}
		}
		return null;
	}

	protected void onDateTimeParseException(DateTimeParseException e) {
		// rotate formatters supposing all date/time columns in the stream have the same format
		_dateTimeFormatters.add(_dateTimeFormatters.removeFirst());
	}

	protected void escapeQuotes(String val) {
		int pos;
		int start = 0;
		do {
			pos = val.indexOf('"', start);
			if (pos != -1) {
				_buffer.ensureCapacity(pos - start + 2);
				_buffer.append(val, start, pos);
				_buffer.append("\\\"");
				start = pos + 1;
			} else
				break;
		} while (true);
		_buffer.append(val, start, val.length());
	}

	public final String toArrowChunk(Iterator<String[]> data) {
		_buffer.setLength(0);
		_buffer.append('{');
		_recordBuffer.clear();
		while (data.hasNext() && _recordBuffer.size() < _recSize)
			_recordBuffer.add(data.next());
		for (int i = 0, hlen = _header.length; i < hlen; i++) {
			if (i != 0)
				_buffer.append(',');
			final String colName = _header[i];
			_buffer.append('"');
			escapeQuotes(colName);
			_buffer.append("\":[");
			for (int j = 0, rblen = _recordBuffer.size(); j < rblen; j++) {
				if (j != 0)
					_buffer.append(',');
				String[] rec = _recordBuffer.get(j);
				String val = i < rec.length ? rec[i] : "";
				_buffer.append('"');
				escapeQuotes(_colTransformers[i].apply(i, val));
				_buffer.append('"');
			}
			_buffer.append(']');
		}
		String res = _buffer.append('}').toString();
		checkSize(res.length());
		return res;
	}

	private void checkSize(int len) {
		int prec = _recSize;
		_recSize *= round(_CHUNK_SIZE / (float)len);
		System.err.println("records " + prec + " -> " + _recSize);
	}

	@Override
	public String toString() {
		return _buffer.toString();
	}
}
