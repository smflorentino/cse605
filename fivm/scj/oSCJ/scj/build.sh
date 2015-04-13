#!/bin/bash

set -e
set -x

rm -rf build
mkdir build

javac `find ri/ -name *.java -not -path "*ovm*"` -d build 
jar cf scj.jar -C build edu -C build javax


cp scj.jar ./ri/scj.jar
rm -rf scj.jar