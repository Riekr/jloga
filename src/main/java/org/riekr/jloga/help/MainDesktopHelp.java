package org.riekr.jloga.help;

import static org.riekr.jloga.ui.utils.UIUtils.getComponentHorizontalCenter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.ui.utils.UIUtils;

public class MainDesktopHelp extends JComponent {
	private static final long serialVersionUID = -1736336915951307265L;

	private static final char[] _ARROW = new char[]{'\u25B2'};

	private final JComponent[] _leftComponents;
	private final JComponent[] _rightComponents;

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
		setLayout(new GridBagLayout());
		Box recentBox = Box.createVerticalBox();
		JLabel title = new JLabel("Recent files:");
		title.setBorder(UIUtils.FLAT_BUTTON_BORDER);
		recentBox.add(title);
		Preferences.RECENT_FILES.subscribe((files) -> {
			while (recentBox.getComponentCount() != 1)
				recentBox.remove(1);
			for (File recent : files)
				recentBox.add(UIUtils.newBorderlessButton(recent.getAbsolutePath(), () -> opener.accept(recent)));
		});
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = gbc.weighty = 0.5;
		add(recentBox, gbc);
	}

	@Override
	protected void paintComponent(Graphics g) {
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
