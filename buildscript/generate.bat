@echo off
REM Copyright (c) 2021 FalsePattern
REM Updated by CosmicDan 2023
REM
REM Permission is hereby granted, free of charge, to any person obtaining a copy
REM of this software and associated documentation files (the "Software"), to deal
REM in the Software without restriction, including without limitation the rights
REM to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
REM copies of the Software, and to permit persons to whom the Software is
REM furnished to do so, subject to the following conditions:
REM
REM The above copyright notice and this permission notice shall be included in all
REM copies or substantial portions of the Software.
REM
REM THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
REM IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
REM FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
REM AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
REM LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
REM OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
REM SOFTWARE.

:: CosmicDan: Move to scriptDir\..
SETLOCAL 
CD "%~dp0\.."

:: CosmicDan: First check for jextract
IF NOT DEFINED JEXTRACT_HOME ( 
	SET JEXTRACT_PATH=%JAVA_HOME%
)
IF NOT EXIST "%JEXTRACT_HOME%\bin\jextract.bat" (
	echo ERROR: jextract.bat not found. JAVA_HOME. Get JDK19+ from jextract site and either set JAVA_HOME or JEXTRACT_HOME for this task.
	EXIT /B 1
)

:: TODO: [CosmicDan] Move this to another gradle package
echo [#] Cleaning src\main\java\...
rmdir /S /Q .\src\main\java\win32 >nul  2>&1
del .\src\main\java\module-info.java >nul 2>&1
echo [#] Creating win32 folders...
mkdir .\src\main\java\win32\mapped\com
mkdir .\src\main\java\win32\mapped\struct
mkdir .\src\main\java\win32\mapped\constants
:: echo Cleanup finished!
echo [#] Generating panama mappings via jextract...
REM Edit this line if you want to make custom mappings:
::CALL "%JEXTRACT_PATH%" --source --header-class-name Win32 --output .\src\main\java -t win32.pure .\c\native.h
CALL :JEXTRACT --source --header-class-name Win32 --output .\src\main\java -t win32.pure .\c\native.h

echo Generation finished!
GOTO :EOF

:JEXTRACT
"%JEXTRACT_HOME%\bin\java" --enable-native-access=org.openjdk.jextract -m org.openjdk.jextract/org.openjdk.jextract.JextractTool %*