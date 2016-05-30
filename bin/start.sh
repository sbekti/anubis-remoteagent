#!/usr/bin/env bash

cd "$(dirname "$0")"

java -classpath ../build/libs/*:../libs/*:../libs/*.jar \
  -Dconfig=../conf/agent.properties \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
  -Dorg.slf4j.simpleLogger.showShortLogName=true \
  io.bekti.anubis.remoteagent.RemoteAgentMain
