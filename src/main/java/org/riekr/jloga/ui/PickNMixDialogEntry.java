package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.riekr.jloga.io.MixFileSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.AutoDetect;

public class PickNMixDialogEntry extends JComponent {

	private final BiConsumer<Boolean, PickNMixDialogEntry> _consumer;

	private final JCheckBox         _checkBox;
	private       Pattern           _dateExtract;
	private       DateTimeFormatter _dateFormatter;
	private       Duration          _offset;

	public PickNMixDialogEntry(File f, TextSource textSource, BiConsumer<Boolean, PickNMixDialogEntry> consumer) {
		_consumer = consumer;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		MRUComboWithLabels<Pattern> dateExtractComp = newCombo("dateExtract", "Date extract pattern:", this::setDateExtract, (ui, text) -> UIUtils.toPattern(ui, text, 1));
		dateExtractComp.setVisible(false);

		MRUComboWithLabels<DateTimeFormatter> dateFormatterComp = newCombo("dateFormat", "Date format pattern:", this::setDateFormat, UIUtils::toDateTimeFormatter);
		dateFormatterComp.setVisible(false);

		JComponent offsetComp = newCombo("offset", "Date offset:", this::setOffset, UIUtils::toDuration);
		offsetComp.setVisible(false);

		JButton wizard = AutoDetect.newButtonFor(textSource, dateExtractComp, dateFormatterComp);
		wizard.setVisible(false);

		_checkBox = new JCheckBox(f.getName());
		_checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		_checkBox.addActionListener((e) -> {
			if (_checkBox.isSelected()) {
				consumer.accept(true, this);
				dateExtractComp.setVisible(true);
				dateFormatterComp.setVisible(true);
				offsetComp.setVisible(true);
				wizard.setVisible(true);
			} else {
				consumer.accept(false, this);
				dateExtractComp.setVisible(false);
				dateFormatterComp.setVisible(false);
				offsetComp.setVisible(false);
				wizard.setVisible(false);
			}
		});

		Box checkBoxCont = Box.createHorizontalBox();
		checkBoxCont.add(_checkBox);
		checkBoxCont.add(Box.createHorizontalGlue());
		checkBoxCont.add(wizard);
		add(checkBoxCont);
		add(dateExtractComp);
		add(dateFormatterComp);
		add(offsetComp);
	}

	private <T> MRUComboWithLabels<T> newCombo(String key, String label, Consumer<T> consumer, BiFunction<Component, String, T> mapper) {
		AtomicReference<MRUComboWithLabels<T>> ref = new AtomicReference<>();
		ref.set(new MRUComboWithLabels<>("PickNMix." + key, label, consumer, (text) -> mapper.apply(ref.get(), text)));
		EventQueue.invokeLater(() -> ref.get().combo.subject.next((String)ref.get().combo.getSelectedItem()));
		return ref.get();
	}

	private void setDateExtract(Pattern dateExtract) {
		_dateExtract = dateExtract;
		_consumer.accept(_checkBox.isSelected(), this);
	}

	private void setDateFormat(DateTimeFormatter dateFormatter) {
		_dateFormatter = dateFormatter;
		_consumer.accept(_checkBox.isSelected(), this);
	}

	private void setOffset(Duration offset) {
		_offset = offset;
		_consumer.accept(_checkBox.isSelected(), this);
	}

	public MixFileSource.SourceConfig getConfig(File file) {
		return new MixFileSource.SourceConfig(file, _dateExtract, _dateFormatter, _offset);
	}
}
