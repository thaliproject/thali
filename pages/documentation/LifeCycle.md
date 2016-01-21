---
title: Life Cycle
layout: page-fullwidth
permalink: "/LifeCycle/"
categories:
    - documentation
header:
   image_fullwidth: "header.png"
---

This document outlines some basic issues regarding life cycle managing for our various platforms.

## Android

JXcore runs as a thread in Android. Threads have an odd life cycle in Android, they basically run forever. It doesn't matter what state the app is in. The only way the thread goes away is if the process is killed. So this means we basically can ignore life cycle issues in Android for the moment. Of course the new Doze and Standby modes in Marshmallow may complicate that. We need to investigate, see [Issue #413](https://github.com/thaliproject/Thali_CordovaPlugin/issues/413).

If the process is killed then the JXcore thread goes with it. In theory this should be o.k. It really depends on PouchDB and LevelDB. LevelDB normally does non-blocking writes using the system write() APIs. In theory these should survive a process getting killed as they are kernel level activity. But who knows what Android actually does? Some day I hope we get to [Issue #450](https://github.com/thaliproject/Thali_CordovaPlugin/issues/450) and find out.

## iOS

See [here](https://github.com/jxcore/jxcore-cordova/issues/133#issuecomment-171800533) for the gory details. But the upshot is that we have to disconnect all client connections, shut down all servers and kill all incoming connections to servers (alas in Node.js just closing a server doesn't kill existing connections so you have to track and kill them manually) as soon as we get an onPause event from JXcore-cordova. This means that when we get an onResume event we have to start up everything again.

To deal with this we need to have a global start() method that encapsulates all the actions needs to set up the Thali environment and a global stop() method that tears everything down. On Android we would call start() once and never call stop(). On iOS we would call start() when we get onResume and stop() on onPause.

We also need to create our own subclasss of HTTP (for discovery) and HTTPS servers where we automatically track all incoming connections and then create a "globalClose" method that will kill all those connections as well as close the server.

Classes that open HTTP connections will need to have a way to force them close everything. Typically we just open connections for discovery and for replication so this should be reasonably easy to handle.
