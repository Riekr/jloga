## In progress:

- External scripts:
  - Added external scripts warning/disclaimer.
  - External process variables substitution with %{var} (for every env file type).
  - External process variables by platform
  - External process variables can include system environment variables with ${var} or %VAR% notation (depending on the env file platform).
  - Fix unix home path notation "~/".
  - External process variables concatenation in path form
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