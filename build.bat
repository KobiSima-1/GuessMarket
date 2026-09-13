@echo off
REM builds engine.jar and ui.jar into the dist folder
cd /d "%~dp0"
if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out\engine out\ui dist

dir /s /b engine\src\*.java > out\engine-sources.txt
javac --release 21 -d out\engine @out\engine-sources.txt || goto :error

dir /s /b ui\src\*.java > out\ui-sources.txt
javac --release 21 -cp "out\engine;javafx-runtime\lib\*" -d out\ui @out\ui-sources.txt || goto :error

xcopy /e /i /y ui\resources out\ui >nul

jar cf dist\engine.jar -C out\engine .
jar cfm dist\ui.jar manifest.txt -C out\ui .
copy run.bat dist\run.bat >nul
xcopy /e /i /y javafx-runtime dist\javafx-runtime >nul

echo Build finished. The jars are in the dist folder.
goto :eof

:error
echo Build failed.