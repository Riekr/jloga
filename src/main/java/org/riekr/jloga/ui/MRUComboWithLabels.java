package org.riekr.jloga.ui;

import static org.riekr.jloga.react.Observer.uniq;
import static org.riekr.jloga.ui.MRUTextCombo.newMRUTextCombo;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MRUComboWithLabels<T> extends JComponent {
	@Serial private static final long serialVersionUID = -4669818889063163941L;

	public static MRUComboWithLabels<String> forString(String key, String label, Consumer<String> onResult) {
		return new MRUComboWithLabels<>(key, label, onResult, Function.identity());
	}

	public final MRUTextCombo<String> combo;
	public final JLabel               error;

	public MRUComboWithLabels(@NotNull String key, @Nullable String label, @Nullable Consumer<T> onResult, @NotNull Function<String, T> mapper) {
		setLayout(new BorderLayout());

		if (label != null && !(label = label.trim()).isEmpty())
			this.add(new JLabel(label + " "), BorderLayout.LINE_START);

		this.combo = newMRUTextCombo(key);
		this.add(combo, BorderLayout.CENTER);

		this.error = new JLabel();
		error.setHorizontalAlignment(SwingConstants.CENTER);
		error.setForeground(Color.RED);
		error.setVisible(false);
		this.add(error, BorderLayout.SOUTH);

		combo.selection.subscribe(uniq((text) -> {
			T res = mapper.apply(text);
			if (res == null)
				combo.setSelectedIndex(-1);
			if (onResult != null)
				onResult.accept(res);
		}));
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
