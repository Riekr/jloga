package org.riekr.jloga.help;

import javax.swing.*;
import java.awt.*;
import java.util.stream.IntStream;

import static org.riekr.jloga.ui.UIUtils.getComponentHorizontalCenter;

public class MainDesktopHelp extends JComponent {

	private static final char[] _ARROW = new char[]{'\u25B2'};

	private final JComponent[] _components;

	public MainDesktopHelp(JToolBar toolBar) {
		_components = IntStream.range(0, toolBar.getComponentCount())
				.mapToObj(toolBar::getComponentAtIndex)
				.map((c) -> c instanceof JComponent ? (JComponent) c : null)
				.filter((c) -> c != null && c.getToolTipText() != null)
				.toArray(JComponent[]::new);
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D)
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		for (JComponent component : _components) {
			drawCallout(g,
					getComponentHorizontalCenter(component),
					component.getToolTipText()
			);
		}
	}

	private void drawCallout(Graphics g, int x, String text) {
		int my = 4;
		int dy = 14;
		char[] chars = text.toCharArray();
		FontMetrics f = g.getFontMetrics();
		int width = getWidth();
		if (x <= (width / 2)) {
			int leftMargin = getComponentHorizontalCenter(_components[_components.length - 3]) + 56;
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
		} else {
			int margin = 56;
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
