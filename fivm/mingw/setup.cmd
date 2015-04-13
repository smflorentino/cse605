@echo off

rem Run this script from the fivm home directory.
rem You may need to modify this script and/or other files in this directory.

echo Copying files and creating directories...

copy mingw\Makefile .
mkdir lib\targets\Win32
copy mingw\cmacros.properties lib\targets\Win32
copy mingw\fivmcrc lib\targets\Win32
copy mingw\fivmr_target.h lib\targets\Win32
copy mingw\fivmcrc lib\
copy mingw\host_fivmcrc lib\
copy mingw\config.rb lib\
copy mingw\Config.java common\src\com\fiji\fivm\
copy mingw\fivmr_config.h runtimec\src\
copy mingw\fivmc.cmd bin\

echo Fiji VM is configured.  Now you must build it by typing 'make'.
