---
title: Understanding Thali's Use of Maven
layout: page-fullwidth
permalink: "/UnderstandingThalisUseOfMaven/"
header:
   image_fullwidth: "header.png"
categories:
    - technologyinvestigations
---

# Building locally, believe me, you want to start here first

For local development Thali depends on mavenLocal. So go install maven if you haven't already.

When we build locally we use a set of properties in all of our projects to control our build behavior. Specifically we use:

<pre>
 systemProp.MAVEN_UPLOAD_REPO_URL=http://thaliartifactory.cloudapp.net/artifactory/libs-release-local
 systemProp.MAVEN_UPLOAD_VERSION=0.0.0
 systemProp.MAVEN_UPLOAD_USERNAME=user
 systemProp.MAVEN_UPLOAD_PASSWORD=[The base64 key from artifactory it should start with {DESede} and end with == (usually)]

 # These are required by Couchbase
 systemProp.buildListenerWithArtifacts = true
 systemProp.buildAndroidWithArtifacts = true
 systemProp.buildJavaWithArtifacts = true
</pre>


Feel free to put junk in USERNAME/PASSWORD. Unless you are publishing to artifactory you don't need it.

I put these values in my global gradle settings file (e.g. on windows this is c:\users\USERNAME\.gradle\gradle.properties) and go on with my day.

# How to manage and configure our Windows Azure Maven Server

These are instructions for configuring your local Maven environment so you can successfully upload to our Maven repository.

1. Log in to Artifactory via a web browser and then:
 * click on your user name (upper right corner of screen)
 * click on Maven Settings
 * click on Generate Settings

1. Go to c:\Users\[username]\.m2 and create a file called settings.xml and put in the content you got from the previous step

1. Go back to our Artifactory website (hit cancel if necessary to get rid of the maven settings) and:

 * Click on your user name (upper right corner) again
 * Enter your Artifactory password in current password and hit unlock
 * Copy the value in the Encrypted Password field

1. Go back to settings.xml and under the <server> entry with id snapshots and id central (e.g. do this twice) add a xml element <password></password> and copy into it the encrypted password you got in the previous step as is

1. Now load up the project in Intellij and open the Maven Projects dialog and double click on the parent (root)/deploy.
