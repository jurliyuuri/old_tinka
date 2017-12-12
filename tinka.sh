#!/bin/sh
filename=${1##*/}
java -jar tinka.jar $1 -o ${filename%.*}.lk
