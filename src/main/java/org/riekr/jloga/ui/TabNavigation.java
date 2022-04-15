package org.riekr.jloga.ui;

import static org.riekr.jloga.utils.TextUtils.TAB_ADD;

import javax.swing.*;

public final class TabNavigation {

	private final JTabbedPane _tabbedPane;

	public TabNavigation(JTabbedPane tabbedPane) {
		_tabbedPane = tabbedPane;
		_tabbedPane.addMouseWheelListener(e -> {
			int units = e.getWheelRotation();
			int oldIndex = _tabbedPane.getSelectedIndex();
			set(oldIndex + units);
		});
	}

	private void set(int newIndex) {
		if (newIndex >= 0 && newIndex < _tabbedPane.getTabCount() && !TAB_ADD.equals(_tabbedPane.getTitleAt(newIndex)))
			_tabbedPane.setSelectedIndex(newIndex);
	}

	public void goToPreviousTab() {
		set(_tabbedPane.getSelectedIndex() - 1);
	}

	public void goToNextTab() {
		set(_tabbedPane.getSelectedIndex() + 1);
	}

}
