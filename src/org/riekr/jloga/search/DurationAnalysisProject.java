package org.riekr.jloga.search;

import org.riekr.jloga.misc.Project;

import javax.swing.*;

public class DurationAnalysisProject extends Project {

	public final PatternField patDateExtract = new PatternField("DateExtractor", "Date extractor pattern:", 1);
	public final DateTimeFormatterField patDate = new DateTimeFormatterField("Date", "Date pattern:");
	public final PatternField patFunc = new PatternField("Func", "Function pattern:", 1);
	public final PatternField patStart = new PatternField("Start", "Start pattern:");
	public final PatternField patEnd = new PatternField("End", "End pattern:");
	public final PatternField patRestart = new PatternField("Restart", "Restart pattern:");
	public final DurationField minDuration = new DurationField("MinDuration", "Minimum duration:");

	public DurationAnalysisProject(JComponent owner) {
		super(owner);
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
	public String toString() {
		return "Date: " + patDate
				+ " | Start: " + patStart
				+ " | End: " + patEnd
				+ " | Func: " + patFunc
				+ " | Restart: " + patRestart
				+ " | Minimum duration: " + minDuration;
	}

	@Override
	public DurationAnalysis get() {
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

}
