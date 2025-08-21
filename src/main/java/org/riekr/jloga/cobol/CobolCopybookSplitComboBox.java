package org.riekr.jloga.cobol;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import net.sf.JRecord.Option.ICobolSplitOptions;

record CopybookSplit(int val, String name) {
	@Override
	public @NotNull String toString() {return name;}
}

class CobolCopybookSplitComboBox extends JComboBox<CopybookSplit> {

	private static final CopybookSplit   _DEFAULT;
	private static final CopybookSplit[] _VALUES = new CopybookSplit[]{
			_DEFAULT = new CopybookSplit(ICobolSplitOptions.SPLIT_NONE, "None"),
			new CopybookSplit(ICobolSplitOptions.SPLIT_REDEFINE, "Redefine"),
			new CopybookSplit(ICobolSplitOptions.SPLIT_01_LEVEL, "1st level"),
			new CopybookSplit(ICobolSplitOptions.SPLIT_TOP_LEVEL, "Top level"),
			new CopybookSplit(ICobolSplitOptions.SPLIT_HIGHEST_REPEATING, "Highest repeating")
	};

	CobolCopybookSplitComboBox() {
		super(_VALUES);
		setSelectedItem(_DEFAULT);
	}

	public Integer getValue() {
		final int i = super.getSelectedIndex();
		return i < 0 ? null : _VALUES[i].val();
	}

}
