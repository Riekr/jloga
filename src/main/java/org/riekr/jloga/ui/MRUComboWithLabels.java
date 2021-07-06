package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class MRUComboWithLabels<T> extends JPanel {

	public static MRUComboWithLabels<String> forString(String key, String label, Consumer<String> onResult) {
		return new MRUComboWithLabels<>(key, label, onResult, Function.identity());
	}

	public final MRUTextCombo combo;
	public final JLabel error;

	public MRUComboWithLabels(String key, String label, Consumer<T> onResult, Function<String, T> mapper) {
		this.setLayout(new BorderLayout());
		this.add(new JLabel(label.trim() + " "), BorderLayout.LINE_START);

		this.combo = new MRUTextCombo(key);
		this.add(combo, BorderLayout.CENTER);

		this.error = new JLabel();
		error.setHorizontalAlignment(SwingConstants.CENTER);
		error.setForeground(Color.RED);
		error.setVisible(false);
		this.add(error, BorderLayout.SOUTH);

		Consumer<String> onText = (text) -> {
			T res = mapper.apply(text);
			if (res == null)
				combo.setSelectedIndex(-1);
			onResult.accept(res);
		};
		combo.setListener(onText);
	}

	public void setError(String text) {
		if (text == null || (text = text.trim()).isEmpty()) {
			error.setText(null);
			error.setVisible(false);
		} else {
			error.setText(text);
			error.setVisible(true);
		}
	}

}
