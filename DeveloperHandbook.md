---
title: Developer Handbook
layout: default
---

# Introduction 

Welcome to Thali! So you want to learn more? Great!

# Read!
The first thing to do is read. Please start [here](ThaliAndCouch). You can probably skip most of the links except for [this one](ThaliAndIoT) which will drive all of our immediate work. With these two articles you should have the background you need to understand what we are up to.

# Sign up to follow us
Please go [here](WaysToContribute) and subscribe to our blog, to our mailing list, bookmark our backlog webpage, etc.

# Blogging
We like to blog but most of us blog on our own blogs. But we would like to republish here interesting articles. So here are the instructions on how to use our blog.

1. Go to https://github.com/thaliproject/thali/tree/gh-pages/_posts
2. Create a new entry following the date format you see in the directory, the definition is given [here](http://jekyllrb.com/docs/posts/)
3. Make sure you put in the 'front matter' (just look at any of the existing posts, they all have it at the top) and then write up your blog article.

That's it. The article will automatically be picked up by our blog feed.

## Building our Javascript Projects 

Right now we are using stock PouchDB. We just keep this section around if we have to remember how we used to build our own version.

[Configuring PouchDB](ConfiguringPouchDB)

# Dev Machine Set Up 
We need a bunch of software. We use IntelliJ Ultimate Edition although I'm told that the (free) community edition supports what we do just fine. What else you need depends on what platforms you are developing for. At a minimum you need node.js. Most people seem to en dup doing some Android work so you need the latest JDK, latest Android SDK, a local maven installation, gradle and a decent android emulator and/or device.

We have a bunch of instructions on how to get this software for Windows, see [set up for windows](SetupForWindows) but we have tested everything on Mac and Linux and it all runs just fine there as well.

NOTE: EVEN IF YOU AREN'T RUNNING ON WINDOWS STILL READ THE [set up for windows](SetupForWindows) BECAUSE IT CONTAINS IMPORTANT CONFIGURATION INSTRUCTIONS THAT APPLY TO ALL PLATFORMS

# Git 

The [Thali Guide to Git](ThaliGuideToGit) has a bunch of information about how we use git but anyone who has used git regularly isn't going to find anything new there.

# Notes on adventures in node.js land
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
