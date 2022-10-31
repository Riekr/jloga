package org.riekr.jloga.prefs;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.io.LinkedProperties;
import org.riekr.jloga.ui.MenuSelectedListener;
import org.riekr.jloga.ui.TextIcon;

public final class Favorites {
	private Favorites() {}

	private static final ArrayList<JComponent> _COMPONENTS = new ArrayList<>();
	private static       JPopupMenu            _MENU;

	static {
		refreshMenu();
		Preferences.USER_FAVORITES.subscribe((data) -> refreshMenu());
	}

	@NotNull
	private static Stream<Map.Entry<Object, Object>> getSystemFavorites() {
		String favoritesFileName = System.getProperty("jloga.favorites");
		if (favoritesFileName != null && !(favoritesFileName = favoritesFileName.trim()).isEmpty()) {
			try (FileReader reader = new FileReader(favoritesFileName)) {
				LinkedProperties props = new LinkedProperties();
				props.load(reader);
				return props.entrySet().stream();
			} catch (Throwable e) {
				System.err.println("Unable to load system favorites from: " + favoritesFileName);
				e.printStackTrace(System.err);
			}
		}
		return Stream.empty();
	}

	private static void refreshMenu() {
		List<JMenuItem> menuItems = new ArrayList<>();
		Stream.concat(
				getSystemFavorites(),
				Preferences.USER_FAVORITES.get().entrySet().stream()
		).forEach(entry -> {
			String title = entry.getKey().toString();
			File folder = new File(entry.getValue().toString());
			if (folder.isDirectory())
				menuItems.add(scan(folder, title));
		});
		if (!menuItems.isEmpty()) {
			JPopupMenu menu = new JPopupMenu();
			menuItems.forEach(menu::add);
			_MENU = menu;
			_COMPONENTS.forEach(comp -> {
				comp.setComponentPopupMenu(_MENU);
				comp.setEnabled(true);
			});
		}
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

	public static void setup(JComponent component) {
		if (_MENU != null)
			component.setComponentPopupMenu(_MENU);
		component.setEnabled(_MENU != null);
		_COMPONENTS.add(component);
	}

}
