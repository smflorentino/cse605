#!/bin/sh

set -e
set -x

rm -rf build
mkdir build
javac -target 1.5 -source 1.5 `find src | grep \\\.java$ | grep -v xml | grep -v optimizer` -d build

jar cf fiji-asm-1.0.jar -C build .

