package org.riekr.jloga.search;

import org.riekr.jloga.misc.Project;

import java.awt.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class DurationAnalysisComponent extends SearchProjectComponentWithExpandablePanel {

	public final Project.Field<Pattern> patDateExtract = newPatternField("DateExtractor", "Date extractor pattern:", 1);
	public final Project.Field<DateTimeFormatter> patDate = newDateTimeFormatterField("Date", "Date pattern:");
	public final Project.Field<Pattern> patFunc = newPatternField("Func", "Function pattern:", 1);
	public final Project.Field<Pattern> patStart = newPatternField("Start", "Start pattern:");
	public final Project.Field<Pattern> patEnd = newPatternField("End", "End pattern:");
	public final Project.Field<Pattern> patRestart = newPatternField("Restart", "Restart pattern:");
	public final Project.Field<Duration> minDuration = newDurationField("MinDuration", "Minimum duration:");

	public DurationAnalysisComponent(int level) {
		super("DurationAnalysisComponent." + level,
				"jloga-dap",
				"Duration analysis project");
		buildUI();
	}

	@Override
	public String getLabel() {
		return "\u0394";
	}

	@Override
	public Component getDialogParentComponent() {
		return this;
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
	public String getDescription() {
		return "Date: " + patDate
				+ " | Start: " + patStart
				+ " | End: " + patEnd
				+ " | Func: " + patFunc
				+ " | Restart: " + patRestart
				+ " | Minimum duration: " + minDuration;
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
