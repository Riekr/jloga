package org.riekr.jloga.cobol;

import org.riekr.jloga.ui.MRUTextCombo;

class CobolFontComboBox extends MRUTextCombo<String> {

	public CobolFontComboBox() {
		super("COBOL_FONT", (newValue, oldValue) -> newValue);
		if (getValue() == null)
			setValue("CP037");
	}

}
