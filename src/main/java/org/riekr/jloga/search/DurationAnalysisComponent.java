package org.riekr.jloga.search;

import org.riekr.jloga.misc.Project;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.riekr.jloga.misc.StdFields.*;

public class DurationAnalysisComponent extends SearchProjectComponentWithExpandablePanel {

	public final Project.Field<Pattern> patDateExtract = newPatternField(DateExtractor, "Date extractor pattern:", 1);
	public final Project.Field<DateTimeFormatter> patDate = newDateTimeFormatterField(Date, "Date pattern:");
	public final Project.Field<Pattern> patFunc = newPatternField(Func, "Function pattern:", 1);
	public final Project.Field<Pattern> patStart = newPatternField(Start, "Start pattern:");
	public final Project.Field<Pattern> patEnd = newPatternField(End, "End pattern:");
	public final Project.Field<Pattern> patRestart = newPatternField(Restart, "Restart pattern:");
	public final Project.Field<Duration> minDuration = newDurationField(MinDuration, "Minimum duration:");

	public DurationAnalysisComponent(int level) {
		super("DurationAnalysisComponent." + level,
				"jloga",
				"Duration analysis project");
		buildUI();
	}

	@Override
	public String getLabel() {
		return "\u0394";
	}

	@Override
	public boolean isReady() {
		return patDateExtract.hasValue()
				&& patDate.hasValue()
				&& patStart.hasValue()
				&& patEnd.hasValue()
				&& patFunc.hasValue();
	}

	@Override
	protected SearchPredicate getSearchPredicate() {
		if (isReady()) {
			return new DurationAnalysis(
					patDateExtract.get(),
					patDate.get(),
					patFunc.get(),
					patStart.get(),
					patEnd.get(),
					patRestart.get(),
					minDuration.get()
			);
		}
		return null;
	}
}
