@echo off

rem This runs the fivmc compiler by invoking Ruby

set cmdname=%~f0
set rubycmd=%cmdname:fivmc.cmd=fivmc%

ruby %rubycmd% %*
