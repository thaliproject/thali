---
title: Platform Checklist
layout: page-fullwidth
permalink: "/PlatformChecklist/"
---

This document attempts to capture our generic requirements to run Thali on a platform. We use this checklist to evaluate potential platforms and to help guide our development efforts on selected platforms.

# Our basic app model
Thali is really a set of capabilities including PouchDB, mutual SSL auth, peer to peer communication/mesh and Tor. We provide these as sets of libraries that an application developer can use to write their app. We are very opinionated about how apps get written and look specifically for them to be written using Cordova (if they have a UX) and Node.js. We depend on having PouchDB with LevelDB, openssl crypto (or equivalent), the ability to work with the platform's native code so we can implement peer to peer logic, etc.

These requirements therefore are an attempt to capture all the things necessary to provide the capabilities on a platform that our libraries depend on.

In the longer run we would like to provide centralized Thali functionality on a platform so that apps can share functionality like only having to run a single instance of node.js and only a single instance of the Tor onion routing proxy. It also turns out that in most cases having a unified peer to peer framework has many battery and simplicity advantages so ideally we would provide a centralized version of that as well.

In the super longer run we dream of moving to a model where Thali apps aren't compiled apps in any normal sense at all. Rather they are really just web pages but web pages that have a concept of identity (via mutual SSL auth) and a long running life with synch.

But one step at a time. The first step is to let devs write stand alone apps that can include Thali functionality. That is where this document mostly focuses for now.

# App store compliance
Requirement AAA - When we are on a platform that has a dedicated app store our software infrastructure MUST meet app store guidelines.

For example, there are some things on the Android platform that would make life easier if the device is rooted. But we can't depend on rooted devices when we put an app in Google Play. As such, however we solve the problems we need to solve for Thali they can't require us to root the Android device as it would keep us out of the store. To be fair there are other reasons not to depend on rooted devices but that's another story.

Requirement AAB - We MUST explicitly define which versions of the platform we intend to support and provide the reasoning for why.

Modern platforms are fast moving animals and especially in the P2P space we are finding new important features showing up all the time. This means that we will often decide not to support older platforms because it's just not worth dealing with all of their feature outages. To address this tradeoff we need to explicitly identify which platform versions we intend to support and explain what the criteria were for making the choice.

# Node.js API Compliance Requirements
Requirement AAC - We MUST define how we will support Node.js 0.10.x on the targetted platform. Where there are limitations on our support those limitations MUST be enumerated and justified.

Requirement AAD - We MUST define how we will eventually be able to support Node.js 0.12.x and subsequent releases on the targetted platform.

Our base platform is Node.js. That is the core of Thali's functionality. So we must support all the Node.js 0.10.x's APIs. It is understood that some of the platforms we target may not be able to properly implement all Node.js APIs. How many APIs we lose will be part of evaluating how much we want to support that platform.

Requirement AAO - We MUST be able to use NPM as-is to manage all pure javascript node.js modules during development.

With the exception of native add-ons, all pure Javascript packages must 'just work' with NPM during development time. Since we are building applications our expectation is that we will essentially freeze those packages in Node_Modules and then put them into the app. The packages are explicitly only expected to be updated when a new version of the app is deployed, not dynamically in place.

Requirement AAE - We MUST define what steps are necessary for existing Node native add-ons to function on the platform.

There are two different aspects to a native add-on. One aspect is the add-on's expectations about the Node.js platform, e.g. what the APIs are as well as how to set up the compile environment and support standard tools like node-gyp. The other aspect is the native add-on's own unique code requirements for the platform that have nothing to do with Node and everything to do with what the add-on is trying to do. This requirement just looks at the first aspect.

# Processes and radio requirements for Node.js
Requirement AAG - The platform MUST support enabling node.js to run based on network and time triggers in the background.

Thali's core scenarios all require synching data while running in the background. This means that we have to understand what are the limitations on the platform for running Thali in the background. At a minimum we need to be able to activate node.js in response to network events (enumerated below) as well as timer events.

Requirement AAI - The platform MUST support triggering node.js using local radios for peer to peer communication with all Thali supported platforms.

