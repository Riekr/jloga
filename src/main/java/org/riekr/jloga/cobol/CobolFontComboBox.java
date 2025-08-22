package org.riekr.jloga.cobol;

import java.util.function.Function;

import org.riekr.jloga.ui.MRUComboWithLabels;

class CobolFontComboBox extends MRUComboWithLabels<String> {

	public CobolFontComboBox() {
		super("COBOL_FONT", "Datafile code page:", null, Function.identity());
		if (combo.getValue() == null)
			combo.setValue("CP037");
		final String toolTipText = "The \"font\" setting specifies the character encoding for EBCDIC data, such as \"CP037\" for U.S. EBCDIC";
		setToolTipText(toolTipText);
		combo.setToolTipText(toolTipText);
	}

}
