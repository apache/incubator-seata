@REM ----------------------------------------------------------------------------
@REM Copyright 2001-2004 The Apache Software Foundation.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM ----------------------------------------------------------------------------
@REM

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\lib

set CLASSPATH="%BASEDIR%"\conf;"%REPO%"\fescar-core-0.1.0-SNAPSHOT.jar;"%REPO%"\fastjson-1.2.48.jar;"%REPO%"\netty-all-4.1.24.Final.jar;"%REPO%"\fescar-config-0.1.0-SNAPSHOT.jar;"%REPO%"\config-1.2.1.jar;"%REPO%"\netty-transport-native-epoll-4.1.24.Final-linux-x86_64.jar;"%REPO%"\netty-common-4.1.24.Final.jar;"%REPO%"\netty-buffer-4.1.24.Final.jar;"%REPO%"\netty-transport-native-unix-common-4.1.24.Final.jar;"%REPO%"\netty-transport-4.1.24.Final.jar;"%REPO%"\netty-resolver-4.1.24.Final.jar;"%REPO%"\netty-transport-native-kqueue-4.1.24.Final-osx-x86_64.jar;"%REPO%"\slf4j-api-1.7.22.jar;"%REPO%"\logback-classic-1.1.6.jar;"%REPO%"\logback-core-1.1.6.jar;"%REPO%"\junit-4.12.jar;"%REPO%"\hamcrest-core-1.3.jar;"%REPO%"\fescar-common-0.1.0-SNAPSHOT.jar;"%REPO%"\commons-pool2-2.4.2.jar;"%REPO%"\commons-pool-1.6.jar;"%REPO%"\commons-lang-2.6.jar;"%REPO%"\fescar-server-0.1.0-SNAPSHOT.jar
set EXTRA_JVM_ARGUMENTS=
goto endInit

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% %EXTRA_JVM_ARGUMENTS% -classpath %CLASSPATH_PREFIX%;%CLASSPATH% -Dapp.name="fescar-server" -Dapp.repo="%REPO%" -Dbasedir="%BASEDIR%" com.alibaba.fescar.server.Server %CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=1

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@endlocal

:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
