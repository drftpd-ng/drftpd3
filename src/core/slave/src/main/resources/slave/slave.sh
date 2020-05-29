#!/bin/sh

CLASSPATH="lib/*:build/*"
# Add JVM Options here however you see fit and please check if the max memory Xmx is good enough for your master
JVM_OPTS="-Xmx1G -XX:+UseG1GC"
OPTIONS="-Dlog4j.configurationFile=config/log4j2-slave.xml"
PROGRAM="org.drftpd.slave.Slave"

java ${JVM_OPTS} -classpath ${CLASSPATH} ${OPTIONS} ${PROGRAM}