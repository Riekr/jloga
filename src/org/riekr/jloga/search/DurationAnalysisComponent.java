package org.riekr.jloga.search;

public class DurationAnalysisComponent extends SearchProjectComponentWithExpandablePanel {

	public DurationAnalysisComponent(int level) {
		super(
				"DurationAnalysisComponent." + level,
				DurationAnalysisProject.class,
				"jloga-dap",
				"Duration analysis project"
		);
		buildUI();
	}

	@Override
	public String getLabel() {
		return "\u0394";
	}
}
