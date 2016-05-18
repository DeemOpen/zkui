#!/bin/sh

ZK_SERVER=${ZK_SERVER:-"localhost:2181"}

sed -i "s/^zkServer=.*$/zkServer=$ZK_SERVER/" /var/app/config.cfg

echo "Starting zkui with server $ZK_SERVER"

exec java -jar /var/app/zkui.jar