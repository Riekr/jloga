package org.riekr.jloga.search;

import static org.riekr.jloga.misc.StdFields.Date;
import static org.riekr.jloga.misc.StdFields.DateExtractor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.riekr.jloga.io.PropsIO;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.misc.DateTimeFormatterRef;
import org.riekr.jloga.misc.Project;
import org.riekr.jloga.ui.MRUComboWithLabels;

public abstract class SearchProjectComponentWithExpandablePanel extends SearchComponentWithExpandablePanel implements Project {
	private static final long serialVersionUID = -1685626672776376188L;

	protected abstract static class WithStdWizard extends SearchProjectComponentWithExpandablePanel implements AutoDetect.Wizard {
		private static final long serialVersionUID = -7459674268125654646L;

		public final Field<Pattern>              patDateExtract = newPatternField(DateExtractor, "Date extractor pattern:", 1);
		public final Field<DateTimeFormatterRef> patDate        = newDateTimeFormatterField(Date, "Date pattern:");

		public WithStdWizard(String id, int level, String fileDescr) {
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

	private final Map<String, MRUComboWithLabels<String>> _combos = new HashMap<>();

	private final String _fileExt;
	private final String _fileDescr;

	protected Supplier<TextSource> _textSource;

	public SearchProjectComponentWithExpandablePanel(String id, int level, String fileDescr) {
		this(id + '.' + level, "jloga", fileDescr);
	}

	@Deprecated
	public SearchProjectComponentWithExpandablePanel(String prefsPrefix, String fileExt, String fileDescr) {
		super(prefsPrefix);
		_fileExt = fileExt;
		_fileDescr = fileDescr;
	}

	@Override
	protected void setupConfigPane(Container configPane) {
		fields().forEach((Field<?> f) -> {
			MRUComboWithLabels<String> combo = newEditableField(f.key, f.label, f);
			configPane.add(combo);
			_combos.put(f.key, combo);
			f.ui = combo;
		});
	}

	@Override
	protected void setupConfigPaneButtons(Container configPaneButtons) {
		if (AutoDetect.Wizard.class.isAssignableFrom(getClass())) {
			configPaneButtons.add(newButton(AutoDetect.GLYPH, ((AutoDetect.Wizard)this)::onWizard));
			configPaneButtons.add(newButtonSpacer());
		}
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, this, _fileExt, _fileDescr, this::updateConfigPanel)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, this, _fileExt, _fileDescr)));
		configPaneButtons.add(newButtonSpacer());
		configPaneButtons.add(newButton("Reset!", this::clear));
	}

	@Override
	public void clear() {
		Project.super.clear();
		updateConfigPanel();
	}

	protected void updateConfigPanel() {
		fields().forEach((field) -> {
			MRUComboWithLabels<String> combo = _combos.get(field.key);
			if (combo != null)
				combo.combo.setValue(field.toString());
		});
	}

	protected void calcTitle() {
		if (isReady())
			setText(getDescription());
		else
			setText("Hey! Hover here!");
	}

	@Override
	protected void search() {
		if (isReady())
			super.search();
	}

	public final void setTextSourceSupplier(Supplier<TextSource> textSource) {
		_textSource = textSource;
	}
}
