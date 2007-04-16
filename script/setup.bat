@echo off
echo ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo WINDOWS setup script
echo To run this you should:
echo + have JAVA_HOME defined
echo + have ANT installed
echo + either
echo + (a) - run all commands, including this one, from the directory containing the build.xml file, OR
echo + (b) - define MINORTHIRD to be that directory
echo ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
if "%MINORTHIRD%"=="" set MINORTHIRD=.
set CLASSPATH=%CLASSPATH%;.
set CLASSPATH=%CLASSPATH%;%MINORTHIRD%
set CLASSPATH=%CLASSPATH%;%MINORTHIRD%/class
set CLASSPATH=%CLASSPATH%;%MINORTHIRD%/config
set CLASSPATH=%CLASSPATH%;%MINORTHIRD%/lib/minorThirdIncludes.jar
set CLASSPATH=%CLASSPATH%;%MINORTHIRD%/lib/mixup;
set MONTYLINGUA=%MINORTHIRD%/lib/montylingua


