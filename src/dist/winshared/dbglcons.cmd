@echo off
set JRE_required=1.7+
java -version:%JRE_required% -version 2> NUL:
if errorlevel 1 (
java -version:%JRE_required% -version
echo.
echo Sorry - you don't seem to have the required version of the
echo Java Runtime Environment [JRE] installed.  DBGL requires at
echo least JRE %JRE_required% to run.
echo.
pause
) else (
java -Djava.library.path=lib -jar dbgl.jar || pause
)
