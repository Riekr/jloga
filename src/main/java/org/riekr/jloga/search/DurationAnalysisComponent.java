package org.riekr.jloga.search;

import static org.riekr.jloga.misc.StdFields.End;
import static org.riekr.jloga.misc.StdFields.Func;
import static org.riekr.jloga.misc.StdFields.MinDuration;
import static org.riekr.jloga.misc.StdFields.Restart;
import static org.riekr.jloga.misc.StdFields.Start;

import java.time.Duration;
import java.util.regex.Pattern;

import org.riekr.jloga.misc.Project;

public class DurationAnalysisComponent extends SearchProjectComponentWithExpandablePanel.WithStdWizard {
	public static final String ID = "DurationAnalysisComponent";

	private static final long serialVersionUID = -5133137752144513068L;

	public final Project.Field<Pattern>  patFunc     = newPatternField(Func, "Function pattern:", 1);
	public final Project.Field<Pattern>  patStart    = newPatternField(Start, "Start pattern:");
	public final Project.Field<Pattern>  patEnd      = newPatternField(End, "End pattern:");
	public final Project.Field<Pattern>  patRestart  = newPatternField(Restart, "Restart pattern:");
	public final Project.Field<Duration> minDuration = newDurationField(MinDuration, "Minimum duration:");

	public DurationAnalysisComponent(int level) {
		super(ID, level, "Duration analysis project");
		buildUI();
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getSearchIconLabel() {
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
