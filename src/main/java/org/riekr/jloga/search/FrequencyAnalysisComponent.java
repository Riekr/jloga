package org.riekr.jloga.search;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import static org.riekr.jloga.misc.StdFields.*;

public class FrequencyAnalysisComponent extends SearchProjectComponentWithExpandablePanel {

	public final Field<Pattern> patDateExtract = newPatternField(DateExtractor, "Date extractor pattern:", 1);
	public final Field<DateTimeFormatter> patDate = newDateTimeFormatterField(Date, "Date pattern:");
	public final Field<Pattern> patFunc = newPatternField(Func, "Function pattern:", 0);
	public final Field<Duration> periodDuration = newDurationField(Period, "Period:", Duration.of(1, ChronoUnit.MINUTES));

	public FrequencyAnalysisComponent(int level) {
		super("FrequencyAnalysisComponent." + level,
				"jloga",
				"Frequency analysis project");
		buildUI();
	}

	@Override
	public String getLabel() {
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
}
