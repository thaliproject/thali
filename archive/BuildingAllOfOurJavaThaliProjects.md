---
title: Building All of Our Java Thali Projects
layout: default
---

<dl>
<dt> Repository Location</dt>
<dd>
<a href="http://github.com/thaliproject/thali">Main Thali repository</a>
</dd>
<dt>Branch</dt>
<dd>master</dd>
</dl>

# Building Libraries 

<dl>
<dt> Project Locations</dt>
<dd> Production/Utilities/UniversalUtilities</dd>
<dd> Production/Utilities/AndroidUtilities</dd>
<dd> Production/Utilities/JavaUtilities </dd>
<dd> Production/ThaliDeviceHub/Universal</dd>
</dl>

The previous all produce libraries. So they are built as given below.

To actually do a build:

1. Follow the instructions in [UnderstandingThalisUseOfMaven](archive/UnderstandingThalisUseOfMaven)

1. Open the Git bash shell, go to the root of each project (in the order listed above, order matters since they have dependencies on each other) and issue 'gradlew uploadArchives'

# Building Thali Device Hubs 

<dl>
<dt> Project Locations</dt>
<dd> Production/ThaliDeviceHub/android</dd>
<dd> Production/ThaliDeviceHub/java</dd>
</dl>

These both produce executables. 

Android produces an apk which you can find in Production/ThaliDeviceHub/android/android/build/apk after doing a "gradlew build". You can use adb to install the apk.

Java, after a "gradlew distZip" produces Production/ThaliDeviceHub/java/build/distributions/java.zip. Move that zip some place nice, unzip it and then executed from your friendly neighborhood command line 'java.jar'.

# Notes 

## Production/Utilities/UniversalUtilities

<pre>
C:\Users\Jon\thali-master\thali\Production\Utilities\UniversalUtilities>gradlew uploadArchives
Creating properties on demand (a.k.a. dynamic properties) has been deprecated and is scheduled to be removed in Gradle 2.0. Please read http://gradle.org/docs/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html for information on the replacement for dynamic properties.
Deprecated dynamic property: "source" on "root project 'UniversalUtilities'", value: "[C:\Users\Jon\thali-ma...".:compileJava
warning: [options] bootstrap class path not set in conjunction with -source 1.6
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 warning
:processResources
:classes
:javadoc
C:\Users\Jon\thali-master\thali\Production\Utilities\UniversalUtilities\src\main\java\com\msopentech\thali\CouchDBListener\BogusAuthorizeCouchDocument.java:68:warning - @return tag has no arguments.
...
27 warnings
:generateJavadocs
:jar
:sourcesJar
:uploadArchives
Uploading: com/msopentech/thali/ThaliUtilitiesUniversal/0.0.2/ThaliUtilitiesUniversal-0.0.2.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 73K from remote
Uploaded 73K
Uploading: com/msopentech/thali/ThaliUtilitiesUniversal/0.0.2/ThaliUtilitiesUniversal-0.0.2-javadoc.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 0K from remote
Uploaded 0K
Uploading: com/msopentech/thali/ThaliUtilitiesUniversal/0.0.2/ThaliUtilitiesUniversal-0.0.2-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 54K from remote
Uploaded 54K

BUILD SUCCESSFUL
</pre>

## Production/Utilities/AndroidUtilities

<pre>
C:\Users\Jon\thali-master\thali\Production\Utilities\AndroidUtilities>gradlew uploadArchives
Relying on packaging to define the extension of the main artifact has been deprecated and is scheduled to be removed in Gradle 2.0
WARNING: Dependency org.apache.httpcomponents:httpclient:4.3 is ignored for debug as it may be conflicting with the internal version provided by Android.
         In case of problem, please repackage it with jarjar to change the class packages

...more of the same...


 packages
