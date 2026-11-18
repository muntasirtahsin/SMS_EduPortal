@echo off
echo =========================================================
echo    Building EduPortal Pure Java and JavaFX Application
echo =========================================================

if not exist bin mkdir bin

REM 1. Download OpenJFX dependencies if missing
if not exist lib\javafx-controls-21.0.2.jar (
    echo Downloading JavaFX dependencies...
    powershell -ExecutionPolicy Bypass -File download_deps.ps1
)

REM 2. Compile Java Source Files
echo Compiling Java source files...
dir /s /B src\*.java > sources.txt
javac -d bin -cp "lib/*" @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    del sources.txt
    pause
    exit /b %ERRORLEVEL%
)
del sources.txt
echo Compilation finished successfully.

REM 2b. Copy CSS and other resources to bin
echo Copying resources...
if exist src\style.css copy /Y src\style.css bin\style.css >nul

REM 3. Launch Pure Java & JavaFX Application
echo Launching JavaFX Desktop Application...
java --module-path lib --add-modules javafx.controls,javafx.fxml,javafx.graphics --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" com.eduportal.Main

pause
