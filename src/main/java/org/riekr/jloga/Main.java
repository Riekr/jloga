package org.riekr.jloga;

import static java.util.stream.Collectors.toList;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.riekr.jloga.httpd.FinosPerspectiveServer;
import org.riekr.jloga.ui.MainPanel;
import org.riekr.jloga.utils.TempFiles;
import org.riekr.jloga.utils.UIUtils;

public class Main {

	private static MainPanel _INSTANCE;

	public static MainPanel getMain() {return _INSTANCE;}

	@SuppressWarnings("SpellCheckingInspection")
	private static boolean loadLAF() {
		try {
			// https://www.formdev.com/flatlaf/themes/
			UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
			UIManager.put("ScrollBar.minimumThumbSize", new Dimension(8, 20));
			return true;
		} catch (Throwable ignored) {}
		try {
			UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
			return true;
		} catch (Throwable ignored) {}
		// https://stackoverflow.com/a/65805346/1326326
		return false;
	}

	public static void main(String[] vargs) {
		try {
			// check args
			ArrayList<String> args = new ArrayList<>();
			if (vargs != null)
				Collections.addAll(args, vargs);
			for (Iterator<String> i = args.iterator(); i.hasNext(); ) {
				final String arg = i.next();
				if (arg.startsWith("#")) {
					i.remove();
					continue;
				}
				switch (arg) {
					case "-perspective":
						i.remove();
						FinosPerspectiveServer.main(args.toArray(String[]::new));
						return;

					case "-remote-info":
						if (InterComm.isAlive())
							InterComm.sendInfoCommand();
						else
							System.err.println("No other instance found");
						return;

					case "-cleanup":
						if (InterComm.isAlive()) {
							System.err.println("Another instance is running, close it first.");
							return;
						}
						//noinspection fallthrough
					case "-force-cleanup":
						TempFiles.cleanup();
						return;
				}
			}

			// startup
			Consumer<List<File>> openFile;
			if (InterComm.isAlive())
				openFile = InterComm::sendFileOpenCommand;
			else {
				TempFiles.cleanup();
				newInstance();
				openFile = _INSTANCE::openFiles;
				InterComm.start();
			}

			// load files
			openFile.accept(
					args.stream().sequential()
							.map(File::new)
							.filter(File::canRead)
							.collect(toList())
			);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	private static void newInstance() {
		if (_INSTANCE == null) {

			// init themes
			boolean dark = loadLAF();

			// init ui
			_INSTANCE = new MainPanel(new Main());
			_INSTANCE.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			_INSTANCE.setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
			_INSTANCE.setExtendedState(_INSTANCE.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			_INSTANCE.setTitle("JLogA");
			try {
				UIUtils.setIcon(_INSTANCE, "icon.png", dark);
			} catch (IOException e) {
				System.err.println("Unable to set window icon!");
				e.printStackTrace(System.err);
			}
			_INSTANCE.setVisible(true);
		}
	}

}
