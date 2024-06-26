package org.riekr.jloga.prefs;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.Main;
import org.riekr.jloga.io.LinkedProperties;
import org.riekr.jloga.ui.MenuSelectedListener;
import org.riekr.jloga.ui.TextIcon;
import org.riekr.jloga.utils.FileUtils;

public final class Favorites {
	private Favorites() {}

	private static final String _DIRECTORY = "\uD83D\uDCC1";
	private static final String _FILE      = "\uD83D\uDDB9";

	private static final ArrayList<JComponent> _COMPONENTS = new ArrayList<>();
	private static final Map<String, JMenu>    _PARENTS    = new HashMap<>();
	private static       JPopupMenu            _MENU;

	static {
		refreshMenu(Preferences.USER_FAVORITES.get());
		Preferences.USER_FAVORITES.subscribe(Favorites::refreshMenu);
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
				if (!(e instanceof FileNotFoundException))
					e.printStackTrace(System.err);
			}
		}
		return Stream.empty();
	}

	private static void refreshMenu(LinkedHashMap<?, ?> userFavorites) {
		try {
			List<JMenuItem> menuItems = new ArrayList<>();
			Stream.concat(
					getSystemFavorites(),
					userFavorites.entrySet().stream()
			).forEach(entry -> {
				Object key = entry.getKey();
				if (key != null) {
					String title = key.toString();
					Object val = entry.getValue();
					if (val != null) {
						File folder = new File(val.toString());
						if (folder.isDirectory())
							menuItems.add(scan(folder, title));
					}
				}
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
		} finally {
			_PARENTS.clear();
		}
	}

	private static JMenuItem scan(File folder) {
		return scan(folder, folder.getName());
	}

	private static JMenuItem scan(File file, String titlePath) {
		String[] path = titlePath.split("[\\\\/]");
		String title = path[path.length - 1];
		JMenuItem res;
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			JMenu menu = new JMenu(title);
			if (files != null && files.length > 0) {
				menu.addMenuListener((MenuSelectedListener)e -> {
					menu.removeAll();
					Arrays.sort(files, FileUtils::sortDirFirstIC);
					for (File child : files)
						menu.add(scan(child));
				});
			} else
				menu.add(new JMenuItem("<empty>"));
			menu.setIcon(new TextIcon(menu, _DIRECTORY));
			menu.setIconTextGap(0);
			res = menu;
		} else {
			JMenuItem container = new JMenuItem(title);
			container.setIcon(new TextIcon(container, _FILE));
			container.setIconTextGap(0);
			container.addActionListener((e) -> Main.getMain().openFile(file));
			res = container;
		}
		for (int i = path.length - 2; i >= 0; i--) {
			String parentTitle = path[i];
			JMenu parent = _PARENTS.computeIfAbsent(stream(path).limit(i).collect(joining("/")), k -> {
				JMenu newParent = new JMenu(parentTitle);
				newParent.setIcon(new TextIcon(newParent, _DIRECTORY));
				return newParent;
			});
			parent.add(res);
			res = parent;
		}
		return res;
	}

	public static void setup(JComponent component) {
		if (_MENU != null)
			component.setComponentPopupMenu(_MENU);
		component.setEnabled(_MENU != null);
		_COMPONENTS.add(component);
	}

}
