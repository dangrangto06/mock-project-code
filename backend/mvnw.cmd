@REM Maven Wrapper Script for Windows
@REM ---------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one or more
@REM contributor license agreements.
@REM ---------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "MVN_CMD=mvn.cmd") ELSE (SET "MVN_CMD=%__MVNW_ARG0_NAME__%")

@SET MAVEN_PROJECTBASEDIR=%~dp0

@SET MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
@SET MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@IF EXIST "%MAVEN_WRAPPER_JAR%" (
    @"%JAVA_HOME%\bin\java.exe" -classpath "%MAVEN_WRAPPER_JAR%" ^
        "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
        org.apache.maven.wrapper.MavenWrapperMain %*
) ELSE (
    @ECHO Maven Wrapper JAR not found. Please run: mvn wrapper:wrapper
    @EXIT /B 1
)
