package org.riekr.jloga;

import static org.riekr.jloga.utils.AsyncOperations.asyncTask;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.riekr.jloga.httpd.FinosPerspectiveServer;
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.theme.Theme;
import org.riekr.jloga.theme.ThemePreference;
import org.riekr.jloga.ui.MainPanel;
import org.riekr.jloga.utils.Info;
import org.riekr.jloga.utils.TempFiles;
import org.riekr.jloga.utils.UIUtils;

public class Main {

	private static MainPanel _INSTANCE;

	public static MainPanel getMain() {return _INSTANCE;}

	private static void loadLAF() {
		Preferences.THEME.subscribe((theme) -> {
			if (theme.apply()) {
				if (_INSTANCE != null)
					SwingUtilities.updateComponentTreeUI(_INSTANCE);
			} else {
				Theme deflt = ThemePreference.getDefault();
				if (deflt != theme) {
					System.err.println("Failed to apply " + theme + ", switching to " + deflt);
					Preferences.THEME.set(deflt);
				}
			}
		});
		// https://stackoverflow.com/a/65805346/1326326
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

					case "-info":
						Info.writeTo(System.out);
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
			Consumer<Stream<File>> openFile;
			if (InterComm.isAlive())
				openFile = InterComm::sendFileOpenCommand;
			else {
				newInstance();
				openFile = _INSTANCE::openFiles;
				asyncTask(TempFiles::cleanup);
				asyncTask(InterComm::start);
			}

			// load files
			openFile.accept(args.stream().map(File::new));
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	private static void newInstance() {
		if (_INSTANCE == null) {
			loadLAF();
			_INSTANCE = new MainPanel();
			_INSTANCE.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			_INSTANCE.setSize(UIUtils.half(Toolkit.getDefaultToolkit().getScreenSize()));
			_INSTANCE.setExtendedState(_INSTANCE.getExtendedState() | JFrame.MAXIMIZED_BOTH);
			_INSTANCE.setTitle("JLogA");
			_INSTANCE.setVisible(true);
		}
	}

}
