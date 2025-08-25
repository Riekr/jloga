- Fixes
  - Grid view takes into account header sizes when sizing columns
  - Fixed grid view copy to clipboard context menu actions
  - Fixed value not being updated in some case in MRU text combos
- General:
  - Cobol open file dialog supports dropping files
  - Cobol view subheaders context menus and size tooltip
  - Text view subheaders shows file name in context menu

## v0.6.1
- Fixes
  - Cobol datafiles are opened in grid view (if enabled)
  - Cobol datafile title is now the file name only
  - Avoid flickering while scrolling grid views
  - Horizontal scroll position is not resetted after vertical scroll
- General:
  - Cobol datafile async (re)loading and progress bar
  - Decoded cobol datafiles are paged to disk: no more file size limit.
  - Added tooltips to open cobol dialog

## v0.6.0
- **New features**:
  - Cobol copybooks and datafile opening using [jRecord](https://github.com/bmTas/JRecord)
- General:
  - Updated to Finos perspective [3.7.4](https://github.com/finos/perspective/compare/v3.5.1...v3.7.4)
  - Updated to flatlaf [3.6.1](https://github.com/JFormDesigner/FlatLaf/releases/tag/3.6.1)
  - Updated to launch4j [4.0.0](https://github.com/TheBoegl/gradle-launch4j/blob/develop/VERSION.md#version-400)
  - Updated undercouch download gradle plugin to [5.6.0](https://github.com/michel-kraemer/gradle-download-task/releases/tag/5.6.0)
  - Updated to gson [2.13.1](https://github.com/google/gson/releases/tag/gson-parent-2.13.1)
  - Updated jetbrains annotations to [26.0.2](https://github.com/JetBrains/java-annotations/compare/24.1.0...26.0.2)

## v0.5.2
- General:
  - Minimum java version is now 21!
- UI:
  - When opening file a new search tab is no longer opened (configurable in preferences)

## v0.5.1
- General:
  - Updated to Finos perspective [3.5.1](https://github.com/finos/perspective/compare/v3.1.3...v3.5.1)
  - Updated to flatlaf 3.6
  - Updated to gson 2.13.0
- Build:
  - Specify perspective version
  - Removed perspective jsmap files

## v0.5.0
- External scripts:
  - System property `jloga.ext.dir` works as addition to other sources
  - If `jloga.scripts` exists in classpath, it is loaded as a list of search definitions:
    - each lines represents a file name to be loaded
    - lines can start with `res://` to load the file from classpath
    - if starting with `file://` or no _schema_ prefix is specified, file will be loaded from local file system
  - _Extension scripts folder_ preference chage is honored if other sources are active 
  - Removed _Skip "EXT:" prefix_ preference to avoid confusion

---
## v0.4.2
- General:
  - Updated to perspective [3.1.3](https://github.com/finos/perspective/compare/v2.7.1...v3.1.3)
  - Updated to flatlaf 3.4.1
  - Updated to toast-notifications 1.0.3
  - Updated to fontchooser 3.1.0

## v0.4.1
- General:
  - Better detection of other instances    
  - Updated to perspective 2.10.1
- External scripts:
  - Environment variables can be set from preferences panel

## v0.4.0
- UI:
  - Support for [toast message notifications](https://github.com/DJ-Raven/swing-toast-notifications)
- Text Viewer:
  - Fix line highlighting if not whole hierarchy is selected in settings
- I/O:
  - Autoscale page size to fit text file line length
- General:
  - Better detection of other instances    
  - Updated to flatlaf 3.4
  - Increase default page size to 2MB
  - Updated to perspective 2.8.1

## v0.3.4
- General:
  - Updated to perspective [2.7.1](https://github.com/finos/perspective/compare/v2.6.1...v2.7.1)
  - Minor performance fixes to perspective data loading
  - Fix for text load issue

## v0.3.3
- General:
  - Updated to perspective 2.6.1
  - Updated to flatlaf 3.2.5
  - Updated to gson 2.10.1
  - Minor performance fixes to perspective data loading

## v0.3.2
- General:
  - Fixed save file regression
  - Updated to perspective 2.2.1
  - Better handling of perspective arrow conversions
  - Asynchronous operations errors dump

## v0.3.1
- Text Viewer:
  - correctly show last line in viewport
  - correctly recalculate lines in viewport after line height change
  - Keep selected text when scrolling
- UI:
  - "Open in file manager" actions on right click menues
  - Fixed GTK theme selection
- General:
  - Fixed I/O threading regression under heavy load
  - PickNMix dates combo works again

## v0.3.0
- Favorites:
  - Sorting (case-insensitive and directories first)
- Preferences:
  - Lazy tab initialization to avoid lag
  - Fixed sizing as in previous versions
  - Added text viewer line height customization
- General:
  - Upgraded build libraries and laf themes
  - Added non latin text to font dialog preview
  - Updated to perspective 1.9.3
  - Changed I/O threading to allow scrolling text while searching
  - Show popup in case of invalid file on command line
- Grids:
  - Changing number of regex groups correctly updates table view columns
  - Right click on grid button to change header or delimiter
- Bug Fixes:
  - Line highlighting determined by caret position

## v0.2.9
- Favorites:
  - Correct inline editing
  - Right click on "Favorites" button to edit them
  - Subfolders in aliases (use "/" or "\" to split as a path)
  - Reordering

## v0.2.8
- General:
  - Favorites are ordered as in file
  - UI for configuring favorites

## v0.2.7
- General:
  - Added icons in favorites views
- Build:
    - Fixed double _v_ in version

## v0.2.6
- General:
  - Preliminary support for favorites folders\
  specify `-Djloga.favorites=favorites.properties` to enable the menu, check sample [file](favorites-sample.properties)
- Build:
  - Get version from _CHANGELOG.md_
  - Upgrade to gradle 7.5.1 and latest wrapper

## v0.2.5
- General:
  - Updated FlatLaf themes and font chooser
  - Updated to perspective 1.7.1
- External scripts:
  - System property `-Djloga.env.override=true` will let os environment variables override the ones defined in _env*.jloga.properties_, this is useful if launching jloga from a workspace.
- Build:
  - Upgrade to gradle 7.4.2 to support upcoming java versions

## v0.2.4
- General:
  - Native file dialogs (awt)
  - Project panel now opens with a click as default, you can roll back to hovering in settings.
  - Project pane can be dragged around and better description
- Grids:
  - Grid view can be forced if header detection result is uncertain
  - Grid columns automatic initial widths
  - Grid view font is the same as in text viewer
- External scripts:
  - Support for search output sections (specify a `sectionRegex` in *.jloga.json* file)
- Bug fixes:
  - Unselectable values in project combos

## v0.2.3
- General:
  - Updated to perspective 1.3.13
  - Info in about pane (and "-info" cli switch)
  - Minor optimizations
- External scripts:
  - External scripts are ordered by filename if it starts with a number
    and no order is specified inside the *.jloga.json* config file.
  - Variables are now supported in combo values
- Bug fixes:
  - Fixed duplicate first line in text views
  - Search while indexing does not wait for finish
  - Now indexing is done in a dedicated fs thread with low priority

## v0.2.2
- General
  - Theme selection and many themes available
  - Updated to perspective 1.3.12
- Bug fixes:
  - Selection highlight persists on viewport change
  - Correct parent line highlighted
  - More header detection
  - Rollback to flatlaf 2.1 due to JSplitPane regression
  - Desktop help callouts auto hide

## v0.2.1
- General
  - Desktop help auto hide
  - Clear highlight by pressing ESC
  - Serch prefill when pressing ctrl+f or ctrl+r
  - Updated to perspective 1.3.11
  - "Scroll beyond end-of-text" can be changed in preferences
  - Updated FlatLaf theme
- Bug fixes:
  - Header detection fixes
  - Fixed selection beyond end-of-text

## v0.2.0
- General
  - Updated to perspective 1.3.10
  - Project combos does not update values
- Bug fixes:
  - Solved failures if some recent file is missing
  - Close sources on search rerun or tab close
  - Header detection fixes
  - Missing buttons if search pattern is too long
  - Non-editable project combos not being saved

## v0.1.4
- General:
  - Check file creation time and fingerprint when before reloading
  - File size in tooltip and recent files
  - Updated perspective resources
- Bug fixes:
  - Fixed opening multiple files
    
## v0.1.3
- General:
  - Hierarchical line highlighting (enable it in settings)
  - Search panel tabs does not grow anymore
  - ~30% more speed in page loading
  - Updated to perspective 1.3.8
- Bug fixes:
  - Temp files cleanup\
   **IMPORTANT:** altough operating systems should automatically delete temp files this was not the intended behaviour and now temp files\
   are deleted on jloga shutdown and startup (in case o crash).\
   If you suspect low disk space after using jloga check your temp directory for files starting with "jloga" and delete them.

## v0.1.2
- External scripts:
  - Line matched via named regex like: `^(?<file>[^:]*):(?<line>\d*):(?<text>.*)`
- Bug fixes:
  - Text not loading while searching
  - Index out of bounds while scrolling on script results
 
## v0.1.1
- General:
  - Improved caching
  - Line numbers starts from 1 instead of zero, you can revert to old behaviour in preferences.
- Bug fixes:
  - Scrolling during indexing does not lag anymore
  - Line numbers not correctly displaying while indexing
  - `.exe` bundle dependencies are included

## v0.1.0
- General:
  - Same instance is reused if possible
  - Added refresh button in main toolbar (or use F5)
  - Press CTRL+T to add new search tab
- Bug fixes:
  - Fixed unwanted empty lines in combos
  - Fixed text selection in search results
  - Fixed text highlight disappearing in searches

## v0.0.13
- General:
  - Recent folders in open file dialog
  - Text viewport overscroll
  - Negate and case buttons now changes state on selection and not on search
- Bug fixes:
  - Fix recent files not being updated
  - Change last open path for each file opened and not only for "file open dialog"
  - Fixed line count again to match with "java.io.BufferedReader.readLine()" behaviour
- Experimental
  - Reload of current open file by pressing "F5" (search results are not refreshed)

## v0.0.12b
- General:
  - One I/O thread for each file root
- Bug fixes:
  - Fixed sporadic freezing during text load
  - Fixed missing lines in displayed text (searches were not affected)

## v0.0.12a
- General:
  - Search button in text and regex searches (you can still press "enter")
  - Cosmetic cleanup of projects (ext, duration, etc..)
  - New preference about creating tabs with key shortcuts
  - More keyboard shortcuts
- External scripts:
  - Script definitions supports `values` shown in a combo or checkbox ([sample](ext-search-samples/cyggrep.jloga.json))
- Bug fixes:
  - Toggle buttons in text and regex
  - Fix for "Key too long" error
  - Fixed cr/lf sequence in indexing

## v0.0.11
- Pick'n'mix:
  - Results prefix with file ID can be disabled
- General:
  - Moved "Cleared recent files" from settings to home panel
  - Right click on search selection button to open context menu instead of popup
- Bug fixes:
  - Fixed missing last line in files not ending with a new-line
  - Fixed disappeared ext process start button
  - Fixed progress bars sometime non appearing (ex in pick'n'mix)

## v0.0.10
- General
  - New tab button in main panel allows reopening recent files
- Analysis:
  - Uniq search timestamp extraction
- External scripts:
  - Support for editing parameters from ui
- Bug fixes:
  - RegEx searches with extraction does not corrupt grid data anymore
  - Date patterns detection wizards correctly gets saved in project combos
  - Fix for empty paged lists and ext processes suddenly closing

## v0.0.9
- Bug fixes:
  - Analysis after 2nd level does not hang
  - Stopping searches does not leave progressbar dirty anymore
  - Ext analysis now correctly stops in case of errors
- General:
  - Key bindings
  - Limited charset autodetection

## v0.0.8
- General:
  - Searches can finally run while indexing files
  - Invalid reflective access warning fix
- External scripts:
  - Added external scripts warning/disclaimer.
  - External process variables substitution with %{var} (for every env file type).
  - External process variables by platform
  - External process variables can include system environment variables with ${var} or %VAR% notation (depending on the env file platform).
  - Fix unix home path notation "~/".
  - External process variables concatenation in path form
  - Allow dot in external scripts custom properties (still forbidden for env variables)
- Grids:
  - Fix single line ext process output in grid mode
  - Fix grid view in sub searches
  - Better header detection and handling
- Preferences:
  - Dependencies between preferences
  - Keep last selected tab visible on settings reopen
  - Browser selection user preference
  - Right click to reset preference

## v0.0.7
- General:
  - Recent files
  - Flat buttons hover
- Grids:
  - Automatic grid view for regex searches with groups
  - Fixed regex search with multiple groups and added header if more than 1 capturing group (for basic perspective data extraction)
- Preferences:
  - Tabbed preferences
  - Date parsing locale preference
- External scripts:
  - External process message box can be closed by pressing ESC key
  - Fixed external process search progress not updating
