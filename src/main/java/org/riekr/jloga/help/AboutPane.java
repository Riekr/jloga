package org.riekr.jloga.help;

import org.riekr.jloga.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AboutPane extends JOptionPane {

	private static final String _HOMEPAGE = "https://github.com/Riekr/jloga";

	private static String getVersion() {
		String ver = Main.class.getPackage().getImplementationVersion();
		if (ver != null)
			return "v" + ver;
		return "<i>dev " + DateTimeFormatter.ofPattern("dd/MM/uuuu").format(LocalDate.now()) + "</i>";
	}

	public AboutPane() {
		super(
				"<html>" +
						"Welcome to <b>JLogA</b> " + getVersion() + "<br><br>" +
						"<p>This program is free as in speech, without any warranty<br>" +
						"even if i tried not to hurt your hardware.</p><br>" +
						"<p>You can find source code in the below link; if you like<br>" +
						"this program please let me know.</p><br>" +
						"<p>No developers were harmed during the making of<br>" +
						"but please log consciously.<p><br>" +
						"</html>",
				INFORMATION_MESSAGE,
				DEFAULT_OPTION
		);
		JLabel link = new JLabel("<html><a href=\"\">" + _HOMEPAGE + "</a></html>");
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
				if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
					try {
						desktop.browse(URI.create(_HOMEPAGE));
					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
			}
		});
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				link.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				link.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
		add(link, 1);
	}

}
