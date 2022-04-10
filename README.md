# jLoga, a log analyzer written in Java

jLoga is a log analyzer, optimized for performance, written in [java 11](https://openjdk.java.net/projects/jdk/11/) and capable to open huge files.
The UI is designed to for heavy log analysis and to show multiple files and multiple search results, inspired by [glogg](https://glogg.bonnefon.org/) a great native and multiplatform log analyzer. 

Files and results can be view in grid mode and inspected with [finos perspective](https://perspective.finos.org/), a standalone perspective viewer launcher is also provided.

The project started in my spare time to simplify crash analisys.

## Downloads and installation
For each jloga variant Java 11 or later is required, you can download OpenJDK jre available from [Adoptium](https://adoptium.net/releases.html?variant=openjdk11&jvmVariant=hotspot), search it from [Oracle's java.com](https://java.com/it/download/) or use the one bundled with your operating system if any.

The `.exe` file is an all-in-one executable file built with Launch4J for windows in order to simplify file associations. You don't need to extract it, just double click to run. The app uses the standard java preferences to store the user selections.

For other platforms you can download the `.jar` file and execute it with `java -jar jloga-all.jar`, all dependencies are included.

Head directly to [this page](https://github.com/Riekr/jloga/releases/latest) for downloads.

## Features
- Hierarchical panel structure to ease files and results navigation
- [No size limit](#file-limits) on open files, each file is indexed and paged from disk. The only limit is the memory of your computer
- Fast and multithread simple searches like text and regex, each can be case-sensitive or not, inclusive or exclusive
- More advanced analysis such as frequency and function duration reports
- Autodetect most common log lines formats for timestamp extraction
- Save and reload search parameters
- Capable of running [external commands](#external-commands) as new analyzers
- Convert and show data into [finos perspective](#finos-perspective)

## File and memory limits
There is no limit in file size, maximum number of lines is 2<sup>63</sup>-1.\
Each open file is indexed keeping a reference to the position on disk each (for example) 1MB, so the heap impact is minimum even for very big files.\
Past pages of the files are kept in memory until garbage collection starts in order to reduce disk i/o activity, due to the nature of the application, the best performance is achieved when reading files from a ssd.

jLoga can open many 4GB log files in few senconds and with default jvm settings, you can increase heap if you have a slow disk in order to explicitly cache text pages and reduce disk accesses.

You can check current memory usage and limits of a running instance by running the command line in this example:
```
# java -jar jloga-all.jar -remote-info

Available Processors = 8
Heap Memory = 536870912
Free Memory = 503673600
Max Memory = 8558477312
```

## External commands

**WARNING:** executing unsafe scripts may be a security risk!\
Be sure that every external analyzer you define comes from a trusted source, jloga can't apply any security related restriction on external processes.

You can create a set of files with extension `.jloga.json` inside a dedicated folder and then have those commands be executed inside jloga as custom analyzers.
This folder can be specified inside the settings or via the system property `jloga.ext.dir`.

Each command must be able to parse data from stdin and produce output to stdout, each line emitted from the command is associated to the last line parsed from the source.

Processes will be started in the folder where the json files are stored, if a `env.jloga.properties` file is present in the same directory it will be parsed as input variables for the external script definition, if a variable is not found in system environment, provided variables or custom ones, an error message will appear and the script will not be executed.

In addition to `env.jloga.properties` a `env-windows.jloga.properties` will be read when running in Windows environment and a `env-unix.jloga.properties` otherwise. The more specific env file will override the less specific properties.

Inside the `.json.jloga` files the `command` tag is an array of strings or other array of string, in the latter case the array contents will be concatenated with the system path separator.

### Variables
By specifing a `%VarName%` in each part of the *.json.jloga* file you can access to system environment variables plus some
more predefined one as in the below table:

| Name        | Description                                                          |
|-------------|----------------------------------------------------------------------|
| *Title*     | The title of the search panel                                        |
| *RootTitle* | The title of the root search panel, ususally contains the file name. |

The variables are read in sequence from: system environment, custom provided variables and search provided variables. Each source takes precedence over the previous ones.

See the folder [ext-search-samples](ext-search-samples) for some examples.

#### Custom parameters
Inside *.json.jloga* files you can define new variables bound to UI widgets, by now the most complete example is [cyggrep.jloga.json](ext-search-samples/cyggrep.jloga.json).

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
- Perspective ([home](https://perspective.finos.org/) - [license](https://github.com/finos/perspective/blob/master/LICENSE))
- Last but not least [IntelliJ Idea Community Edition](https://www.jetbrains.com/idea/)
