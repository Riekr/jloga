package org.riekr.jloga.prefs;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.riekr.jloga.Main;
import org.riekr.jloga.io.LinkedProperties;
import org.riekr.jloga.ui.MenuSelectedListener;
import org.riekr.jloga.ui.TextIcon;

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
					LinkedProperties props = new LinkedProperties();
					props.load(reader);
					for (Map.Entry<Object, Object> entry : props.linkedEntrySet()) {
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
				JMenu menu = new JMenu(title);
				if (files != null && files.length > 0) {
					menu.addMenuListener((MenuSelectedListener)e -> {
						menu.removeAll();
						for (File child : files)
							menu.add(scan(child));
					});
				} else
					menu.add(new JMenuItem("<empty>"));
				menu.setIcon(new TextIcon(menu, "\uD83D\uDCC1"));
				menu.setIconTextGap(0);
				return menu;
			} else {
				JMenuItem container = new JMenuItem(title);
				container.setIcon(new TextIcon(container, "\uD83D\uDDB9"));
				container.setIconTextGap(0);
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
