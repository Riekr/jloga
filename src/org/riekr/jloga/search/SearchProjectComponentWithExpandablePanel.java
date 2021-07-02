package org.riekr.jloga.search;

import org.riekr.jloga.io.PropsIO;
import org.riekr.jloga.misc.Project;
import org.riekr.jloga.ui.MRUTextComboWithLabel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class SearchProjectComponentWithExpandablePanel extends SearchComponentWithExpandablePanel {

	private final Project _project;

	private final Map<String, MRUTextComboWithLabel<String>> _combos = new HashMap<>();

	private final String _fileExt;
	private final String _fileDescr;

	public SearchProjectComponentWithExpandablePanel(String prefsPrefix, Class<? extends Project> projectTypeClass, String fileExt, String fileDescr) {
		super(prefsPrefix);
		_fileExt = fileExt;
		_fileDescr = fileDescr;
		try {
			_project = projectTypeClass.getConstructor(JComponent.class).newInstance(this);
		} catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace(System.err);
			throw new IllegalArgumentException("Invalid project class: " + projectTypeClass.getCanonicalName());
		}
	}

	@Override
	protected void setupConfigPane(Container configPane) {
		_project.fields().forEach((f) -> {
			MRUTextComboWithLabel<String> combo = newEditableField(f.key, f.label, f);
			configPane.add(combo);
			_combos.put(f.key, combo);
		});
	}

	@Override
	protected void setupConfigPaneButtons(Container configPaneButtons) {
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, _project, _fileExt, _fileDescr, this::updateConfigPanel)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, _project, _fileExt, _fileDescr)));
		configPaneButtons.add(newButtonSpacer());
		configPaneButtons.add(newButton("Reset!", () -> PropsIO.reset(_project, this::updateConfigPanel)));
	}

	protected void updateConfigPanel() {
		_project.fields().forEach((field) -> {
			MRUTextComboWithLabel<String> combo = _combos.get(field.key);
			if (combo != null)
				combo.combo.set(field.toString());
		});
	}

	protected void calcTitle() {
		if (_project.isReady())
			setText(_project.toString());
		else
			setText("Hey! Hover here!");
	}

	@Override
	protected SearchPredicate getSearchPredicate() {
		if (_project.isReady())
			return _project.get();
		return null;
	}


}
