package org.riekr.jloga.help;

import static org.riekr.jloga.utils.MouseListenerBuilder.mouse;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.riekr.jloga.Main;
import org.riekr.jloga.ui.ROKeyListener;
import org.riekr.jloga.utils.ContextMenu;
import org.riekr.jloga.utils.Info;
import org.riekr.jloga.utils.UIUtils;

public class AboutPane extends JOptionPane {
	private static final long serialVersionUID = 5267265626558401149L;

	private static final String _HOMEPAGE = "https://github.com/Riekr/jloga";

	private static String getVersion() {
		String ver = Main.class.getPackage().getImplementationVersion();
		if (ver != null)
			return ver;
		return "<i>dev " + DateTimeFormatter.ofPattern("dd/MM/uuuu").format(LocalDate.now()) + "</i>";
	}

	private static Component compose() {
		JTabbedPane root = new JTabbedPane(JTabbedPane.LEFT);
		JLabel about = new JLabel(
				"<html>" +
						"Welcome to <b>JLogA</b> " + getVersion() + "<br><br>" +
						"<p>This program is free as in speech, without any warranty<br>" +
						"even if i tried not to hurt your hardware.</p><br>" +
						"<p>You can find source code in the below link; if you like<br>" +
						"this program please let me know.</p><br>" +
						"<p>No developers were harmed during the making of<br>" +
						"but please log consciously.<p><br>" +
						"</html>");
		about.setBorder(BorderFactory.createEmptyBorder(0, UIUtils.VSPACE, 0, 0));
		root.add("About", about);

		JTextArea info = new JTextArea(Info.get());
		info.setBorder(BorderFactory.createEmptyBorder(0, UIUtils.VSPACE, 0, 0));
		info.addKeyListener(new ROKeyListener());
		ContextMenu.addActionCopy(info);
		info.setBackground(about.getBackground());
		root.add("Info", info);
		return root;
	}

	public AboutPane() {
		super(compose(), PLAIN_MESSAGE, DEFAULT_OPTION);
		JLabel link = new JLabel("<html><a href=\"\">" + _HOMEPAGE + "</a></html>");
		link.addMouseListener(mouse().onClick((e) -> {
			Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
			if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(URI.create(_HOMEPAGE));
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
		}));
		link.addMouseListener(mouse()
				.onEnter(e -> link.setCursor(new Cursor(Cursor.HAND_CURSOR)))
				.onExit(e -> link.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)))
		);
		add(link, 1);
	}

}