This is an area we are spending a lot of time on. For example, we have a [whole investigation](/AndroidP2P) just on how to handle peer to peer communication using local radios (BLE, Bluetooth, Wi-Fi, etc.) on Android. When we want to add a new platform to the mix we have to understand how it will work peer to peer both with other instances of the same platform as well as all the other platforms we care about. For example, we know at minimum we want to run on Android, iOS and WinRT. So any time we evaluate a platform we have to figure out how it will handle peer to peer communication between itself and all those other platforms.

Requirement AAJ - The platform MUST support hosting node.js on localhost and enabling local apps to wake node.js up when they attempt to talk to node.js over localhost.

Requirement AAK - The platform MUST support allowing node.js to expose an externally routable IP endpoint over which it can receive requests from any connected networks (e.g. endpoints connected to over cellular, Wi-Fi infrastructure, BLE, Bluetooth, Wi-Fi Direct, etc.).

Requirement AAL - The platform SHOULD support allowing node.js to park on an externally reachable port, go to sleep and be awakened whenever a request is received. If this requirement cannot be met then an explanation for how requirement AAK will be met MUST be provided.

Requirement AAN - We MUST identify if the platform supports running our Node.js plugin as a singleton, even when it's being activated by network and timer triggers.

Requirement AAM - For platforms that do not support long running processes/threads we MUST identify how long it takes from when a network/timer trigger is received and when the node.js process can be expected to be fully operational.

Some platforms support long running processes. Others however do not and will only start a process when an event occurs and then shut it down after the event is handled. We have to understand the costs in time for this behavior. Some platforms apparently have issues guaranteeing singletons. In other words rather than having a single process that all events get queued up for they will potentially start separate processes, running in parallel, for each event. LevelDB specifically and PouchDB in general expect to be the only process touching the underlying store. So if we run two processes in parallel we can end up with data corruption. As such we have to understand when a platform doesn't support singletons so we can put mutexes and such in place.

Requirement AAR - The platform MUST provide us with a mechanism to determine when we have Internet connectivity.

