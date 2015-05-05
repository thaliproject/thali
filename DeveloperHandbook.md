---
title: Developer Handbook
layout: default
---

# Introduction 

Welcome to Thali! So you want to learn more? Great!

# Read!
The first thing to do is read. Please start [here](ThaliAndCouch). You can probably skip most of the links except for [this one](nodeondevices) and [this one](ThaliAndIoT) which will drive all of our immediate work. With these three articles you should have the background you need to understand what we are up to.

# Sign up to follow us
Please go [here](WaysToContribute) and subscribe to our blog, to our mailing list, bookmark our backlog webpage, etc. Also make sure you go to our main projects and hit 'watch' on them in GitHub.

If you don't 'watch' the projects then you won't get notified when we have updates, new issues, etc. So please hit 'watch'.

# Blogging
We like to blog but most of us blog on our own blogs. But we would like to republish here interesting articles. So here are the instructions on how to use our blog.

1. Go to https://github.com/thaliproject/thali/tree/gh-pages/_posts
2. Create a new entry following the date format you see in the directory, the definition is given [here](http://jekyllrb.com/docs/posts/)
3. Make sure you put in the 'front matter' (just look at any of the existing posts, they all have it at the top) and then write up your blog article.
4. Keep in mind that your blog article will be automatically pushed to our twitter feed so please make the first sentence in the blog article something that will read reasonably well on Twitter.

That's it. The article will automatically be picked up by our blog feed.

# Process!!!!
We have 1 week sprints.

Monday - Sprint starts!

Wednesday/Thursday - Each dev is responsible for putting their proposed work items for the next sprint in the ready column of https://waffle.io/thaliproject/thali. Items in the ready column should be one week or less of work. And yes, this means that if you have some huge item assigned to you in the backlog we expect you to break off one week or less pieces that go to the ready column.

 Items should ideally result in working code. For example, let's say we are doing discovery. The first week we might just announce our location. The next week we might put in the crypto. The idea is to break things into steps that each result in some code that can run. And yes, we are willing to have things take longer. Experience shows that by ending each sprint with running code, even if this means creating additional mini-milestones, in the long run we are more likely to have good code.

Friday - On Friday we walk through all the items "in progress" and see how we did. Success means doing a demo showing the working functionality. The demo can just be test code. It doesn't need to be pretty. We then close all successfully completed items and review the items in proposed work. All the approved items are then moved to 'in progress' to start the new sprint.

# Dev Machine Set Up 
We need a bunch of software. We use IntelliJ Ultimate Edition although I'm told that the (free) community edition supports what we do just fine. What else you need depends on what platforms you are developing for. At a minimum you need node.js. Most people seem to en dup doing some Android work so you need the latest JDK, latest Android SDK, a local maven installation, gradle and a decent android emulator and/or device.

We have a bunch of instructions on how to get this software for Windows, see [set up for windows](SetupForWindows) but we have tested everything on Mac and Linux and it all runs just fine there as well.

NOTE: EVEN IF YOU AREN'T RUNNING ON WINDOWS STILL READ THE [set up for windows](SetupForWindows) BECAUSE IT CONTAINS IMPORTANT CONFIGURATION INSTRUCTIONS THAT APPLY TO ALL PLATFORMS

# Git 
The [Thali Guide to Git](ThaliGuideToGit) has a bunch of information about how we use git but anyone who has used git regularly isn't going to find anything new there.

But please, DO NOT SUBMIT COMMITS DIRECTLY TO THE DEPOT! Fork and then submit PRs from a branch on your fork. The *only* exception to this rule is update to gh-pages for the website. Those (and those alone) may be pushed directly to the depot.

# Notes on adventures in node.js land
## Building our Javascript Projects 

Right now we are using stock PouchDB. We just keep this section around if we have to remember how we used to build our own version.

[Configuring PouchDB](ConfiguringPouchDB)

## How to debug PouchDB tests in Node.js and Intellij
I wanted to debug the tests in PouchDB as part of a PR. The main problem I ran into is that I use IntelliJ as my IDE and I needed a way to run PouchDB's mocha tests. Normally this is handled easily by just executing ./bin/test-node.sh which handles all the details. The good news is that what test-node.sh does is very straight forward and easy to set up as a test in IntelliJ. Except.... it turns out that in the tests directory there are tests both for node.js and tests for the browser. test-node.sh works around this by providing a test file path that ends with test.*.js where all files that match that pattern are guaranteed to be safe for node.js. The shell then expands the wild card into a set of files and then node/mocha gets called. The issue is https://youtrack.jetbrains.com/issue/WEB-10067 which doesn't support wild card expansing of files when specifying the test directory. To work around this here is what I do.

1. Run -> Edit Configurations -> Green Plus -> Mocha
2. Set Node Interpreter (if not already set) to /usr/bin/node
3. Set working directory to the pouchdb root directory from git
4. Set Mocha package to the path for the working directory plus /node_modules/mocha
5. Set extra Mocha options to "--timeout 50000 --require ./tests/integration/node.setup.js --grep test.replication.js" (obviously grep should be set to whatever you need, in my case I was running replication tests)
6. Set test directory to working directory plus /tests/integration/ (MAKE SURE TO END WITH A FINAL SLASH)
7. Do NOT check 'Include sub directory"
8. Now hit OK
9. Now get out your favorite editor and open up working directory plus node\_modules/mocha/bin/_mocha
10. At line 23 (right after the initial var declaration with all the dependencies) insert the code below

```Javascript
/*
 Awful hack for which I will burn in hades for eternity. But it lets me run the pouchdb node.js tests
 */
var lastIndexOfArgv = process.argv.length - 1;
process.argv[lastIndexOfArgv] += "test.*.js";
var globMatchedFiles = glob.sync(process.argv[process.argv.length - 1]);
process.argv = process.argv.slice(0, -1).concat(globMatchedFiles);
```

This code specifically extends the last argument to add the wildcard 'test.*.js' and then uses glob to extend it to a list of matching files and then adding those to the args. This is an awful hack. The right way to do this would be to add a new argument to mocha specifying a wildcard for files to be processed (which is different than grep, that is applied AFTER the list of files is chosen). But honestly I don't care. I just need this one thing to work for PouchDB so I can debug.

## How to get better pull replication perf out of PouchDB in Node.js
Assuming that [my pr](https://github.com/pouchdb/pouchdb/pull/3015) gets accepted then it becomes possible to get reasonable pull replication perf with PouchDB. The logic that PouchDB uses is that it collects B GET requests into a batch. With the PR the entries in the batch will be spread across C parallel connections. By default B = 100 and C = 5. But in heavy replication those numbers suck. Especially because of a bug in Node.js 0.10 that causes it to be extremely aggressive in garbage collecting connections which in practice means all TCP connections are closed between batches. So one wants a bigger batch size to increase the number of requests that use the same pool of connections (before they get recycled at the end of the batch) and one wants more connections in the connection pool because real world testing shows that 15 is much better than 5 connections (note that after 15 we didn't see as good a perf increase). 

To set the batch size one has to pass in an option 'batch_size' to replicate.

To set the connection pool size one has two options. One option is to globally set 'http.globalAgent.maxSockets' and 'https.globalAgent.maxSockets'. These require('http') and require('https') respectively. Another way to do this is to pass in {ajax: {pool: {maxSockets: 15}}} to the PouchDB constructor for a DB. This option CANNOT be passed into the replicate method. It has to be passed into the PouchDB constructor used to create the database instance.

There is however another complication. The default timeout for node.js ajax requests is 10 second. In practice 1000 batches * 15 connections can push over that limit (because the time the request sits in the queue is considered part of the time out, not just the time the request was outstanding on the wire). So depending on your usage profile you have to play around with the timeout. The way to set this is via the ajax option passed into the PouchDB constructor. E.g. {ajax: {timeout: 30000, pool: {maxSockets: 15}}}.

In node.js 0.11 the aggressive connection recycling is supposed to go away. If true then it means that it's safe to use smaller batch sizes and therefore not worry about the timeout. So at that point the only thing one should have to set is how many connections are in the pool. So let's hope we get to node.js 0.12 soon.

## How to use our custom PouchDB with our node.js project
We often seem to be running custom version of PouchDB so it's useful to know how to link things together.

1. Go to where we have cloned our pouchdb repro and run 'npm link' in that directory
2. Go to the node_modules directory which already contains pouchdb where you want to hook in the custom pouchdb and run 'npm link pouchdb'
 3. An issue I ran into is permissions because of where I happened to have the code I wanted to link to. Since I'm using windows I had to open a git bash window as an admin and then run 'npm link pouchdb'.
3. Anytime you refresh (such as with npm install pouchdb-server, in my case) the parent project the link to pouchdb gets lost and has to be re-created.

## How to get debug statements to output in node.js using IntelliJ
Go edit your configuration and set Environment variables to have the name "NODE_DEBUG" and the value equal to a comma separated list of the 'require' values you want to output debug for. For example "request".

However the default debug package that folks like PouchDB as well as Thali use require a different variable, "DEBUG". In the case of pouchdb one wants a value such as "pouchdb:\*" because pouchdb actually breaks down its debug targets. For Thali, at least right now, one can just say thalinode. So something like "pouchdb:\*, thalinode" should work.

## How to run PouchDB Integration tests against IE locally
When running tests locally while developing PouchDB we need to test things in IE, e.g. navigating IE to http://127.0.0.1:8000/tests/integration/ etc. Unfortunately CORS requests to localhost pretty much fail by default. This is a [known bug in IE](http://stackoverflow.com/questions/22098259/access-denied-in-ie-10-and-11-when-ajax-target-is-localhost). The work around is to go to internet options -> Security -> Trusted Sites -> Sites and then disable 'Require server verification (https:) for all sites in this zone) and then put 'http://127.0.0.1' into 'Add this website to the zone' and then add.

## How to run the PouchDB perf tests on our existing node.js Android system
I wanted to benchmark our existing node.js Android system against JXCore and decided to do it by running the PouchDB perf tests. I set up the tests so that both the node code that runs the test and the remote server are running on the Android device. To make this work I took our existing Cordova plugin and:

1. Went to the directory that contains node_modules
2. Ran "npm install express pouchdb memdown express-pouchdb"
3. Went into node_modules/pouchdb and issued "npm install" (this will put in place the dev dependencies we need to run the perf tests)
4. Went inside of pouchdb/tests/performance/index.js and replaced:

```Javascript
var PouchDB = process.browser ? window.PouchDB : require('../..');
```

with

```Javascript
var PouchDB = require('../..').defaults({db: require("memdown")});
```

I also edited in a new service which when called runs:

```Javascript
var express = require("express");
var pouchDBApp = express();
var PouchDB = require("pouchdb");
var InMemPouchDB = PouchDB.defaults({db: require("memdown")});

// Set up our test server
pouchDBApp.use("/", require("express-pouchdb")(InMemPouchDB, {mode: "minimumForPouchDB"}));
// Node tests by default will look at http://localhost:5984
pouchDBApp.listen(5984);

var eek = require('pouchdb/tests/performance/index.js');
```

The PouchDB perf results will be automatically output to the Android log.

## How to build and deploy JXCore for Android
The core depot for JXCore's Android code is [here](https://github.com/obastemur/jxcore-android-basics). However it's designed for eclipse and not for IntelliJ. I had to use Intellij 14.0.3 specifically to import the project and even then I ran into issues. We have my own [fork](https://github.com/thaliproject/jxcore-android-basics) that works in Intellij. See the first commit that we made to understand what we had to do in order to get it to work in IntelliJ.

### Hopefully you don't need to do this
When you clone our fork if you are using Windows you are going to have a problem because we need to use the NDK and it doesn't work all that well on Windows. The way I have been working around this is that I have a Linux image in a VM with a shared folder. I put the clone into the shared folder. I then go into Linux, into jxcore-droid and run android-ndk-r10d/ndk-build to hook things together. BUT!!! You may not need to do this. The binaries are already compiled and (sigh) checked in. So it might 'just work' without further effort.

### How to get a newer JXCore binary
The downside to jxcore-android-basics is that it ships with the jx core binaries which means if you want to run later binaries you have to set things up yourself. The way to do that is to:

1. Get our your favorite Linux image (I use Elementary OS running in Virtual Box)
2. Following JXCore's [instructions](https://github.com/jxcore/jxcore/blob/master/doc/Android_Compile.md), they worked great for me on Linux (not so much on Windows). This builds JXCore. Remember the path to the project.
3. Get a coffee or a tea, maybe a magazine, that compile takes forever.
4. Go to jxcore-droid/jni/Android.mk which contains a value JXCORE_OUT_ANDROID that I needed to point to the jxcore/out_android/android/bin/ sub-directory created in step 2.
5. Now run android-ndk-r10d/ndk-build in the root of the Android project we want to use JXCore in.

