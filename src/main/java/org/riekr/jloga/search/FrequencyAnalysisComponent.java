package org.riekr.jloga.search;

import static org.riekr.jloga.misc.StdFields.Date;
import static org.riekr.jloga.misc.StdFields.DateExtractor;
import static org.riekr.jloga.misc.StdFields.Func;
import static org.riekr.jloga.misc.StdFields.Period;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;

public class FrequencyAnalysisComponent extends SearchProjectComponentWithExpandablePanel implements AutoDetect.Wizard {
	public static final String ID = "FrequencyAnalysisComponent";

	public final Field<Pattern>           patDateExtract = newPatternField(DateExtractor, "Date extractor pattern:", 1);
	public final Field<DateTimeFormatter> patDate        = newDateTimeFormatterField(Date, "Date pattern:");
	public final Field<Pattern>           patFunc        = newPatternField(Func, "Function pattern:", 0);
	public final Field<Duration>          periodDuration = newDurationField(Period, "Period:", Duration.of(1, ChronoUnit.MINUTES));

	private Supplier<TextSource> _textSource;

	public FrequencyAnalysisComponent(int level) {
		super("FrequencyAnalysisComponent." + level,
				"jloga",
				"Frequency analysis project");
		buildUI();
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getSearchIconLabel() {
		return "f";
	}

	@Override
	public boolean isReady() {
		return patDateExtract.hasValue()
				&& patDate.hasValue()
				&& patFunc.hasValue();
	}

	@Override
	protected SearchPredicate getSearchPredicate() {
		if (isReady()) {
			return new FrequencyAnalysis(
					patDateExtract.get(),
					patDate.get(),
					patFunc.get(),
					periodDuration.get()
			);
		}
		return null;
	}

	@Override
	public void setTextSourceSupplier(Supplier<TextSource> textSource) {
		_textSource = textSource;
	}

	@Override
	public void onWizard() {
		AutoDetect autoDetect = AutoDetect.from(_textSource.get());
		if (autoDetect != null) {
			patDateExtract.ui.combo.setValue(autoDetect.pattern.pattern());
			patDate.ui.combo.setValue(autoDetect.formatterString);
		} else {
			patDateExtract.ui.combo.setValue(null);
			patDate.ui.combo.setValue(null);
		}
	}
}
