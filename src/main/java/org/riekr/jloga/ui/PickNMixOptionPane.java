package org.riekr.jloga.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PickNMixOptionPane {

	private static final String _TITLE = "Pick'n'mix";

	private static final ZoneId _ZONE_ID = ZoneId.systemDefault();

	private static final DateTimeFormatter _FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
			.withZone(_ZONE_ID)
			.withLocale(Locale.ENGLISH);


	/**
	 * Will repropose preview values until the app is restarted, this is intended behaviour as saving
	 * dates across different days may be more usless than useful.
	 */
	private static final Map<TemporalAdjuster, String> _INITIAL_VALUES = new HashMap<>() {
		private static final long serialVersionUID = -5550699003856362764L;

		@Override
		public String get(Object key) {
			String res = super.get(key);
			if (res == null && key instanceof TemporalAdjuster) {
				res = LocalDateTime.now(_ZONE_ID).with((TemporalAdjuster) key).format(_FORMATTER);
				put((TemporalAdjuster) key, res);
			}
			return res;
		}
	};


	@Nullable
	public static MixFileSource.Config show(@NotNull Map<File, TextSource> inputFiles, @Nullable Component parentComponent) {
		if (inputFiles.size() < 2) {
			JOptionPane.showMessageDialog(parentComponent, "Please open more than 1 log file first", _TITLE, JOptionPane.INFORMATION_MESSAGE);
			return null;
		}

		HashMap<File, PickNMixDialogEntry> selectedFiles = new HashMap<>();
		JOptionPane optionPane = new JOptionPane(null, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = optionPane.createDialog(parentComponent, _TITLE);
		ArrayList<Object> options = new ArrayList<>();
		inputFiles.keySet().stream().map((f) -> new PickNMixDialogEntry(f, (selected, entry) -> {
					if (selected)
						selectedFiles.put(f, entry);
					else
						selectedFiles.remove(f);
					EventQueue.invokeLater(dialog::pack);
				}))
				.forEachOrdered(options::add);
		AtomicReference<Instant> from = new AtomicReference<>();
		AtomicReference<Instant> to = new AtomicReference<>();
		options.add(getDateTimePicker("From:", LocalTime.MIN, from::set));
		options.add(getDateTimePicker("To:", LocalTime.MAX, to::set));
		optionPane.setMessage(options.toArray());
		EventQueue.invokeLater(dialog::pack);
		dialog.setMinimumSize(new Dimension(480, 0));
//		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();

		if (optionPane.getValue() == (Integer) JOptionPane.OK_OPTION) {
			if (selectedFiles.size() >= 2) {
				Map<TextSource, MixFileSource.SourceConfig> res = new HashMap<>();
				selectedFiles.forEach((k, v) -> res.put(inputFiles.get(k), v.getConfig(k)));
				return new MixFileSource.Config(res, from.get(), to.get());
			}
			JOptionPane.showMessageDialog(parentComponent, "Please select more than 1 log file", _TITLE, JOptionPane.INFORMATION_MESSAGE);
		}

		return null;
	}

	private static JComponent getDateTimePicker(String label, TemporalAdjuster adjuster, Consumer<Instant> consumer) {
		Box box = Box.createVerticalBox();
		Box textFieldBox = Box.createHorizontalBox();

		JCheckBox checkBox = new JCheckBox(label);
		textFieldBox.add(checkBox);

		JTextField textField = new JTextField();
		textField.setEnabled(false);
		textFieldBox.add(textField);

		JLabel error = new JLabel();
		error.setVisible(false);
		error.setAlignmentX(Component.CENTER_ALIGNMENT);
		error.setForeground(Color.RED);

		String initialValue = _INITIAL_VALUES.get(adjuster);
		checkBox.addActionListener((e) -> {
			textField.setEnabled(checkBox.isSelected());
			if (textField.isEnabled()) {
				if (textField.getText().isBlank())
					textField.setText(initialValue);
			} else
				consumer.accept(null);
		});
		Color fg = textField.getForeground();
		textField.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
			try {
				String text = textField.getText();
				if (text.length() < initialValue.length())
					text += initialValue.substring(text.length());
				Instant instant = _FORMATTER.parse(text, LocalDateTime::from).toInstant(ZoneOffset.UTC);
				consumer.accept(instant);
				textField.setForeground(fg);
				error.setVisible(false);
			} catch (DateTimeParseException ex) {
				consumer.accept(null);
				textField.setForeground(Color.RED);
				error.setText(ex.getLocalizedMessage());
				error.setToolTipText(ex.getLocalizedMessage());
				error.setVisible(true);
			}
		});
		box.add(textFieldBox);
		box.add(error);
		return box;
	}

}
