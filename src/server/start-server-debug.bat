@echo off

if EXIST logs goto clean_logs
md logs

:start-server
node debug server.js
goto done

:clean_logs
del /q logs\*.*
goto start-server

:done
