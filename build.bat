@echo off
setlocal
cd /d "%~dp0"

set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=build"

if exist "C:\msys64\ucrt64\bin" set "PATH=C:\msys64\ucrt64\bin;%PATH%"

if not defined JAVA_HOME (
    echo JAVA_HOME nao definido. Aponte para o JDK 8.
    exit /b 1
)
"%JAVA_HOME%\bin\javac.exe" -version 2>&1 | findstr /c:"1.8." >nul
if errorlevel 1 (
    echo JAVA_HOME nao e JDK 8: %JAVA_HOME%
    exit /b 1
)

set "MAKE_BIN=mingw32-make"
where mingw32-make >nul 2>&1 || set "MAKE_BIN=make"
where %MAKE_BIN% >nul 2>&1
if errorlevel 1 (
    echo make nao encontrado. Instale mingw-w64-ucrt-x86_64-make no MSYS2.
    exit /b 1
)

"%MAKE_BIN%" %TARGET%
if errorlevel 1 (
    echo Falha no alvo: %TARGET%
    exit /b 1
)

echo OK: %TARGET%
if /i "%TARGET%"=="build" echo Saida em dist\
endlocal
