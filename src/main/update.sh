#!/usr/bin/env bash
wget "https://cdn.jsdelivr.net/npm/@finos/perspective" -O html/org/riekr/jloga/http/perspective/perspective.js
wget "https://cdn.jsdelivr.net/npm/@finos/perspective-viewer" -O html/org/riekr/jloga/http/perspective/perspective-viewer.js
wget "https://cdn.jsdelivr.net/npm/@finos/perspective-viewer-datagrid" -O html/org/riekr/jloga/http/perspective/perspective-viewer-datagrid.js
wget "https://cdn.jsdelivr.net/npm/@finos/perspective-viewer-d3fc" -O html/org/riekr/jloga/http/perspective/perspective-viewer-d3fc.js

wget --page-requisites --convert-links -e robots=off \
     --span-hosts --restrict-file-names=windows \
     --directory-prefix=html/org/riekr/jloga/http/perspective/res --adjust-extension \
     "https://cdn.jsdelivr.net/npm/@finos/perspective-viewer/dist/css/themes.css"
