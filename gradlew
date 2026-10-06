#!/bin/sh
APP_HOME=$(pwd)
exec java -Dorg.gradle.appname=gradlew -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
