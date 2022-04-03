package org.riekr.jloga.project;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;
import org.riekr.jloga.search.SearchComponentWithExpandablePanel;

import javax.swing.*;

public abstract class ProjectComponent extends SearchComponentWithExpandablePanel implements Project {
	private static final long serialVersionUID = -1685626672776376188L;

	private final String _fileExt;
	private final String _fileDescr;

	protected Supplier<TextSource> _textSource;

	public ProjectComponent(String id, int level, String fileDescr) {
		this(id + '.' + level, "jloga", fileDescr);
	}

	@Deprecated
	public ProjectComponent(String prefsPrefix, String fileExt, String fileDescr) {
		super(prefsPrefix);
		_fileExt = fileExt;
		_fileDescr = fileDescr;
	}

	@Override
	protected void setupConfigPane(Container configPane) {
		List<ProjectCheckboxField<?>> checkboxFieldList = new ArrayList<>();
		fields().forEach((ProjectField<?, ?> f) -> {
			if (f instanceof ProjectCheckboxField)
				checkboxFieldList.add((ProjectCheckboxField<?>)f);
			else
				configPane.add(f.ui(this));
		});
		if (!checkboxFieldList.isEmpty()) {
			Box checkboxPanel = Box.createHorizontalBox();
			checkboxFieldList.forEach((f) -> checkboxPanel.add(f.ui(this)));
			checkboxPanel.add(Box.createHorizontalGlue());
			configPane.add(checkboxPanel);
		}
	}

	@Override
	protected void setupConfigPaneButtons(Container configPaneButtons) {
		if (AutoDetect.Wizard.class.isAssignableFrom(getClass())) {
			configPaneButtons.add(newButton(AutoDetect.GLYPH, ((AutoDetect.Wizard)this)::onWizard));
			configPaneButtons.add(newButtonSpacer());
		}
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, this, _fileExt, _fileDescr)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, this, _fileExt, _fileDescr)));
		configPaneButtons.add(newButtonSpacer());
		configPaneButtons.add(newButton("Reset!", this::clear));
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
