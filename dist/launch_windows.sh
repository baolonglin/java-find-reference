#!/bin/sh
JLINK_VM_OPTIONS="\
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
DIR=`dirname $0`
CLASSPATH_OPTIONS="-classpath $DIR/classpath/gson-2.8.9.jar;$DIR/classpath/commons-cli-1.5.0.jar;$DIR/classpath/java-find-reference.jar"
java $JLINK_VM_OPTIONS $CLASSPATH_OPTIONS $@
