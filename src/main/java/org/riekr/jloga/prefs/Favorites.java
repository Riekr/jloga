package org.riekr.jloga.prefs;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.riekr.jloga.Main;
import org.riekr.jloga.ui.MenuSelectedListener;

public final class Favorites {
	private Favorites() {}

	private static final class Lazy {
		private Lazy() {}

		public static final JPopupMenu MENU;

		static {
			String favoritesFileName = System.getProperty("jloga.favorites");
			JPopupMenu menu = null;
			if (favoritesFileName != null && !(favoritesFileName = favoritesFileName.trim()).isEmpty()) {
				List<JMenuItem> menuItems = new ArrayList<>();
				try (FileReader reader = new FileReader(favoritesFileName)) {
					Properties props = new Properties();
					props.load(reader);
					for (Map.Entry<Object, Object> entry : props.entrySet()) {
						String title = entry.getKey().toString();
						File folder = new File(entry.getValue().toString());
						if (folder.isDirectory())
							menuItems.add(scan(folder, title));
					}
					if (!menuItems.isEmpty()) {
						menu = new JPopupMenu();
						menuItems.forEach(menu::add);
					}
				} catch (Throwable e) {
					System.err.println("Unable to load favorites from: " + favoritesFileName);
					e.printStackTrace(System.err);
					menu = null;
				}
			}
			MENU = menu;
		}

		private static JMenuItem scan(File folder) {
			return scan(folder, folder.getName());
		}

		private static JMenuItem scan(File file, String title) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				if (files != null && files.length > 0) {
					JMenu container = new JMenu(title);
					container.addMenuListener((MenuSelectedListener)e -> {
						container.removeAll();
						for (File child : files)
							container.add(scan(child));
					});
					return container;
				}
				return new JMenuItem(title);
			} else {
				JMenuItem container = new JMenuItem(title);
				container.addActionListener((e) -> Main.getMain().openFile(file));
				return container;
			}
		}
	}

	public static boolean hasFavorites() {
		return Lazy.MENU != null;
	}

	public static void setup(JComponent component) {
		if (hasFavorites())
			component.setComponentPopupMenu(Lazy.MENU);
	}

}
