package org.riekr.jloga.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.Main;
import org.riekr.jloga.misc.DateTimeFormatterRef;
import org.riekr.jloga.react.BoolConsumer;
import org.riekr.jloga.ui.MRUComboWithLabels;

public final class UIUtils {

	public static final int VSPACE = 6;

	public static final Border FLAT_BUTTON_BORDER = new EmptyBorder(VSPACE, 8, VSPACE, 8);

	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private UIUtils() {}

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

	public static <T extends Component> T drawOnHover(T comp, Component... additionalHovers) {
		AtomicBoolean paint = new AtomicBoolean();
		Color orig = comp.getForeground();
		comp.setForeground(TRANSPARENT);
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (paint.compareAndSet(false, true))
					comp.setForeground(orig);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (paint.compareAndSet(true, false))
					comp.setForeground(TRANSPARENT);
			}
		};
		comp.addMouseListener(mouseListener);
		if (additionalHovers != null) {
			for (final Component additionalHover : additionalHovers)
				additionalHover.addMouseListener(mouseListener);
		}
		return comp;
	}

	public static JButton newBorderlessButton(String text, Runnable action, String tooltip) {
		JButton res = newBorderlessButton(text, action);
		res.setToolTipText(tooltip);
		return res;
	}

	public static JButton newBorderlessButton(String text, Runnable action) {
		return makeBorderless(newButton(text, action));
	}

	public static <T extends JComponent> T makeBorderless(T comp) {
		comp.setBorder(FLAT_BUTTON_BORDER);
		if (comp instanceof JButton) {
			JButton btn = (JButton)comp;
			btn.setBorderPainted(false);
			btn.setContentAreaFilled(false);
			btn.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {btn.setContentAreaFilled(true);}

				@Override
				public void mouseExited(MouseEvent e) {btn.setContentAreaFilled(false);}
			});
		}
		return comp;
	}

	public static JButton newButton(String text, Runnable action) {
		JButton btn = new JButton(text);
		if (action != null)
			btn.addActionListener((e) -> action.run());
		return btn;
	}

	public static JToggleButton newToggleButton(String text, String tooltip, boolean initialValue) {
		return newToggleButton(text, tooltip, initialValue, null);
	}

	public static JToggleButton newToggleButton(String text, String tooltip, boolean initialValue, BoolConsumer consumer) {
		JToggleButton btn = new JToggleButton(text);
		btn.setBorder(FLAT_BUTTON_BORDER);
		btn.setToolTipText(tooltip);
		btn.setSelected(initialValue);
		if (consumer != null)
			btn.addActionListener((e) -> consumer.accept(btn.isSelected()));
		return btn;
	}

	public static JComponent newTabHeader(String text, @Nullable Runnable onClose, @Nullable Runnable onSelect) {
		JLabel label = new JLabel(text);
		if (onClose == null) {
			if (onSelect != null) {
				label.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {onSelect.run();}
				});
			}
			return label;
		}
		// If we have an onClose let's add an X button
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2 && e.getClickCount() == 1)
					onClose.run();
				else if (onSelect != null)
					onSelect.run();
			}
		};
		label.addMouseListener(mouseListener);
		Box box = new Box(BoxLayout.LINE_AXIS);
		box.addMouseListener(mouseListener);
		box.add(label);
		box.add(Box.createHorizontalStrut(5));
		JButton xBtn = newBorderlessButton("\u274C", onClose, "Close " + text);
		xBtn.addMouseListener(mouseListener);
		box.add(xBtn);
		return box;
	}

	private static void dispatchErrorMessage(Component component, String message, String title, String value) {
		if (component instanceof MRUComboWithLabels) {
			((MRUComboWithLabels<?>)component).setError(title + ": " + message);
			((MRUComboWithLabels<?>)component).combo.markInvalidValue(value);
		} else
			JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private static void dispatchErrorCleared(Component component) {
		if (component instanceof MRUComboWithLabels)
			((MRUComboWithLabels<?>)component).setError(null);
	}

	public static Pattern toPattern(Component component, String text, int minGroups) {
		return toPattern(component, text, minGroups, 0);
	}

	public static Pattern toPattern(Component component, String text, int minGroups, int flags) {
		if (text != null && !text.isBlank()) {
			try {
				Pattern pat = Pattern.compile(text, flags);
				if (minGroups > 0 && pat.matcher("").groupCount() < minGroups)
					dispatchErrorMessage(component, "This field requires " + minGroups + " groups", "RegEx syntax error", text);
				else {
					dispatchErrorCleared(component);
					return pat;
				}
			} catch (PatternSyntaxException pse) {
				dispatchErrorMessage(component, pse.getLocalizedMessage(), "RegEx syntax error", text);
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
				dispatchErrorMessage(component, e.getLocalizedMessage(), "Duration syntax error", text);
			}
		}
		return null;
	}

	public static DateTimeFormatterRef toDateTimeFormatter(Component component, String patDate) {
		if (patDate != null && !patDate.isBlank()) {
			try {
				DateTimeFormatterRef res = DateTimeFormatterRef.ofPattern(patDate);
				dispatchErrorCleared(component);
				return res;
			} catch (IllegalArgumentException iae) {
				dispatchErrorMessage(component, iae.getLocalizedMessage(), "Date/time pattern error", patDate);
			}
		}
		return null;
	}

	public static int getComponentHorizontalCenter(Component component) {
		return component.getX() + (component.getWidth() / 2);
	}

	public static void setIcon(@NotNull JFrame frame, @NotNull String name, boolean invert) throws IOException {
		URL resource = Main.class.getResource(name);
		if (resource == null)
			return;
		BufferedImage image = ImageIO.read(resource);
		if (invert) {
			final int w = image.getWidth(), h = image.getHeight();
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					int rgba = image.getRGB(x, y);
					rgba ^= 0x00FFFFFF;
					image.setRGB(x, y, rgba);
				}
			}
		}
		frame.setIconImages(Arrays.asList(
				image.getScaledInstance(32, 32, Image.SCALE_SMOOTH),
				image.getScaledInstance(64, 64, Image.SCALE_SMOOTH)
		));
	}

	public static void setFileDropListener(@NotNull Component component, Consumer<List<File>> consumer) {
		try {
			DropTarget dropTarget = new DropTarget();
			dropTarget.addDropTargetListener(new DropTargetAdapter() {
				@SuppressWarnings("unchecked")
				@Override
				public void drop(DropTargetDropEvent dtde) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					try {
						Object data = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if (data instanceof List) {
							consumer.accept((List<File>)data);
						}
					} catch (UnsupportedFlavorException | IOException | ClassCastException e) {
						e.printStackTrace(System.err);
					}
				}
			});
			component.setDropTarget(dropTarget);
		} catch (TooManyListenersException e) {
			e.printStackTrace(System.err);
		}
	}

	public static JRadioButton newRadioButton(ButtonGroup group, String text, String tooltip, Runnable onSelection, boolean selected) {
		JRadioButton radioButton = new JRadioButton(text);
		radioButton.setSelected(selected);
		radioButton.setToolTipText(tooltip);
		radioButton.addActionListener((e) -> onSelection.run());
		group.add(radioButton);
		return radioButton;
	}

	public static Stream<Component> allComponents(Container root) {
		return Stream.concat(Stream.of(root), findComponents(root));
	}

	public static Stream<Component> findComponents(Container container) {
		Component[] components = container.getComponents();
		return Stream.concat(
				Arrays.stream(components),
				Arrays.stream(components)
						.filter(Container.class::isInstance)
						.flatMap(c -> findComponents((Container)c))
		);
	}

	public static Component center(Component component) {
		Box horiz = Box.createHorizontalBox();
		Box vert = Box.createVerticalBox();
		horiz.add(Box.createHorizontalGlue());
		horiz.add(vert);
		horiz.add(Box.createHorizontalGlue());
		vert.add(Box.createVerticalGlue());
		vert.add(component);
		vert.add(Box.createVerticalGlue());
		return horiz;
	}

	public static Component atStart(Component component) {
		Box horiz = Box.createHorizontalBox();
		horiz.add(component);
		horiz.add(Box.createHorizontalGlue());
		return horiz;
	}

	public static Component atEnd(Component component) {
		Box horiz = Box.createHorizontalBox();
		horiz.add(Box.createHorizontalGlue());
		horiz.add(component);
		return horiz;
	}

	public static Box horizontalBox(Component... components) {
		Box horiz = Box.createHorizontalBox();
		for (Component c : components)
			horiz.add(c);
		return horiz;
	}

	public static Border createEmptyBorder(int size) {
		return BorderFactory.createEmptyBorder(size, size, size, size);
	}

	public static MouseListener onClickListener(Runnable runnable) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 1) {
					runnable.run();
					e.consume();
				}
			}
		};
	}

	public static void relativeLocation(Component currComponent, Component parent, Point currComponentLocation) {
		currComponentLocation.x = 0;
		currComponentLocation.y = 0;
		while (currComponent != null && currComponent != parent) {
			Point relativeLocation = currComponent.getLocation();
			currComponentLocation.translate(relativeLocation.x, relativeLocation.y);
			currComponent = currComponent.getParent();
		}
	}

	public static void showComponentMenu(JComponent component) {
		component.getComponentPopupMenu().show(component, 0, component.getHeight());
	}
}
