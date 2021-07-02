package org.riekr.jloga.search;

import org.riekr.jloga.io.PropsIO;
import org.riekr.jloga.misc.Project;
import org.riekr.jloga.ui.MRUTextComboWithLabel;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class SearchProjectComponentWithExpandablePanel extends SearchComponentWithExpandablePanel implements Project {

	private final Map<String, MRUTextComboWithLabel<String>> _combos = new HashMap<>();

	private final String _fileExt;
	private final String _fileDescr;

	public SearchProjectComponentWithExpandablePanel(String prefsPrefix, String fileExt, String fileDescr) {
		super(prefsPrefix);
		_fileExt = fileExt;
		_fileDescr = fileDescr;
	}

	@Override
	protected void setupConfigPane(Container configPane) {
		fields().forEach((f) -> {
			MRUTextComboWithLabel<String> combo = newEditableField(f.key, f.label, f);
			configPane.add(combo);
			_combos.put(f.key, combo);
		});
	}

	@Override
	protected void setupConfigPaneButtons(Container configPaneButtons) {
		configPaneButtons.add(newButton("Load...", () -> PropsIO.requestLoad(this, _fileExt, _fileDescr, this::updateConfigPanel)));
		configPaneButtons.add(newButton("Save...", () -> PropsIO.requestSave(this, _fileExt, _fileDescr)));
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
			MRUTextComboWithLabel<String> combo = _combos.get(field.key);
			if (combo != null)
				combo.combo.set(field.toString());
		});
	}

	protected void calcTitle() {
		if (isReady())
			setText(getDescription());
		else
			setText("Hey! Hover here!");
	}

}
