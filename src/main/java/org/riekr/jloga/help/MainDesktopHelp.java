package org.riekr.jloga.help;

import static org.riekr.jloga.utils.TextUtils.describeKeyBinding;
import static org.riekr.jloga.utils.UIUtils.center;
import static org.riekr.jloga.utils.UIUtils.drawOnHover;
import static org.riekr.jloga.utils.UIUtils.getComponentHorizontalCenter;
import static org.riekr.jloga.utils.UIUtils.makeBorderless;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newButton;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.riekr.jloga.prefs.KeyBindings;
import org.riekr.jloga.prefs.LimitedList;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.utils.FileUtils;
import org.riekr.jloga.utils.UIUtils;

public class MainDesktopHelp extends JComponent {
	private static final long serialVersionUID = -1736336915951307265L;

	private static final char[] _ARROW = new char[]{'\u25B2'};

	private static Unsubscribable[] _UNSUBSCRIBABLES;

	private final JComponent[] _leftComponents;
	private final JComponent[] _rightComponents;
	private final JLabel       _keyBindings;

	private boolean _hideArrows;

	public MainDesktopHelp(JToolBar toolBar, Consumer<File> opener) {
		ArrayList<JComponent> lc = new ArrayList<>();
		ArrayList<JComponent> rc = new ArrayList<>();
		ArrayList<JComponent> arr = lc;
		for (int i = 0; i < toolBar.getComponentCount(); i++) {
			Component comp = toolBar.getComponentAtIndex(i);
			if (comp instanceof Box.Filler) {
				arr = rc;
				continue;
			}
			if (comp instanceof JComponent && ((JComponent)comp).getToolTipText() != null)
				arr.add((JComponent)comp);
		}
		_leftComponents = lc.toArray(new JComponent[0]);
		_rightComponents = rc.toArray(new JComponent[0]);

		// recent files
		setLayout(new BorderLayout());
		Box recentBox = Box.createVerticalBox();
		recentBox.add(makeBorderless(new JLabel("Recent files:")));
		recentBox.add(newButton("Clear recent files", Preferences.RECENT_FILES::reset));
		Preferences.RECENT_FILES.subscribe((files) -> {
			while (recentBox.getComponentCount() != 2)
				recentBox.remove(1);
			int i = 0;
			for (File recent : files) {
				if (!recent.canRead())
					continue;
				Box row = Box.createHorizontalBox();
				row.setAlignmentX(0);
				JButton openBtn = newBorderlessButton(
						recent.getAbsolutePath() + " (" + FileUtils.sizeToString(recent) + ')',
						() -> opener.accept(recent)
				);
				row.add(openBtn);
				row.add(drawOnHover(newBorderlessButton("\u274C", () -> {
					LimitedList<File> snap = Preferences.RECENT_FILES.get();
					snap.remove(recent);
					Preferences.RECENT_FILES.set(snap);
				}), openBtn));
				recentBox.add(row, ++i);
			}
			recentBox.setVisible(!files.isEmpty());
			recentBox.revalidate();
		});

		add(center(recentBox), BorderLayout.CENTER);
		_keyBindings = new JLabel();
		_keyBindings.setBorder(UIUtils.createEmptyBorder(16));
		add(_keyBindings, BorderLayout.SOUTH);

		if (_UNSUBSCRIBABLES != null) {
			System.err.println("DOUBLE INSTANTIATION OF MainDesktopHelp");
			for (final Unsubscribable unsubscribable : _UNSUBSCRIBABLES)
				unsubscribable.unsubscribe();
		}
		_UNSUBSCRIBABLES = KeyBindings.getGUIKeyBindings().stream()
				.map((pref) -> pref.subscribe(Observer.skip(1, (ks) -> updateKeyBindings())))
				.toArray(Unsubscribable[]::new);
		updateKeyBindings();
	}

	private void updateKeyBindings() {
		_keyBindings.setText("<html>" + String.join("<br>",
				"Main window:",
				describeKeyBinding(KeyBindings.KB_OPENFILE),
				describeKeyBinding(KeyBindings.KB_SETTINGS),
				"<br>Search panel:",
				describeKeyBinding(KeyBindings.KB_FINDTEXT),
				describeKeyBinding(KeyBindings.KB_FINDREGEX),
				describeKeyBinding(KeyBindings.KB_FINDSELECT)
		) + "</html>");
	}

	public void setHideArrows(boolean hideArrows) {
		_hideArrows = hideArrows;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (_hideArrows)
			return;
		if (g instanceof Graphics2D)
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		// impl
		int my = 4;
		int dy = 14;
		FontMetrics f = g.getFontMetrics();

		final int leftMargin = getComponentHorizontalCenter(_leftComponents[_leftComponents.length - 1]) + 32;
		for (JComponent component : _leftComponents) {
			int x = getComponentHorizontalCenter(component);
			String text = component.getToolTipText();
			char[] chars = text.toCharArray();
			int radius = leftMargin - x;
			int diameter = radius * 2;
			g.drawArc(x, -radius + dy + my, diameter, diameter, 270, -90);
			g.drawChars(chars, 0, chars.length,
					leftMargin + 6,
					dy + radius + (f.getAscent() / 2) - 1 + my
			);
			g.drawChars(_ARROW, 0, 1,
					x - (f.charWidth('\u25B2') / 2) - 1,
					g.getFont().getSize() - 4 + my
			);
		}

		final int rightStart = _rightComponents[0].getX();
		for (JComponent component : _rightComponents) {
			int x = getComponentHorizontalCenter(component);
			String text = component.getToolTipText();
			char[] chars = text.toCharArray();
			int radius = x - rightStart;
			int diameter = radius * 2;
			g.drawArc(x - diameter, -radius + dy + my, diameter, diameter, -90, 90);
			g.drawChars(chars, 0, chars.length,
					x - 6 - f.charsWidth(chars, 0, chars.length) - radius,
					dy + radius + (f.getAscent() / 2) - 1 + my
			);
			g.drawChars(_ARROW, 0, 1,
					x - (f.charWidth('\u25B2') / 2),
					g.getFont().getSize() - 4 + my
			);
		}
	}

}
