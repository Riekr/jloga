package org.riekr.jloga.help;

import javax.swing.*;
import java.awt.*;

public class MainDesktopHelp extends JComponent {

	private static final int _MARGIN = 64;
	private static final char[] _ARROW = new char[]{'\u25B2'};

	private final JToolBar _toolBar;

	public MainDesktopHelp(JToolBar toolBar) {
		_toolBar = toolBar;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D)
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		for (int i = _toolBar.getComponentCount() - 1; i >= 0; i--) {
			Component component = _toolBar.getComponentAtIndex(i);
			if (component instanceof JComponent) {
				String tooltip = ((JComponent) component).getToolTipText();
				if (tooltip != null) {
					int x = component.getX() + (component.getWidth() / 2);
					drawCallout(g, x, tooltip);
				}
			}
		}
	}

	private void drawCallout(Graphics g, int x, String text) {
		int my = 4;
		int dy = 14;
		char[] chars = text.toCharArray();
		FontMetrics f = g.getFontMetrics();
		int width = getWidth();
		if (x <= (width / 2)) {
			int radius = _MARGIN - x;
			int diameter = radius * 2;
			g.drawArc(x, -radius + dy + my, diameter, diameter, 270, -90);
			g.drawChars(chars, 0, chars.length,
					_MARGIN + 6,
					dy + radius + (f.getAscent() / 2) - 1 + my
			);
			g.drawChars(_ARROW, 0, 1,
					x - (f.charWidth('\u25B2') / 2) - 1,
					g.getFont().getSize() - 4 + my
			);
		} else {
			int radius = width - _MARGIN - x;
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
