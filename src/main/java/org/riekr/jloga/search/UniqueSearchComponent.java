package org.riekr.jloga.search;

import org.riekr.jloga.project.ProjectComponentWithStdWizard;

import static org.riekr.jloga.project.StdFields.SearchPat;

import java.util.regex.Pattern;

public class UniqueSearchComponent extends ProjectComponentWithStdWizard {
	private static final long serialVersionUID = -9087197176161972847L;

	public static final String ID = "UniqueSearchComponent";

	public final Field<Pattern> pattern = newPatternField(SearchPat, "Pattern:", 1);

	public UniqueSearchComponent(int level) {
		super(ID, level, "Unique search project");
		buildUI();
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getSearchIconLabel() {
		return "U";
	}

	@Override
	public boolean isReady() {
		return pattern.hasValue();
	}

	@Override
	protected SearchPredicate getSearchPredicate() {
		return new UniqueSearch(
				pattern.get(),
				patDateExtract.get(),
				patDate.get().formatter
		);
	}
}
