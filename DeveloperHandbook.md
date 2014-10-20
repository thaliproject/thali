---
title: Developer Handbook
layout: default
---

# Introduction 

This document is exclusively intended for people who are developing Thali itself. This document is not for those who wish to build apps on top of Thali.

# FIRST THING 

Go off to [https://github.com/thaliproject/](https://github.com/thaliproject/) and after you login with your GitHub account please go to each of the projects and hit 'watch'. We don't send out emails with notifications of PRs, issues, builds, etc. We use GitHub for that. So you need to be set up to watch (and make sure your settings at GitHub are that you should be emailed when a watch even happens) or you will miss what's going on.

Next go [here](https://www.pivotaltracker.com/n/projects/1163162). That is where we keep our backlog. If you are officially part of the project then go bug Yaron to set up an account for you so you can be assigned items. By default however it is world readable so anyone can track what we are up to.

# Code Lay out 

Our first dependency is Tor_Onion_Proxy_Library. It contains three sub-projects, universal, java and android. Most of the code lives in universal who builds a JAR that then the java and android projects depend on. The java project then produces a Jar apps can use to enable themselves with Tor and android produces an AAR for the same purpose.

Next up is our CouchBase forks. These contain custom versions of CouchBase with extensions we need. The dependency structure is:

<pre>
 couchbase-lite-java-native <- couchbase-lite-java-core <- couchbase-lite-listener 
                                                        <- couchbase-lite-java
                                                        <- couchbase-lite-android
</pre>


We then have the Thali project which is where we build the TDH. Right now there is an odd structure in there involving two root directories, Production and Prototype. When we resolve [https://www.pivotaltracker.com/story/show/78493988](https://www.pivotaltracker.com/story/show/78493988) we will get rid of Prototype and hoist the contents of Production to the root. For now we will ignore Prototype.

<pre>
 Thali
  -Production
   -Utilities
    -UniversalUtilities
    -JavaUtilities
    -AndroidUtilities
    -DotNetUtilities
    -WindowsStoreUtilities
</pre>

Of these right now active development is focused on UniversalUtilities, JavaUtilities and AndroidUtilities. They have the same relationship as the similarly named projects in Tor_Onion_Proxy_Library. The bulk of the logic is in UniversalUtilities which produces a jar consumed by JavaUtilities and AndroidUtilities who themselves produce a Jar and AAR respectively.

This then takes us to:

<pre>
 Thali
  -Production
   -ThaliDeviceHub
    -Universal
    -android
    -java
</pre>

Same structure as before.

The ThaliHTML5ApplicationFramework contains three key directories, web, Java and Android. The relationship here is slightly different as both Java and Android will cause web to build (it's a bunch of bower scripts) and then copy the files they need.

The thali-addressbook depo is actually a fork of the ThaliHTML5ApplicationFramework. So it's structured the same way. Eventually we expect this project to be folded into the Thali project.

Please note that we have standardized on gradle for our build environment and that no IDE specific code is to be checked in.

# Instructions for building our repositories 

We publish releases and binaries so this section is only needed for people who are actually developing Thali so they can make changes locally.

## Using the global build script to build the TDH 

The most sane way to handle things until we have Jenkins automating our builds is to use the global build script. To do this one has to:

1. Go to https://github.com/thaliproject/ and fork everything in sight

1. On your dev machine clone all your forks into the same directory

1. Set up your gradle.properties as explained in [Understanding Thali's Use of Maven](UnderstandingThalisUseOfMaven)

1. Now go to the thali clone and run "thali\Production\gradlew installAll"

This will build everything the TDHs need including the Tor library, all the couchbase libraries, and all the Thali libraries. What it won't build is the address book or the Thali HTML5 app template. We'll talk about them in the next section.

It's worth looking at thali\Production\build.gradle as it contains various useful options including ones to turn off tests and do global cleans.

## Building the Thali HTML 5 framework and the address book 

Since one is a fork of the other they both build in the same way. You need to go to "ThaliHTML5ApplicationFramework\gradlew buildAll" and everything will be built. Note that the Android build takes 2 or 3 minutes. So during development I'll typically just call "ThaliHTML5ApplicationFramework\gradlew :java:installApp" to save time.

## Building our Java/Android Projects 

Make sure to review these links as they have all sorts of annoying bits of information needed to actually build things.

[Understanding Thali's Use of Maven](UnderstandingThalisUseOfMaven)

[Building All Of Our CouchBase Dependencies](BuildingAllOfOurCouchbaseDependencies)

[Building All Of Our Java Thali Projects](BuildingAllOfOurJavaThaliProjects)

## Building our .net Projects 

The code here hasn't had much love recently so this section should be considered historical.

[Configuring LoveSeat](ConfiguringLoveSeat)

[Building our .net Projects](BuildingOurDotNetProjects)

## Building our Javascript Projects 

Right now we are using stock PouchDB. We just keep this section around if we have to remember how we used to build our own version.

[Configuring PouchDB](ConfiguringPouchDB)

# Dev Machine Set Up 

We need a bunch of software. We use IntelliJ Ultimate Edition although I'm told that the (free) community edition supports what we do just fine. In addition one needs the latest JDK, latest Android SDK, a local maven installation, gradle and a decent android emulator and/or device.

We have a bunch of instructions on how to get this software for Windows, see [set up for windows](SetupForWindows) but we have tested everything on Mac and Linux and it all runs just fine there as well.

NOTE: EVEN IF YOU AREN'T RUNNING ON WINDOWS STILL READ THE [set up for windows](SetupForWindows) BECAUSE IT CONTAINS IMPORTANT CONFIGURATION INSTRUCTIONS THAT APPLY TO ALL PLATFORMS

# Git 

The [Thali Guide to Git](ThaliGuideToGit) has a bunch of information about how we use git but anyone who has used git regularly isn't going to find anything new there.

# Debugging #

Please see the [Guide to Debugging](GuideToDebugging) for more information than you could ever want on how debugging works across our various dependencies.

# Virtual Box 

Just install the latest from [here](https://www.virtualbox.org/wiki/Downloads). It automatically updates itself anyway so I don't worry about what version I'm running.

# Android Emulator (that isn't glacially slow) 

See [Android Emulator](AndroidEmulator)


