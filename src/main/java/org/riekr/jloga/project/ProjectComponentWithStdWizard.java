package org.riekr.jloga.project;

import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.misc.DateTimeFormatterRef;

import java.io.Serial;
import java.util.regex.Pattern;

import static org.riekr.jloga.project.StdFields.Date;
import static org.riekr.jloga.project.StdFields.DateExtractor;

public abstract class ProjectComponentWithStdWizard extends ProjectComponent implements AutoDetect.Wizard {
	@Serial private static final long serialVersionUID = -7459674268125654646L;

	public final ProjectEditableField<Pattern>              patDateExtract = newPatternField(DateExtractor, "Date extractor pattern:", 1);
	public final ProjectEditableField<DateTimeFormatterRef> patDate        = newDateTimeFormatterField(Date, "Date pattern:");

	public ProjectComponentWithStdWizard(String id, int level, String fileDescr) {
		super(id, level, fileDescr);
	}

	@Override
	public void onWizard() {
		AutoDetect autoDetect = AutoDetect.from(_textSource.get());
		if (autoDetect != null) {
			patDateExtract.set(autoDetect.pattern);
			patDate.set(autoDetect.formatterRef);
		} else {
			patDateExtract.set(null);
			patDate.set(null);
		}
	}
}