:AndroidUtilities:compileLint
:AndroidUtilities:copyReleaseLint UP-TO-DATE
:AndroidUtilities:mergeReleaseProguardFiles
:AndroidUtilities:packageReleaseAidl UP-TO-DATE
:AndroidUtilities:preBuild
:AndroidUtilities:preReleaseBuild
:AndroidUtilities:checkReleaseManifest
:AndroidUtilities:preDebugBuild
:AndroidUtilities:preDebugTestBuild
:AndroidUtilities:prepareComAndroidSupportAppcompatV71901Library
:AndroidUtilities:prepareComCouchbaseLiteAndroid100Beta3rc1Library
:AndroidUtilities:prepareReleaseDependencies
:AndroidUtilities:compileReleaseAidl
:AndroidUtilities:compileReleaseRenderscript
:AndroidUtilities:generateReleaseBuildConfig
:AndroidUtilities:mergeReleaseAssets
:AndroidUtilities:generateReleaseResValues
:AndroidUtilities:generateReleaseResources
:AndroidUtilities:mergeReleaseResources
:AndroidUtilities:processReleaseManifest
:AndroidUtilities:processReleaseResources
:AndroidUtilities:generateReleaseSources
:AndroidUtilities:compileReleaseJava
:AndroidUtilities:processReleaseJavaRes UP-TO-DATE
:AndroidUtilities:packageReleaseJar
:AndroidUtilities:compileReleaseNdk
:AndroidUtilities:packageReleaseJniLibs UP-TO-DATE
:AndroidUtilities:packageReleaseLocalJar UP-TO-DATE
:AndroidUtilities:packageReleaseRenderscript UP-TO-DATE
:AndroidUtilities:packageReleaseResources
:AndroidUtilities:bundleRelease
:AndroidUtilities:sourcesJar
:AndroidUtilities:uploadArchives
Uploading: com/msopentech/thali/ThaliUtilitiesAndroid/0.0.2/ThaliUtilitiesAndroid-0.0.2.aar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 42K from remote
Uploaded 42K
Uploading: com/msopentech/thali/ThaliUtilitiesAndroid/0.0.2/ThaliUtilitiesAndroid-0.0.2-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 4K from remote
Uploaded 4K

BUILD SUCCESSFUL
</pre>

## Production/Utilities/JavaUtilities

<pre>

C:\Users\Jon\thali-master\thali\Production\Utilities\JavaUtilities>gradlew uploadArchives
The ConfigurationContainer.add() method has been deprecated and is scheduled to
be removed in Gradle 2.0. Please use the create() method instead.
Creating properties on demand (a.k.a. dynamic properties) has been deprecated and is scheduled to 
be removed in Gradle 2.0. Please read http://gradle.org/docs/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html 
for information on the replacement for dynamic properties.
Deprecated dynamic property: "source" on "root project 'JavaUtilities'", value:"[C:\Users\Jon\thali-ma...".
:compileJava
:processResources UP-TO-DATE
:cssToBin
:classes
:javadoc
C:\Users\Jon\thali-master\thali\Production\Utilities\JavaUtilities\src\main\java\com\msopentech\thali\utilities\java\JavaEktorpCreateClientBuilder.java:32: warning - @return tag has no arguments.
1 warning
:generateJavadocs
:jar
:jfxJar
:jfxSignJar SKIPPED
:sourcesJar
:uploadArchives
Uploading: com/msopentech/thali/ThaliUtilitiesJava/0.0.2/ThaliUtilitiesJava-0.0.2.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 20K from remote
Uploaded 20K
Uploading: com/msopentech/thali/ThaliUtilitiesJava/0.0.2/ThaliUtilitiesJava-0.0.2-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 3K from remote
Uploaded 3K
Uploading: com/msopentech/thali/ThaliUtilitiesJava/0.0.2/ThaliUtilitiesJava-0.0.2-javadoc.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 0K from remote
Uploaded 0K

BUILD SUCCESSFUL
</pre>

## Production/ThaliDeviceHub/Universal

<pre>
C:\Users\Jon\thali-master\thali\Production\ThaliDeviceHub\Universal>gradlew uploadArchives
Creating properties on demand (a.k.a. dynamic properties) has been deprecated and is 
scheduled to be removed in Gradle 2.0. Please read 
http://gradle.org/docs/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html for information
on the replacement for dynamic properties.
Deprecated dynamic property: "source" on "root project 'Universal'", value: "[C:
\Users\Jon\thali-ma...".
:compileJava
warning: [options] bootstrap class path not set in conjunction with -source 1.6
1 warning
:processResources
:classes
:javadoc
:generateJavadocs
:jar
:sourcesJar
:uploadArchives
Uploading: com/msopentech/thali/ThaliTDHUniversal/0.0.2/ThaliTDHUniversal-0.0.2.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 2K from remote
Uploaded 2K
Uploading: com/msopentech/thali/ThaliTDHUniversal/0.0.2/ThaliTDHUniversal-0.0.2-sources.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 1K from remote
Uploaded 1K
Uploading: com/msopentech/thali/ThaliTDHUniversal/0.0.2/ThaliTDHUniversal-0.0.2-javadoc.jar to repository remote at file://C:\Users\Jon\.m2\repository
Transferring 0K from remote
Uploaded 0K

BUILD SUCCESSFUL
</pre>

## Build executables 

<dl>
<dt> Project Locations</dt>
<dd> Production/ThaliDeviceHub/android</dd>
</dl>

Right now there is just one project here that builds the Thali Device Hub for Android. Soon we will hopefully add a second project to build a Java JAR that will run the Thali Device Hub for use on desktop OSs.

In the case of android the output is an apk file that can then be run as an android program. To build the apk just go into the android project and run 'gradlew build'
