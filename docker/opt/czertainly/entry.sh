#!/bin/sh

czertainlyHome="/opt/czertainly"
source ${czertainlyHome}/static-functions

log "INFO" "Launching the Email Notification Provider"
java $JAVA_OPTS -jar ./app.jar

#exec "$@"