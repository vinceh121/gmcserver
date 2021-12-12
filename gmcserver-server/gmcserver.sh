#!/bin/sh
. /usr/lib/java-wrappers/java-wrappers.sh

find_java_runtime openjdk

find_jars gmcserver

run_java me.vinceh121.gmcserver.GMCServer "$@"

