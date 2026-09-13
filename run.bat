@echo off
cd /d "%~dp0"

java -cp "engine.jar;ui.jar" guessmarket.ui.MainApp
if %ERRORLEVEL% EQU 0 goto :eof

echo Could not start with the system Java runtime alone.
echo Retrying with the bundled JavaFX runtime...
set PATH=%~dp0javafx-runtime\bin;%PATH%
java --module-path "javafx-runtime\lib" --add-modules javafx.controls,javafx.fxml -cp "engine.jar;ui.jar" guessmarket.ui.MainApp
pause