# jLoga, a log analyzer written in Java

jLoga is a log analyzer, optimized for performance, written in [java 11](https://openjdk.java.net/projects/jdk/11/) and capable to open huge files.
The UI is designed to for heavy log analysis and to show multiple files and multiple search results.

Files and results can be view in grid mode and inspected with [finos perspective](https://perspective.finos.org/), a standalone perspective viewer launcher is also provided.

The project started in my spare time to simplify crash analisys.

## Features
- Hierarchical panel structure to ease files and results navigation
- [No size limit](#file-limits) on open files, each file is indexed and paged from disk. The only limit is the memory of your computer
- Fast and multithread simple searches like text and regex, each can be case-sensitive or not, inclusive or exclusive
- More advanced analysis such as frequency and function duration reports
- Autodetect most common log lines formats for timestamp extraction
- Save and reload search parameters
- Capable of running [external commands](#external-commands) as new analyzers
- Convert and show data into [finos perspective](#finos-perspective)

## File limits
The only limit is the amout of memory available to jloga: when a file is opened it is indexed using the specified charset then only a line number and a position on the disk is kept in memory once in a while.\
Past pages of the files are kept in memory until garbage collection starts in order to reduce disk i/o activity, due to the nature of the application, the best performance is achieved when reading files from a ssd.

## External commands
You can create a set of files with extension `.jloga.json` inside a dedicated folder and then have those commands be executed inside jloga as custom analyzers.\
Each command must be able to parse data from stdin and produce output to stdout, each line emitted from the command is associated to the last line parsed from the source.\
See the folder `ext-search-samples` for some instruction.

## Finos perspective
I've fallen in love with [Data Preview](https://marketplace.visualstudio.com/items?itemName=RandomFractalsInc.vscode-data-preview) for Visual Studio Code then discovered [finos perspective](https://perspective.finos.org/). \
Perspective is a powerful data analysis tool (check their site) and you can now apply that analysis on your search results. Input data is automatically translated and adapted into a compatible format, perspective is then opened in a standalone browser.\
Unfortunately firefox has problems with perspective and even javaFX browser does not support webassembly and workers, you will have to use a [chromium based browser](https://en.wikipedia.org/wiki/Chromium_(web_browser)#Browsers_based_on_Chromium).

### Standalone viewer
You can launch a standalone perspective viewer with:
```shell
java -cp build/libs/jloga-all.jar org.riekr.jloga.httpd.FinosPerspectiveServer input_data.csv
```
Once you close the browser, the app will automatically close.

## Future and licensing
This software is free as in speech, do whatever you want with it.

As a spare time project this source base is updated only when needed, don't expect fast response times to bug/issues or requests. I'll do my best anyway.

### Used libraries includes:
- FlatLaf - Flat Look and Feel ([home](https://www.formdev.com/flatlaf/) - [license](https://github.com/JFormDesigner/FlatLaf/blob/main/LICENSE))
- fontchooser ([home](https://gitlab.com/dheid/fontchooser) - [license](https://gitlab.com/dheid/fontchooser/-/blob/master/LICENSE))
- NanoHTTPD – a tiny web server in Java ([home](https://github.com/NanoHttpd/nanohttpd) - [license](https://github.com/NanoHttpd/nanohttpd/blob/master/LICENSE.md))
- Gson ([home](https://github.com/google/gson) - [license](https://github.com/google/gson/blob/master/LICENSE))
- Launch4J - Cross-platform Java executable wrapper ([license](https://sourceforge.net/p/launch4j/git/ci/master/tree/LICENSE.txt) - [home](http://launch4j.sourceforge.net/) - [gradle plugin](https://github.com/TheBoegl/gradle-launch4j))
- [IntelliJ Idea Community Edition](https://www.jetbrains.com/idea/)
