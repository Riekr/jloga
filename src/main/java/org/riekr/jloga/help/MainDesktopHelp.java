package org.riekr.jloga.help;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static org.riekr.jloga.ui.utils.UIUtils.getComponentHorizontalCenter;

public class MainDesktopHelp extends JComponent {

	private static final char[] _ARROW = new char[]{'\u25B2'};

	private final JComponent[] _leftComponents;
	private final JComponent[] _rightComponents;

	public MainDesktopHelp(JToolBar toolBar) {
		ArrayList<JComponent> lc = new ArrayList<>();
		ArrayList<JComponent> rc = new ArrayList<>();
		ArrayList<JComponent> arr = lc;
		for (int i = 0; i < toolBar.getComponentCount(); i++) {
			Component comp = toolBar.getComponentAtIndex(i);
			if (comp instanceof Box.Filler) {
				arr = rc;
				continue;
			}
			if (comp instanceof JComponent && ((JComponent) comp).getToolTipText() != null)
				arr.add((JComponent) comp);
		}
		_leftComponents = lc.toArray(new JComponent[0]);
		_rightComponents = rc.toArray(new JComponent[0]);
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D)
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		// impl
		int my = 4;
		int dy = 14;
		FontMetrics f = g.getFontMetrics();
		int width = getWidth();

		int leftMargin = getComponentHorizontalCenter(_leftComponents[_leftComponents.length - 1]) + 32;
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

		for (JComponent component : _rightComponents) {
			int x = getComponentHorizontalCenter(component);
			String text = component.getToolTipText();
			char[] chars = text.toCharArray();
			int margin = 32;
			int radius = width - margin - x;
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
