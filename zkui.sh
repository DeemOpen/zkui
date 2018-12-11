#!/usr/bin/env bash

PIDFILE=/var/run/zkui.pid
ZKUIBINDIR=$(cd `dirname $0`; pwd)
ZKUICLASSNAME="zkui-2.0-SNAPSHOT-jar-with-dependencies.jar"
ZKUI_DAEMON_OUT=$ZKUIBINDIR/zkui.out

start(){
    echo  -n "Starting zkui ... "
    if [ -f "$PIDFILE" ]; then
        if kill -0 `cat "$PIDFILE"` > /dev/null 2>&1; then
            echo zkui already running as process `cat "$PIDFILE"`. 
            exit 0
        fi
    fi
    nohup java -jar "$ZKUIBINDIR/$ZKUICLASSNAME" > "$ZKUI_DAEMON_OUT" 2>&1 < /dev/null &
    if [ $? -eq 0 ];
    then
        echo $!>$PIDFILE
        if [ $? -eq 0 ];
        then
            sleep 1
            echo STARTED
        else
            echo FAILED TO WRITE PID
            exit 1
        fi
    else
        echo SERVER DID NOT START
        exit 1
    fi
}

stop(){
    echo -n "Stopping zkui ... "
    if [ ! -f "$PIDFILE" ]
    then
        echo "no zkui to stop (could not find file $PIDFILE)"
    else
        kill -9 $(cat "$PIDFILE")
        rm "$PIDFILE"
        echo STOPPED
    fi
    exit 0
}

case "$1" in
start)
    start
    ;;
stop)
    stop
    ;;
restart)
    shift
    "$0" stop
    sleep 3
    "$0" start
    ;;
*)
    echo "Usage: $0 {start|stop|restart}" >&2
esac