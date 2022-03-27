package org.riekr.jloga.search;

import static org.riekr.jloga.misc.StdFields.Func;
import static org.riekr.jloga.misc.StdFields.Period;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public class FrequencyAnalysisComponent extends SearchProjectComponentWithExpandablePanel.WithStdWizard {
	private static final long serialVersionUID = -3621473265054156767L;

	public static final String ID = "FrequencyAnalysisComponent";

	public final Field<Pattern>  patFunc        = newPatternField(Func, "Function pattern:", 0);
	public final Field<Duration> periodDuration = newDurationField(Period, "Period:", Duration.of(1, ChronoUnit.MINUTES));

	public FrequencyAnalysisComponent(int level) {
		super(ID, level, "Frequency analysis project");
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
		return new FrequencyAnalysis(
				patDateExtract.get(),
				patDate.get().formatter,
				patFunc.get(),
				periodDuration.get()
		);
	}
}
