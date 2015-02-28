---
title: Platform Checklist
layout: default
---

This document attempts to capture our generic requirements to run Thali on any platform. We use this checklist to evaluate potential platforms and to help guide our development efforts on selected platforms.

# Our basic app model
Thali is really a set of capabilities including PouchDB, mutual SSL auth, peer to peer communication/mesh and Tor. We provide these as sets of libraries that an application developer can use to write their app. We are very opinionated about how apps get written and look specifically for them to be written using Cordova (if they have a UX) and Node.js. We depend on having PouchDB with LevelDB, openssl crypto (or equivalent), the ability to work with the platform's native code so we can implement peer to peer logic, etc.

These requirements therefore are an attempt to capture all the things necessary to provide the capabilities on a platform that our libraries depend on.

In the longer run we would like to provide centralized Thali functionality on a platform so that apps can share functionality like only having to run a single instance of node.js and only a single instance of the Tor onion routing proxy. It also turns out that in most cases having a unified peer to peer framework has many battery and simplicity advantages so ideally we would provide a centralized version of that as well.

In the super longer run we dream of moving to a model where Thali apps aren't compiled apps in any normal sense at all. Rather they are really just web pages but web pages that have a concept of identity (via mutual SSL auth) and a long running life with synch.

But one step at a time. The first step is to let devs write stand alone apps that can include Thali functionality. That is where this document mostly focuses for now.

# App store compliance
Requirement AAA - When we are on a platform that has a dedicated app store our software infrastructure MUST meet app store guidelines.

For example, there are some things on the Android platform that would make life easier if the device is rooted. But we can't depend on rooted devices when we put an app in Google Play. As such however we solve the problems we need to solve for Thali they can't require us to root the Android device as it would keep us out of the store. To be fair there are other reasons not to depend on rooted devices but that's another story.

Requirement AAB - We MUST explicitly define which versions of the platform we intend to support and provide the reasoning for why.

Modern platforms are fast moving animals and especially in the P2P space we are finding new important features showing up all the time. This means that we will often decide not to support older platforms because it's just not worth dealing with all of their feature outages. To address this tradeoff we need to explicitly identify which platform versions we intend to support and explain what the criteria were for making the choice.

# Node.js API Compliance Requirements
Requirement AAC - We MUST define how we will support Node.js 0.10.x on the targetted platform. Where there are limitations on our support those limitations MUST be enumerated and justified.

Requirement AAD - We MUST define how we will eventually be able to support Node.js 0.12.x and subsequent releases on the targetted platform.

Our base platform is Node.js. That is the core of Thali's functionality. So we must support all the Node.js 0.10.x's APIs. It is understood that some of the platforms we target may not be able to properly implement all Node.js APIs. How many APIs we loose will be part of evaluating how much we want to support that platform.

Requirement AAO - We MUST be able to use NPM as-is to manage all pure javascript node.js modules during development.

Requirement AAE - We MUST define what steps are necessary for existing Node native add-ons to function on the platform.

There are two different aspects to a native add-on. One aspect is the add-on's expectations about the Node.js platform, e.g. what the APIs are as well as how to set up the compile environment and support standard tools like node-gyp. The other aspect is the native add-on's own unique code requirements for the platform that have nothing to do with Node and everything to do with what the add-on is trying to do. This requirement just looks at the first aspect.

# Processes and radio requirements for Node.js
Requirement AAG - The platform MUST support enabling node.js to run based on network and time triggers in the background.

Thali's core scenarios all require synching data while running in the background. This means that we have to understand what are the limitations on the platform for running Thali in the background.

Requirement AAH - The platform MUST support talking to node.js/waking it up based on network events and time events.

At a minimum we need to be able to activate node.js in response to network events (enumerated below) as well as timer events.

Requirement AAI - The platform MUST support triggering node.js using local radios for peer to peer communication both with other instances of the platform as well as with all other Thali supported platforms.

This is an area we are spending a lot of time on. For example, we have a [whole investigation](AndroidP2P) just on how to handle peer to peer communication using local radios (BLE, Bluetooth, Wi-Fi, etc.) on Android. When we want to add a new platform to the mix we have to understand how it will work with the peer to peer world we already live in.

Requirement AAJ - The platform MUST support hosting node.js on localhost and enabling local apps to wake node.js up when they attempt to talk to node.js over localhost.

Requirement AAK - The platform MUST support allowing node.js to expose an externally routable IP endpoint over which it can receive requests from off platform.

Requirement AAL - The platform SHOULD support allowing node.js to park on an externally reachable port, go to sleep and be awakened whenever a request is received. If this requirement cannot be met then an explanation for how requirement AAK will be met MUST be provided.

Requirement AAN - We MUST identify if the platform supports running our Node.js plugin as a singleton, even when it's being activated by network and timer triggers.

Requirement AAM - For platforms that do not support long running processes/threads we MUST identify how long it takes from when a network/timer trigger is received and when the node.js process can be expected to be fully operational.

Some platforms support long running processes. Others however do not and will only start a process when an event occurs and then shut it down after the event is handled. We have to understand the costs in time for this behavior. Some platforms apparently have issues guaranteeing singletons. In other words rather than having a single process that all events get queued up for they will potentially start separate processes, running in parallel, for each event. LevelDB specifically and PouchDB in general expect to be the only process touching the underlying store. So if we run two processes in parallel we can end up with data corruption. As such we have to understand when a platform doesn't support singletons so we can put mutexes and such in place.

# Node.js library requirements
Requirement AAM - The platform SHOULD support compiling and linking PouchDB so we can use LevelDown as part of PouchDB. If LevelDB cannot be used then an alternative supported by LevelUP that meets our perf/memory/storage requirements MUST be identified.

PouchDB is the beating heart of Thali and LevelDB is the preferred database for PouchDB on Node.js. At this point we largely assume that LevelDB will run everywhere which is why we haven't defined exactly what our storage requirements are. If it comes to the point that we need to consider an alternative then we will have to be more specific. But generally if it works as well as LevelDB then it should work for us.



# Cordova requirements

# Tor Requirements










# Last used requirement ID
Note: Requirement IDs are meaningless strings. They have no inherent ordering. The only reason we hand them out in sequence is because we are doing it manually and want to avoid accidentally assigning the same ID to two requirements. Also note that if requirements are removed this will result in some IDs in the sequence being missing. Since the IDs are not inherently meaningful this shouldn't matter.
AAO
