package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.InstantRange;
import org.riekr.jloga.prefs.Preferences;

public class PickNMixOptionPane {

	private static final String _TITLE = "Pick'n'mix";

	private static final ZoneId _ZONE_ID = ZoneId.systemDefault();

	private static final DateTimeFormatter _DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
			.withZone(_ZONE_ID)
			.withLocale(Preferences.LOCALE.get());

	private static final DateTimeFormatter _TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
			.withZone(_ZONE_ID)
			.withLocale(Preferences.LOCALE.get());

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
		inputFiles.entrySet().stream().map((e) -> new PickNMixDialogEntry(e.getKey(), e.getValue(), (selected, entry) -> {
					if (selected)
						selectedFiles.put(e.getKey(), entry);
					else
						selectedFiles.remove(e.getKey());
					EventQueue.invokeLater(dialog::pack);
				}))
				.forEachOrdered(options::add);
		AtomicReference<LocalDate> fromDate = new AtomicReference<>();
		AtomicReference<LocalTime> fromTime = new AtomicReference<>();
		AtomicReference<LocalDate> toDate = new AtomicReference<>();
		AtomicReference<LocalTime> toTime = new AtomicReference<>();
		options.add(getDateTimePicker("From:", LocalTime.MIN, fromDate::set, fromTime::set));
		options.add(getDateTimePicker("To:", LocalTime.MAX, toDate::set, toTime::set));
		optionPane.setMessage(options.toArray());
		EventQueue.invokeLater(dialog::pack);
		dialog.setMinimumSize(new Dimension(480, 0));
		//		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.dispose();

		if (optionPane.getValue() == (Integer)JOptionPane.OK_OPTION) {
			if (selectedFiles.size() >= 2) {
				Map<TextSource, MixFileSource.SourceConfig> res = new HashMap<>();
				selectedFiles.forEach((k, v) -> res.put(inputFiles.get(k), v.getConfig(k)));
				return new MixFileSource.Config(res, InstantRange.from(
						fromDate.get(), fromTime.get(),
						toDate.get(), toTime.get()
				));
			}
			JOptionPane.showMessageDialog(parentComponent, "Please select more than 1 log file", _TITLE, JOptionPane.INFORMATION_MESSAGE);
		}

		return null;
	}

	private static <T extends TemporalAccessor> JComponent getPicker(String label, Consumer<T> consumer, TemporalQuery<T> query, Supplier<T> initialValueSupplier, DateTimeFormatter formatter) {
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

		String initialValue = formatter.format(initialValueSupplier.get());
		checkBox.addActionListener((e) -> {
			textField.setEnabled(checkBox.isSelected());
			if (textField.isEnabled()) {
				if (textField.getText().isBlank())
					textField.setText(initialValue);
			} else
				consumer.accept(null);
		});
		Color fg = textField.getForeground();
		textField.getDocument().addDocumentListener((SimpleDocumentListener)e -> {
			try {
				String text = textField.getText();
				if (text.length() < initialValue.length())
					text += initialValue.substring(text.length());
				T date = formatter.parse(text, query);
				consumer.accept(date);
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

	private static JComponent getDateTimePicker(String label, TemporalAdjuster timeAdjuster, Consumer<LocalDate> dateConsumer, Consumer<LocalTime> timeConsumer) {
		Box box = Box.createHorizontalBox();
		box.add(getPicker(label, dateConsumer, LocalDate::from, LocalDate::now, _DATE_FORMATTER));
		box.add(getPicker("time:", timeConsumer, LocalTime::from, () -> {
			LocalTime initialValue = LocalTime.now();
			if (timeAdjuster != null)
				initialValue = initialValue.with(timeAdjuster);
			return initialValue;
		}, _TIME_FORMATTER));
		return box;
	}

}
