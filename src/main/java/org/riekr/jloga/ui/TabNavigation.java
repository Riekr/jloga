package org.riekr.jloga.ui;

import javax.swing.*;

public interface TabNavigation {

	static TabNavigation createFor(JTabbedPane tabbedPane) {
		return new TabNavigation() {
			private void set(int newIndex) {
				if (newIndex >= 0 && newIndex < tabbedPane.getTabCount())
					tabbedPane.setSelectedIndex(newIndex);
			}

			@Override
			public void goToPreviousTab() {
				set(tabbedPane.getSelectedIndex() - 1);
			}

			@Override
			public void goToNextTab() {
				set(tabbedPane.getSelectedIndex() + 1);
			}
		};
	}

	void goToPreviousTab();

	void goToNextTab();
}
