package org.riekr.jloga.search;

import static org.riekr.jloga.project.StdFields.Func;
import static org.riekr.jloga.project.StdFields.Period;

import java.io.Serial;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import org.riekr.jloga.project.ProjectComponentWithStdWizard;
import org.riekr.jloga.project.ProjectEditableField;

public class FrequencyAnalysisComponent extends ProjectComponentWithStdWizard {
	@Serial private static final long serialVersionUID = -3621473265054156767L;

	public static final String ID = "FrequencyAnalysisComponent";

	public final ProjectEditableField<Pattern>  patFunc        = newPatternField(Func, "Function pattern:", 0);
	public final ProjectEditableField<Duration> periodDuration = newDurationField(Period, "Period:", Duration.of(1, ChronoUnit.MINUTES));

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
