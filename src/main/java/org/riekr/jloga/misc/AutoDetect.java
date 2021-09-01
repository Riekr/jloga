package org.riekr.jloga.misc;

import static java.util.regex.Pattern.compile;

import javax.swing.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.ui.MRUComboWithLabels;
import org.riekr.jloga.ui.UIUtils;

@SuppressWarnings({"RegExpRedundantEscape", "RegExpUnnecessaryNonCapturingGroup"})
public final class AutoDetect implements Predicate<String> {

	public interface Wizard {
		void onWizard();
	}

	public static final  String                    GLYPH         = "\uD83D\uDD0D";
	private static final int                       _HEAD_LIMIT   = 100;
	private static final LinkedHashSet<AutoDetect> _AUTO_DETECTS = new LinkedHashSet<>();

	static {
		// 2021/08/30 | 16:46:23.058 | [localhost-startStop...
		_AUTO_DETECTS.add(new AutoDetect(compile("^(\\d\\d\\d\\d/\\d\\d/\\d\\d \\| \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d) \\| \\["), "uuuu/MM/dd | HH:mm:ss.SSS"));

		// 30 Aug 2021 16:46:14,186 [Thread-1] INFO ...
		_AUTO_DETECTS.add(new AutoDetect(compile("^(\\d+ \\w+ \\d+ \\d+:\\d+:\\d+,\\d+) \\["), "dd MMM uuuu HH\\:mm\\:ss,SSS"));

		// <rec><d>2021/08/30 16:46:24:099</d><u>...
		_AUTO_DETECTS.add(new AutoDetect(compile("^<rec><d>(\\d\\d\\d\\d/\\d\\d/\\d\\d \\d\\d:\\d\\d:\\d\\d:\\d\\d\\d)</d>"), "uuuu/MM/dd HH\\:mm\\:ss\\:SSS"));

		// 2021-08-30 17:23:13,787 INFO  [http-nio-...
		_AUTO_DETECTS.add(new AutoDetect(compile("^(\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d,\\d\\d\\d) \\w+\\s+\\["), "uuuu-MM-dd HH\\:mm\\:ss,SSS"));

		// 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
		_AUTO_DETECTS.add(new AutoDetect(compile("^[\\d\\.]+ - [\\w\\d\\.]+ \\[(\\d\\d/\\w\\w\\w/\\d\\d\\d\\d:\\d\\d:\\d\\d:\\d\\d) (?:-)?\\d+\\] \""), "dd/MMM/uuuu\\:HH\\:mm\\:ss"));
	}

	@NotNull
	public static JButton newButtonFor(TextSource textSource, MRUComboWithLabels<Pattern> patternCombo, MRUComboWithLabels<DateTimeFormatter> formatterCombo) {
		return UIUtils.newButton(GLYPH, () -> {
			try {
				for (int i = 0; i < Math.min(_HEAD_LIMIT, textSource.getLineCount()); i++) {
					AutoDetect res = from(textSource.getText(i));
					if (res != null) {
						patternCombo.combo.setValue(res.pattern.pattern());
						formatterCombo.combo.setValue(res.formatterString);
						return;
					}
				}
				patternCombo.combo.setValue(null);
				formatterCombo.combo.setValue(null);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		});
	}

	@Nullable
	public static AutoDetect from(String line) {
		for (AutoDetect candidate : _AUTO_DETECTS) {
			if (candidate.test(line))
				return candidate;
		}
		return null;
	}


	public final @NotNull Pattern           pattern;
	public final @NotNull DateTimeFormatter formatter;
	public final @NotNull String            formatterString;

	private AutoDetect(@NotNull Pattern pattern, @NotNull String dateFormat) {
		this.pattern = pattern;
		this.formatter = Formatters.newDefaultDateTimeFormatter(dateFormat);
		this.formatterString = dateFormat;
	}


	@Override
	public boolean test(String s) {
		if (s != null && !s.isBlank()) {
			Matcher matcher = pattern.matcher(s);
			if (matcher.find()) {
				String date = matcher.group(matcher.groupCount());
				try {
					formatter.parse(date);
					return true;
				} catch (DateTimeParseException ignored) {}
			}
		}
		return false;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final AutoDetect that = (AutoDetect)o;
		if (!pattern.equals(that.pattern))
			return false;
		return formatter.equals(that.formatter);
	}

	@Override public int hashCode() {
		int result = pattern.hashCode();
		result = 31 * result + formatter.hashCode();
		return result;
	}
}
