package org.riekr.jloga.help;

import static org.riekr.jloga.utils.TextUtils.describeKeyBinding;
import static org.riekr.jloga.utils.UIUtils.center;
import static org.riekr.jloga.utils.UIUtils.drawOnHover;
import static org.riekr.jloga.utils.UIUtils.getComponentHorizontalCenter;
import static org.riekr.jloga.utils.UIUtils.makeBorderless;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newButton;
import static org.riekr.jloga.utils.UIUtils.relativeLocation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
	private final Box          _recentBox;

	private boolean _arrowsHidden    = false;
	private boolean _arrowsUncovered = true;

	private final Point _recentBoxLoc = new Point();
	private       int   _leftArrowsX, _rightArrowsX;

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
		_recentBox = Box.createVerticalBox();
		_recentBox.add(makeBorderless(new JLabel("Recent files:")));
		_recentBox.add(newButton("Clear recent files", Preferences.RECENT_FILES::reset));
		Preferences.RECENT_FILES.subscribe((files) -> {
			while (_recentBox.getComponentCount() != 2)
				_recentBox.remove(1);
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
				_recentBox.add(row, ++i);
			}
			_recentBox.setVisible(!files.isEmpty());
			_recentBox.revalidate();
		});

		add(center(_recentBox), BorderLayout.CENTER);
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
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				onResize();
			}
		});
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

	public void setArrowsHidden(boolean arrowsHidden) {
		_arrowsHidden = arrowsHidden;
	}

	private void onResize() {
		relativeLocation(_recentBox, this, _recentBoxLoc);
		boolean leftArrows = _leftArrowsX < _recentBoxLoc.x;
		// System.out.println("L " + leftArrows + " = " + _leftArrowsX + " < " + _recentBoxLoc.x);
		boolean rightArrows = _rightArrowsX > _recentBoxLoc.x + _recentBox.getWidth();
		// System.out.println("R " + rightArrows + " = " + _rightArrowsX + " > " + _recentBoxLoc.x + " + " + _recentBox.getWidth());
		_arrowsUncovered = leftArrows && rightArrows;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (_arrowsHidden)
			return;
		if (g instanceof Graphics2D)
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.GRAY);
		// impl
		int my = 4;
		int dy = 14;
		FontMetrics f = g.getFontMetrics();

		final int leftMargin = getComponentHorizontalCenter(_leftComponents[_leftComponents.length - 1]) + 32;
		int leftArrowsX = Integer.MIN_VALUE;
		for (JComponent component : _leftComponents) {
			char[] chars = component.getToolTipText().toCharArray();
			int charsX = leftMargin + 6;
			int charsEnd = charsX + f.charsWidth(chars, 0, chars.length);
			if (charsEnd > leftArrowsX)
				leftArrowsX = charsEnd;
			if (_arrowsUncovered) {
				int x = getComponentHorizontalCenter(component);
				int radius = leftMargin - x;
				int diameter = radius * 2;
				g.drawArc(x, -radius + dy + my, diameter, diameter, 270, -90);
				g.drawChars(chars, 0, chars.length,
						charsX,
						dy + radius + (f.getAscent() / 2) - 1 + my
				);
				g.drawChars(_ARROW, 0, 1,
						x - (f.charWidth('\u25B2') / 2) - 1,
						g.getFont().getSize() - 4 + my
				);
			}
		}
		_leftArrowsX = leftArrowsX;

		final int rightStart = _rightComponents[0].getX();
		int rightArrowsX = Integer.MAX_VALUE;
		for (JComponent component : _rightComponents) {
			int x = getComponentHorizontalCenter(component);
			char[] chars = component.getToolTipText().toCharArray();
			int radius = x - rightStart;
			int charsX = x - 6 - f.charsWidth(chars, 0, chars.length) - radius;
			if (_arrowsUncovered) {
				int diameter = radius * 2;
				g.drawArc(x - diameter, -radius + dy + my, diameter, diameter, -90, 90);
				g.drawChars(chars, 0, chars.length,
						charsX,
						dy + radius + (f.getAscent() / 2) - 1 + my
				);
				g.drawChars(_ARROW, 0, 1,
						x - (f.charWidth('\u25B2') / 2),
						g.getFont().getSize() - 4 + my
				);
			}
			if (charsX < rightArrowsX)
				rightArrowsX = charsX;
		}
		_rightArrowsX = rightArrowsX;
	}

}
