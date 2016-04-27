#!/bin/sh

REFERENCE_FILE=/home/nikitosh/Documents/lib.tar.gz

PROJ=$PWD
TMP=/tmp/torrent

rm -rf $TMP
mkdir $TMP

mkdir $TMP/server
mkdir $TMP/client1
mkdir $TMP/client2
mkdir $TMP/client3

JAVA=/usr/lib/jvm/java-8-oracle/bin/java

cd $PROJ
mvn assembly:assembly 

JAR=$PROJ/target/torrent-1.0-SNAPSHOT-jar-with-dependencies.jar
SERVER="$JAVA -cp $JAR ru.spbau.mit.TorrentServer"
CLIENT="$JAVA -cp $JAR ru.spbau.mit.TorrentClient"

echo "KEK-1"

cd $TMP/server
$SERVER & 
PID0=$!

cd $TMP/client1

echo "KEK0"

$CLIENT newfile 127.0.0.1 $REFERENCE_FILE 

echo "KEK"

$CLIENT list 127.0.0.1

echo "KEK2"

#$CLIENT run 127.0.0.1
$CLIENT run 127.0.0.1 &
PID1=$!

echo "KEK3"

cd $TMP/client2
$CLIENT get 127.0.0.1 0
$CLIENT run 127.0.0.1 &
PID2=$!

cd $TMP/client3
$CLIENT get 127.0.0.1 0
$CLIENT run 127.0.0.1 &
PID3=$!

sleep 10 

FILE_SUFFIX=downloads/0/`basename $REFERENCE_FILE`


cmp --silent $TMP/client2/$FILE_SUFFIX $REFERENCE_FILE || echo "Bad file content for client 2"
cmp --silent $TMP/client3/$FILE_SUFFIX $REFERENCE_FILE || echo "Bad file content for client 3"

kill $PID0 $PID1 $PID2 $PID3
kill -9 $PID0 $PID1 $PID2 $PID3
