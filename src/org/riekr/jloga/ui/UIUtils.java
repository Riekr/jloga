package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class UIUtils {

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

	public static JButton newButton(String text, Runnable action) {
		JButton btn = new JButton(text);
		btn.setBorderPainted(false);
		btn.setContentAreaFilled(false);
		btn.setBorder(new EmptyBorder(6, 8, 6, 8));
		btn.addActionListener((e) -> action.run());
		return btn;
	}

	public static Component newTabHeader(String text, Runnable onClose, Runnable onSelect) {
		JLabel label = new JLabel(text);
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
//				System.out.println(e);
				if (e.getButton() == MouseEvent.BUTTON2 && e.isAltDown() && e.getClickCount() == 1)
					onClose.run();
				else
					onSelect.run();
			}
		});
		return label;
	}

	public static Pattern toPattern(Component parentComponent, String text, int minGroups) {
		if (text != null && !text.isBlank()) {
			try {
				Pattern pat = Pattern.compile(text);
				if (minGroups > 0 && pat.matcher("").groupCount() < minGroups)
					JOptionPane.showMessageDialog(parentComponent, "This field requires " + minGroups + " groups", "RegEx syntax error", JOptionPane.ERROR_MESSAGE);
				else
					return pat;
			} catch (PatternSyntaxException pse) {
				JOptionPane.showMessageDialog(parentComponent, pse.getLocalizedMessage(), "RegEx syntax error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return null;
	}

	public static DateTimeFormatter toDateTimeFormatter(Component parentComponent, String patDate) {
		if (patDate != null && !patDate.isBlank()) {
			try {
				return new DateTimeFormatterBuilder()
						.appendPattern(patDate)
//						.parseDefaulting(ChronoField.NANO_OF_DAY, 0)
						.toFormatter();
			} catch (IllegalArgumentException iae) {
				JOptionPane.showMessageDialog(parentComponent, iae.getLocalizedMessage(), "Date/time pattern error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return null;
	}
}
