package org.riekr.jloga.help;

import javax.swing.*;

public class AboutPane extends JOptionPane {

	public AboutPane() {
		super(
				"<html>" +
						"Welcome to <b>JLogA</b> v0.0.1<br><br>" +
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
		add(new JLabel("<html><a href=\"\">https://github.com/Riekr/jloga</a></html>"), 1);
	}
}
