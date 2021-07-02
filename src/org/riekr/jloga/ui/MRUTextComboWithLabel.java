package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class MRUTextComboWithLabel<T> extends JPanel {

	public static MRUTextComboWithLabel<String> forString(String key, String label, Consumer<String> onResult) {
		return new MRUTextComboWithLabel<>(key, label, onResult, Function.identity());
	}

	public final MRUTextCombo combo;

	public MRUTextComboWithLabel(String key, String label, Consumer<T> onResult, Function<String, T> mapper) {
		this.setLayout(new BorderLayout());
		this.add(new JLabel(label.trim() + " "), BorderLayout.LINE_START);
		this.combo = new MRUTextCombo(key);
		this.add(combo, BorderLayout.CENTER);

		Consumer<String> onText = (text) -> {
			T res = mapper.apply(text);
			if (res == null)
				combo.setSelectedIndex(-1);
			onResult.accept(res);
		};
		combo.setListener(onText);
	}

}
