#!/bin/sh
JLOGA_HOME=~/bin/jloga.bin
java -cp "$JLOGA_HOME/jloga.jar:$JLOGA_HOME/flatlaf-1.2.jar" org.riekr.jloga.Main $@
