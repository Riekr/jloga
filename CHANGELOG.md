## In progress:

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
