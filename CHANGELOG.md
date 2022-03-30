## In progress:

- Pick'n'mix:
  - Results prefix with file ID can be disabled
- General:
  - Moved "Cleared recent files" from settings to home panel
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
