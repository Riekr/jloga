package org.riekr.jloga.ui;

import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.react.BoolConsumer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class UIUtils {

	public static final Border BUTTON_BORDER = new EmptyBorder(6, 8, 6, 8);

	private UIUtils() {
	}

	public static void invokeAfter(Runnable runnable, int delay) {
		Timer timer = new Timer(delay, (ev) -> runnable.run());
		timer.setRepeats(false);
		timer.start();
	}

	public static Dimension half(Dimension d) {
		d.height /= 2;
		d.width /= 2;
		return d;
	}

	public static JButton newButton(String text, Runnable action, String tooltip) {
		JButton res = newButton(text, action);
		res.setToolTipText(tooltip);
		return res;
	}

	public static JButton newButton(String text, Runnable action) {
		JButton btn = new JButton(text);
		btn.setBorderPainted(false);
		btn.setContentAreaFilled(false);
		btn.setBorder(BUTTON_BORDER);
		btn.addActionListener((e) -> action.run());
		return btn;
	}

	public static JToggleButton newToggleButton(String text, String tooltip, boolean initialValue, BoolConsumer consumer) {
		JToggleButton btn = new JToggleButton(text);
		btn.setBorder(BUTTON_BORDER);
		btn.setToolTipText(tooltip);
		btn.setSelected(initialValue);
		if (consumer != null)
			btn.addActionListener((e) -> consumer.accept(btn.isSelected()));
		return btn;
	}

	public static Component newTabHeader(String text, @Nullable Runnable onClose, @Nullable Runnable onSelect) {
		JLabel label = new JLabel(text);
		if (onClose == null) {
			if (onSelect != null) {
				label.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						onSelect.run();
					}
				});
			}
			return label;
		}
		Box box = new Box(BoxLayout.LINE_AXIS);
		box.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2 && e.isAltDown() && e.getClickCount() == 1)
					onClose.run();
				else if (onSelect != null)
					onSelect.run();
			}
		});
		box.add(label);
		box.add(Box.createHorizontalStrut(5));
		box.add(newButton("\u274C", onClose, "Close " + text));
		return box;
	}

	private static void dispatchErrorMessage(Component component, String message, String title) {
		if (component instanceof MRUComboWithLabels)
			((MRUComboWithLabels<?>) component).setError(title + ": " + message);
		else
			JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private static void dispatchErrorCleared(Component component) {
		if (component instanceof MRUComboWithLabels)
			((MRUComboWithLabels<?>) component).setError(null);
	}

	public static Pattern toPattern(Component component, String text, int minGroups) {
		return toPattern(component, text, minGroups, 0);
	}

	public static Pattern toPattern(Component component, String text, int minGroups, int flags) {
		if (text != null && !text.isBlank()) {
			try {
				Pattern pat = Pattern.compile(text, flags);
				if (minGroups > 0 && pat.matcher("").groupCount() < minGroups)
					dispatchErrorMessage(component, "This field requires " + minGroups + " groups", "RegEx syntax error");
				else {
					dispatchErrorCleared(component);
					return pat;
				}
			} catch (PatternSyntaxException pse) {
				dispatchErrorMessage(component, pse.getLocalizedMessage(), "RegEx syntax error");
			}
		}
		return null;
	}

	public static Duration toDuration(Component component, String text) {
		if (text != null && !text.isBlank()) {
			try {
				Duration res = Duration.parse(text);
				dispatchErrorCleared(component);
				return res;
			} catch (DateTimeParseException e) {
				dispatchErrorMessage(component, e.getLocalizedMessage(), "Duration syntax error");
			}
		}
		return null;
	}

	public static DateTimeFormatter toDateTimeFormatter(Component component, String patDate) {
		if (patDate != null && !patDate.isBlank()) {
			try {
				DateTimeFormatter res = new DateTimeFormatterBuilder()
						.appendPattern(patDate)
						.toFormatter()
						.withLocale(Locale.ENGLISH)
						.withZone(ZoneId.systemDefault());
				dispatchErrorCleared(component);
				return res;
			} catch (IllegalArgumentException iae) {
				dispatchErrorMessage(component, iae.getLocalizedMessage(), "Date/time pattern error");
			}
		}
		return null;
	}
}
