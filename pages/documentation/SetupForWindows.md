---
title: Setup for Windows
layout: page-fullwidth
permalink: "/SetupForWindows/"
categories:
    - documentation
header:
   image_fullwidth: "header.png"
---


# Setting System Properties On Windows

Just about all the software we use depends on a variety of environment variables in order to work. So I'm writing here how to set those environment variables generically. That way whenever the code says 'set environment X to Y' you can refer to this section on how. This text is for Windows 7. Presumably Windows 8 is similar.

1. Right click on computer and select properties

1. In the screen that pops up select "Advanced system settings"

1. In the next screen that pops up select the 'Advanced' tab

1. On that tab select (at the bottom) "Environment Variables..."

You should then see a screen entitled 'Environment Variables'. It is broken into two parts, one for the user account you are logged in as and one for system variables that applies to all users. In general it doesn't matter for most people in most situations which one you select. I am inconsistent about which one I use but generally I suspect I should prefer system variables.

To create a new variable:

1. Hit New

1. Type in the variable name

1. Type in the path. The path does not need to be escaped, so you can type in `C:\Program Files (x86)\`...

In many cases you need to add to the system path. This is where the system looks to find binaries. Many apps depend on the path rather than dedicated system variables.

Generally speaking 'good behavior' is to not put file paths directly into your system path. Instead it's usually better to create a new system variable with the path you want and then put that variable's name into the system path. The reason for this indirection is that it makes it easier if you need to change a path (for example, you install a new version of a piece of software with a different version path) you don't have to dig through the huge path in the tiny path dialog but instead can just edit the variable you created.

For example, I needed to add the android tools to my path. I already had to have a variable *ANDROID_HOME* defined for other purposes. The tools live off `\tools` from *ANDROID_HOME*. So I first defined *ANDROID_HOME* and then added to the end of my path `";%ANDROID_HOME%\tools`.

To add to path:

1. If you don't already have some `*_HOME` variable pointing to the location (or base of the location) for your path then follow the directions above to create a variable to hold the path

1. In System Variables scroll down to the entry titled 'Path' and hit Edit.

1. In the 'Edit User Variable' dialog always APPEND the name of your variable to the end by typing a ';' (this is the delimiter) and then your variable name surrounded by '%' characters (tells the system its a variable) followed (if needed) by a '\\' and the rest of the path.

Existing cmd windows won't pick up the new variables so you need to close existing windows and open new ones to get the new values.

# What variables paths do you need?

I am going to try my best to capture all of them in the instructions below. But I looked through my own environmental variables and here is what I found so far:

<dl>

<dt> System variables</dt>
<dd> ANDROID_HOME, ANT_HOME, JAVA_HOME, M2, M2_HOME, MAVEN_HOME, VBOX_INSTALL</dd>

<dt> Values in path </dt>
<dd> Note some of these were put in automatically so they don't follow the variable strategy</dd>
<dd> Also note that these are extracted from the much larger path values, these are just the ones that I believe are relevant to Thali</dd>
<dd>%M2%;C:\Program Files (x86)\apache-maven-3.1.1\bin;%ANT_HOME%\bin;%ANDROID_HOME%\tools;c:\Program Files\apache-maven-3.0.3\bin;c:\apache_ant\bin;C:\Program Files (x86)\nodejs\;C:\Program Files\Perforce;%ANDROID_HOME%\platform-tools</dd>

</dl>

# Java for Windows

1. Go [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and click on the download button under JDK

1. Navigate to the section entitled 'Java SE Development Kit 8u5', click the accept button and then click on the entry for Windows_X86 (I know, you have a 64 bit OS, it doesn't matter, just click X86, there are still some folks who don't use 64 bit Java and the issues working with their software are not worth dealing with so just install the 32 bit version).

1. <strike>Now do it again, but with the 64 bit version. Android Studio now requires 64 bit Java. Joy!</strike> <-- Need to validate if this is true

Set *JAVA_HOME*, the path is usually something like: `C:\Program Files (x86)\Java\jdk1.8.0`

[Note: The latest Android studio gets really unhappy if you don't have *JAVA_HOME* pointing to 64 bit Java. So I'm now experimenting with that too.]

# Maven for Windows

We are currently using Maven 3.1.1. Please go [here](http://maven.apache.org/download.cgi) and download the binary zip. Unzip the file and put the unzipped directory (e.g. apache-maven-3.1.1) under `C:\Program Files (x86)`. Then create a system variable *MAVEN_HOME* that points to `c:\Program Files (x86)\apache-maven-3.1.1`. Then add to the end of your path `"%MAVEN_HOME%\bin"`.

However there is one thing worth knowing about how Maven works locally and that is where the local repository is. It lives under `c:\user\[your user id]\.m2\repository`. The JAR and AAR files are then stored in directories using their package names. When things aren't working it can be useful to jump into the local repository to see what the heck went wrong or even to see if you did your build correctly and the JAR/AAR has been updated in the repository.

As of March 2014 Maven is at 3.2.1.

As per [http://stackoverflow.com/questions/17136324/what-is-the-difference-between-m2-home-and-maven-home](http://stackoverflow.com/questions/17136324/what-is-the-difference-between-m2-home-and-maven-home), *M2_HOME* supercedes *MAVEN_HOME* for Maven 2.

To validate the install, mvn --version.

# Gradle for Windows

Since we use gradle wrappers there is no need to install gradle on your machine. Stuff should 'just work'.

# Android for Windows

We are constantly changing SDKs. Our base SDK version is 18 but we are currently building with 19 and I'm sure that will change. So just install the latest Android SDK from [here](http://developer.android.com/sdk/installing/studio.html). Make sure to create a system variable *ANDROID_HOME* and set it to something like `C:\Program Files (x86)\Android\android-studio\sdk`. Then add to the end of your path `";%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools"`.

To set up Android we need access to the Android SDK Manager. To do this I open an admin cmd window and navigate to `c:\Program Files (x86)\Android\android-studio\sdk\tools\lib` and execute android.bat.

* Tools/Android SDK Build-tools 19.1
* Tools/Android SDK Build-tools 19.0.1 (this might not be needed anymore)
* Tools/Android SDK Build-tools 18.1.1 (this might not be needed anymore)
* Tools/Android 4.4.2 (API 19) - What we are building with
* Tools/Android 4.3 (API 18) - This is what we are currently targeting
* Extras/Android Support Repository
* Extras/Android Support Library
* Extras/Google Repository

Then hit the Install Packages key, accept License and hit Install again.

Note that you may have to go through the install and/or delete Packages process a couple of times before the SDK Manager is happy.

I'm sure the specific numbers above will be out of date soon but the process should work so when you get errors about not having the right versions of whatever use the approach above to fix it.

# Visual Studio for Windows

You need to install the latest community edition of Visual Studio or you won't be able to build couchbase-lite-java-native.
