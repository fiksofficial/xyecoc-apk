#!/bin/sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
if [ -z "$DIRNAME" ]; then
  DIRNAME=.
fi

CLASSPATH=""
for f in "$DIRNAME"/gradle/wrapper/gradle-wrapper.jar; do
  if [ -f "$f" ]; then
    CLASSPATH="$f"
    break
  fi
done

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ];  then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if [ -z "$CLASSPATH" ]; then
    gradle "$@"
else
    exec "$JAVACMD" -jar "$CLASSPATH" "$@"
fi
