#!/usr/bin/env bash

APP_NAME="money-transfer"
LOG_DIR="${LOG_DIR:-/var/log/$APP_NAME}"

function error_exit() {
    printf "\033[1;31m$1\033[0m\n"
    exit 1
}

if [[ ! -d ${LOG_DIR} ]]; then
    error_exit "log directory not found, path = $LOG_DIR"
fi

if [[ ! -w ${LOG_DIR} ]]; then
    error_exit "cannot write to log directory, path = $LOG_DIR"
fi

if [[ ! -f gradlew ]] || [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then
    error_exit "couldn't find gradle wrapper in current directory"
fi

PID_FILE="${PID_FILE:-./services.pid}"

function start_service() {
    printf "\033[1;34m$1\033[0m starting... "
    nohup ./gradlew $1:bootRun > "$LOG_DIR/$1.out" 2>&1 &
    service_pid=$!
    started=yes
    for i in 1 2 3 4 5; do
        sleep 3
        if ! kill -0 ${service_pid} > /dev/null 2>&1; then
            started=no
            break
        fi
    done
    if [[ ${started} = yes ]]; then
        echo ${service_pid} >> ${PID_FILE}
        printf "\033[1;32m[$service_pid]\033[0m\n"
    else
        printf "\033[1;31m[FAILED]\033[0m\n"
    fi
    printf "see logs = \033[1;33m$LOG_DIR/$1.out\033[0m\n\n"
}

start_service transaction
start_service account
start_service payment
start_service edge
