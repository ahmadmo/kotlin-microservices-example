#!/usr/bin/env bash

PID_FILE="${PID_FILE:-./services.pid}"

if [[ ! -f ${PID_FILE} ]]; then
    text="pid file not found, path = $PID_FILE"
    printf "\033[1;31m$text\033[0m\n"
    exit 1
fi

kill -9 `cat ${PID_FILE}`
rm ${PID_FILE}
