---
title: Building All of our Couchbase Dependencies
layout: default
---

<dl>

<dt> Repository Location</dt>
<dd> <a href="https://github.com/thaliproject/couchbase-lite-java-native">https://github.com/thaliproject/couchbase-lite-java-native</a></dd>

<dt> Branch</dt>
<dd> master</dd>
</dl>

This is the CouchBase repository that builds SQLite from a [single](https://raw.githubusercontent.com/couchbase/couchbase-lite-java-native/master/src/main/c/sqlite3.c) [amalgamated](https://sqlite.org/amalgamation.html) C file and makes it available to Java. Note that the Windows build is handled via Visual Studio so please make sure you have that installed when you build on Windows.

NOTE: YOU MUST HAVE INSTALLED 32 BIT JAVA AND CONFIGURED INTELLIJ TO USE 32 BIT JAVA OR BAD THINGS WILL HAPPEN. 

<dl>

<dt> Repository Locations</dt>

<dd> <a href="https://github.com/thaliproject/couchbase-lite-java-core">https://github.com/thaliproject/couchbase-lite-java-core</a>, <a href="https://github.com/thaliproject/couchbase-lite-java-listener">https://github.com/thaliproject/couchbase-lite-java-listener</a>, <a href="https://github.com/thaliproject/couchbase-lite-android">https://github.com/thaliproject/couchbase-lite-android</a> & <a href="https://github.com/thaliproject/couchbase-lite-java">https://github.com/thaliproject/couchbase-lite-java</a> </dd>

<dt> Branch </dt>

<dd> master </dd>

</dl>

We have our own custom version of the entire CouchBase Lite stack that has a variety of features that aren't in mainline CouchBase Lite. Which is why we have to fork everything.

To actually do a build:

1. Follow the instructions in [Understanding Thalis Use of Maven](Understanding Thalis Use of Maven)

1. Open the Git bash shell, go to the root of each project (in the order listed above, order matters since they have dependencies on each other) and issue 'gradlew installAll'


# Notes 

## couchbase-lite-java-native 

You may need to update the gradle.properties file (until the bug fix is committed) to this build to satisfy the build.gradle requirement for MAVEN_UPLOAD_VERSION.  Should look like this:

<pre>
systemProp.UPLOAD_VERSION_CBLITE=1.0.0-beta3rc1
systemProp.MAVEN_UPLOAD_VERSION=1.0.0-beta3rc1
</pre>

First try fails, header file conflicts.

<pre>
C:\Users\Jon\thali-master\couchbase-lite-java-native>gradlew uploadArchives
:compileJava
:processResources
:classes
:native_libraryCExtractHeaders
:compileWindows_x86Native_librarySharedLibraryNative_libraryCcom_couchbase_lite_storage_JavaSQLiteStorageEngine.c
C:\Users\Jon\thali-master\couchbase-lite-java-native\src\main\c\com_couchbase_lite_storage_JavaSQLiteStorageEngine.c(33) : error C2275: 'va_list' : illegal useof this type as an expression
        C:\Program Files (x86)\Microsoft Visual Studio 12.0\VC\include\vadefs.h(59) : see declaration of 'va_list'
C:\Users\Jon\thali-master\couchbase-lite-java-native\src\main\c\com_couchbase_lite_storage_JavaSQLiteStorageEngine.c(33) : error C2146: syntax error : missing ';' before identifier 'ap'
C:\Users\Jon\thali-master\couchbase-lite-java-native\src\main\c\com_couchbase_lite_storage_JavaSQLiteStorageEngine.c(33) : error C2065: 'ap' : undeclared identifier
</pre>

Move c:\MinGW\mingw32\bin;c:\MinGW\bin; from end of %PATH% to front and retry

<pre>
C:\Users\Jon\thali-master\couchbase-lite-java-native\build\objectFiles\native_librarySharedLibrary\windows_x86\native_libraryC\log.obj:(.text$mn+0x0): multiple definition of `log_w'
C:\Users\Jon\thali-master\couchbase-lite-java-native\build\objectFiles\native_librarySharedLibrary\windows_x86\native_libraryC\log.o:log.c:(.text+0x1ea): first defined here
C:\Users\Jon\thali-master\couchbase-lite-java-native\build\objectFiles\native_librarySharedLibrary\windows_x86\native_libraryC\log.obj:(.text$mn+0x30): multiple definition of `log_e'
C:\Users\Jon\thali-master\couchbase-lite-java-native\build\objectFiles\native_librarySharedLibrary\windows_x86\native_libraryC\log.o:log.c:(.text+0x219): first defined here
</pre>

gradle clean and retry

<pre>
C:\Users\Jon\thali-master\couchbase-lite-java-native>gradlew clean

BUILD SUCCESSFUL

Total time: 4.234 secs

C:\Users\Jon\thali-master\couchbase-lite-java-native>gradlew uploadArchives
:compileJava
:processResources
:classes
:native_libraryCExtractHeaders UP-TO-DATE
:compileWindows_x86Native_librarySharedLibraryNative_libraryC
:linkWindows_x86Native_librarySharedLibrary
:jar
:sourcesJar
:uploadArchives
Uploading: com/couchbase/lite/java-native/1.0.0-beta3rc1/java-native-1.0.0-beta3
rc1.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 377K from remote
Uploaded 377K
Uploading: com/couchbase/lite/java-native/1.0.0-beta3rc1/java-native-1.0.0-beta3
rc1-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 1K from remote
Uploaded 1K

BUILD SUCCESSFUL
</pre>

## couchbase-lite-java-core 

<pre>
C:\Users\Jon\thali-master\couchbase-lite-java-core>gradlew uploadArchives
:compileJava UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:jar UP-TO-DATE
:sourcesJar UP-TO-DATE
:uploadArchives
Uploading: com/couchbase/lite/java-core/1.0.0-beta3rc1/java-core-1.0.0-beta3rc1.
jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 352K from remote
Uploaded 352K
Uploading: com/couchbase/lite/java-core/1.0.0-beta3rc1/java-core-1.0.0-beta3rc1-
sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 231K from remote
Uploaded 231K

BUILD SUCCESSFUL
</pre>

## couchbase-lite-java-listener 

<pre>

C:\Users\Jon\thali-master\couchbase-lite-java-listener>gradlew uploadArchives
:compileJava
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
:processResources UP-TO-DATE
:classes
:jar
:sourcesJar
:uploadArchives
Uploading: com/couchbase/lite/java-listener/1.0.0-beta3rc1/java-listener-1.0.0-b
eta3rc1.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 11K from remote
Uploaded 11K
Uploading: com/couchbase/lite/java-listener/1.0.0-beta3rc1/java-listener-1.0.0-b
eta3rc1-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 9K from remote
Uploaded 9K

BUILD SUCCESSFUL

</pre>

## couchbase-lite-java 

First try: Not yet. Requires couchbase-lite-native.

Second try after successful build of couchbase-lite-native

<pre>

C:\Users\Jon\thali-master\couchbase-lite-java>gradlew uploadArchives
:compileJava
Download http://thaliartifactory.cloudapp.net/artifactory/libs-snapshot/org/apache/httpcomponents/httpclient/4.0-beta1/httpclient-4.0-beta1.jar
Download http://thaliartifactory.cloudapp.net/artifactory/libs-snapshot/org/apache/httpcomponents/httpcore/4.0-beta2/httpcore-4.0-beta2.jar
Download http://thaliartifactory.cloudapp.net/artifactory/libs-snapshot/commons-codec/commons-codec/1.3/commons-codec-1.3.jar
:processResources
:classes
:jar
:sourcesJar
:uploadArchives
Uploading: com/couchbase/lite/java/1.0.0-beta3rc1/java-1.0.0-beta3rc1.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 13K from remote
Uploaded 13K
Uploading: com/couchbase/lite/java/1.0.0-beta3rc1/java-1.0.0-beta3rc1-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 7K from remote
Uploaded 7K

BUILD SUCCESSFUL
</pre>

## couchbase-lite-android 

<pre>
C:\Users\Jon\thali-master\couchbase-lite-android>gradlew uploadArchives
:compileLint
:copyReleaseLint UP-TO-DATE
:mergeReleaseProguardFiles UP-TO-DATE
:packageReleaseAidl UP-TO-DATE
:preBuild
:preReleaseBuild
:checkReleaseManifest
:prepareReleaseDependencies
:compileReleaseAidl
:compileReleaseRenderscript
:generateReleaseBuildConfig
:mergeReleaseAssets
:generateReleaseResValues UP-TO-DATE
:generateReleaseResources
:packageReleaseResources
:processReleaseManifest
:processReleaseResources
:generateReleaseSources
:compileReleaseJava
:processReleaseJavaRes
:packageReleaseJar
:compileReleaseNdk
:packageReleaseJniLibs UP-TO-DATE
:packageReleaseLocalJar UP-TO-DATE
:packageReleaseRenderscript UP-TO-DATE
:bundleRelease
:sourcesJar
:uploadArchives
Uploading: com/couchbase/lite/android/1.0.0-beta3rc1/android-1.0.0-beta3rc1.aar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 58K from remote
Uploaded 58K
Uploading: com/couchbase/lite/android/1.0.0-beta3rc1/android-1.0.0-beta3rc1-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 5K from remote
Uploaded 5K

BUILD SUCCESSFUL
</pre>