Requirement AAR is driven by our need to upload information to the cloud (e.g. "Ohh look, the Internet has shown up, go synch to the cloud!) and also switch our Peer to Peer functionality to the Internet when it's available (e.g. "Ohh look, the Internet has show up, go synch to our peers over Tor or equivalent!").

# Node.js library requirements
Requirement AAP - The platform SHOULD support compiling and linking PouchDB so we can use LevelDown as part of PouchDB. If LevelDB cannot be used then an alternative supported by LevelUP that meets our perf/memory/storage requirements MUST be identified.

PouchDB is the beating heart of Thali and LevelDB is the preferred database for PouchDB on Node.js. At this point we largely assume that LevelDB will run everywhere which is why we haven't defined exactly what our storage requirements are. If it comes to the point that we need to consider an alternative then we will have to be more specific. But generally if it works as well as LevelDB then it should work for us.

Requirement AAQ - The platform MUST support calling OpenSSL/LibreSSL from inside of node.js.

The real requirement here is that we need to be able to do the following at run time:
* Generate public/private keys with variable key sizes using cryptographically secure random number generators for RSA and various flavors of Eliptical Curve.
* Store the public and private keys in a format we can use for crypto operations, typically PKCS12.
* Generate X.509 certificates both to sign a leaf stand alone certificate and to sign a cert chain.
* Validate and Parse X.509 certificates to extract information about the keys in the cert chain
* Be able to present certs to be used with TLS for outgoing client and server side authentication
* Be able to parse/validate certs used with TLS for incoming client and server side authentication

Our current approach is to use OpenSSL for this. It has been suggested that perhaps instead we should expand the crypto library in Node.js to handle this since OpenSSL is already compiled into Node.js. This isn't necessarily a bad idea but we really don't want to create our own custom version of Node.js on our own. So in general we just look for OpenSSL support.

Requirement ABE - The platform MUST support calling out to the host platform's local toast/notification mechanism.

Since we primarily run in the background it is often the case that upon receiving certain kinds of data by synch we will need to notify the user so they can respond.

# Cordova requirements
Requirement AAT - For platforms that support a display it MUST be possible to deploy our applications to the platform using Apache Cordova.

Requirement AAU - For platforms where Cordova is relevent it MUST be possible to include Node.js functionality in the Cordova application using a Cordova plugin.

We are very opinionated on this one. We are explicitly saying that the preferred way to write applications with a UI is via Cordova and its webview. This means we can write our front end code using HTML+JS+CSS and then drop in a plugin which contains the node.js files.

Requirement AAV - The platform MUST support enabing Cordova to securely communicate with the local node.js server being run as a Cordova plugin in order to exchange information such as what port the node.js server is listening on.

The real issue here is that when the local node.js server is running on localhost we can't be sure ahead of time what port it will get. In the worst case we can just have the node.js server write to a local file and the Cordova bridge can use that as a poor man's RFC.

Requirement AAW - The platform MUST support a WebView where either the Cordova bridge can intercept and re-write all outgoing HTTP/HTTPS requests or where the WebView allows us to provision our own self signed cert in the WebView's local CA store (not the global store).

This requirement is driven by our needs to secure communication between the Cordova WebView and the local Node.js server being run as a Cordova plugin. Please see [here](/SecuringCordovaAndNodeJs) for the awful details driving this requirement.

# Tor Requirements
Requirement AAX - The platform MUST support running the Tor Onion Proxy.

On most platforms the Tor Onion Proxy (OP) is a self standing binary that is run from the command line. See, for example, a [library we published](https://github.com/thaliproject/Tor_Onion_Proxy_Library) that automates launching the OP on Android, Linux, OS/X and Windows. Once launched the proxy runs on its own process where it listens for incoming SOCKS requests that it will then forward to the Tor network. It also can host hidden services and forward requests it receives for a hidden service to a port on localhost.

# Thali Device Hub (TDH) requirements
Our general belief is that we first have to get the previous requirements running before we are ready to deal with the TDH. In general technology, we suspect, works best when incrementally adopted rather than requiring everyone to swallow it whole. While the TDH and it's ability to provide secure centralized storage and discovery for all data, especially identities, is important for Thali's long term vision we need to get there first.

We suspect the path starts by individual applications adopting bits and pieces of the Thali libraries. As more and more apps use pieces of the Thali infrastructure for their own local needs, so will grow the need to start making it all work together. Therefore these requirements naturally take a second place to those above.

Requirement ABA - The platform MUST support enabling applications to discover if there is a TDH on the platform and to discover how to talk to it via HTTPS over localhost.

Requirement ABC - The platform MUST support enabling applications to prompt the user to install the TDH if it is not present.

Requirement ABD - The platform SHOULD support enabling downloading and installing the TDH from another device rather than just from the platform's application store.

Requirement AAY - The platform MUST support a long running TDH providing services such as data storage over HTTPS on localhost.

This mostly has to do with background execution. If an application needs something from the TDH it needs to be able to rely on the TDH being available. So there has to be a way for applications on the platform to wake the TDH up if it's not running.

Requirement AAZ - The platform MUST support the ability for an application to be registered as a handler of a particular data type (like calendaring) and for the TDH to discover who that application is.

Requirement ABB - The platform MUST support the ability for the TDH and another application to securely exchange identity information.

In practice we don't want the TDH doing everything. It should really just be handling data storage, synch and identity. So, for example, we could have a local application that handles calendaring. The calendaring application should store it's data in the TDH. Other applications with the right permissions (if relevant to the platform's design) can read calendaring data out of the TDH but only the calendaring application will have write rights to the calendaring data in the TDH. That way other apps that want to change the calendar will go through the calendaring app rather than changing data under it in the TDH. This isn't perfect since most TDHs will be synching with other TDHs in the user's personal mesh so data can still change underneath it. But regardless we need to know who the calendar app is so we can give it unique permissions to calendaring data.

In addition we need a way for applications to know if they are really talking to the TDH and vice versa.

# Last used requirement ID
Note: Requirement IDs are meaningless strings. They have no inherent ordering. The only reason we hand them out in sequence is because we are doing it manually and want to avoid accidentally assigning the same ID to two requirements. Also note that if requirements are removed this will result in some IDs in the sequence being missing. Since the IDs are not inherently meaningful this shouldn't matter.
ABE
