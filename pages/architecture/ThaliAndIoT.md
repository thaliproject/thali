---
title: Thali and the Internet of Things
layout: page-fullwidth
permalink: "/ThaliAndIoT/"
header:
   image_fullwidth: "header.png"
categories:
    - architecture
---

# Introduction
The Internet of Things (IoT) needs the web as much as anyone else does. But, perhaps not surprisingly, in many scenarios it specifically needs a peer to peer web. And where there is a peer to peer web, there is Thali.

By default I'm finding that most IoT scenarios involve things talking to the cloud. IoT systems leverage the cloud heavily for a number of reasons including having a centralized cloud makes issues from data synchronization to identity easier to deal with, gives cross device and even site data collection and data analysis and much more. However, a peer to peer system makes for an incredibly resilient network without a single point of failure, allows your devices to operate in an offline mode as a system rather than disconnected devices and gives you incredibly fast collaboration between the devices because the "show must go on", regardless of cloud availability.

This is where Thali and the peer to peer web come in. In building solutions for the peer to peer web Thali has had to deal with the hard problems that show up when one wants to remain functional even when the cloud is not available. It answers questions such as how to confirm identity or keep data in synch in a peer to peer manner.

By bringing Thali to IoT we enable IoT systems to retain their close connection to the cloud but to also remain fully functional even when the cloud isn't available.

The rest of this article explores exactly what libraries we need to build in order to Thali enable IoT and how we will use these libraries to build Thali itself.

# Terminology
* __General Purpose Computers (GPCs)__ - Computing devices intended as multi-purpose computing platforms, typically with a focus on human interaction. Examples include phones, tablets, laptops, PCs and even the cloud.
* __Things__ - Computing devices that are intended to be used for a single purpose associated with a physical artifact. Examples include sensor suites, manufacturing equipment, home automation controllers, etc. Note that the computers inside of things often are on par in power and capabilities with GPCs. The distinction between GCPs and things is driven not by their computational capabilities but by the intent for how those capabilities should be used.
* __Devices__ - The intersection of GPCs and things.

# Node.js Everywhere
As [previously mentioned](/NodeOnDevices) a key decision made in Thali land is that we are going to switch from Java to Javascript. The driver for this decision was the desire to have Thali code run literally 'everywhere'. However the belief is that only two languages truly run absolutely everywhere - C and Javascript. We picked Javascript.

But Thali does most of its work in the background, not in a web browser. So we need a packaging framework for Thali logic that is based around listeners and waiting for events. This is, of course, what Node.js is perfect for and as such we have picked Node.js as our core platform.

It so happens that Node.js is super popular amongst the IoT crowd. No, we really didn't know that when we picked Node.js. But now that we do, let's ride the wave!

But what's interesting is that our IoT customers don't just want Node.js on their maker boards. They also want them on their GPCs. The reason is that they all need software that runs on GPCs (usually phones) that can receive asynchronous notifications over whatever radio makes most sense (e.g. Bluetooth, Bluetooth LE, Wi-Fi, Wi-Fi Direct, etc.) and they want to write the logic to handle those notifications in the same language/environment they are using for their things. So the desire is to see Node.js everywhere, e.g. on GPCs (especially phones) as well as on things.

So it turns out that IoT needs Node.js running everywhere (including Android, iOS and WinRT) as much as Thali does.

# Cordova Everywhere
As mentioned above all the IoT customers we are talking with need to build apps for GPCs (especially phones) to interact with their things. Yes, they want to use Node.js to handle events, but they also want to use HTML 5/Cordova for the front end. In fact when we talk to them about the [vision](http://www.goland.org/html6packagedapps/) for Cordova+Node.js they get really excited and want to adopt it.

So yet again, we find Thali and IoT interests converging. Specifically, what we want to build is:

 * a Cordova plugin for Node.js
 * a Cordova build environment to run on desktops (most IoT customers are focused on Phones but they all need desktop as well)

# AdHocWireless Node.js Package
Our IoT customers need all devices (e.g. both GPCs and things) to be able to talk to each other. And they need to be able to do it over a variety of transports. They need GPCs and things talking to each other using Bluetooth, Bluetooth LE, Wi-Fi, Wi-Fi direct, Alljoyn, etc. And yes, [nothing new here for Thali](http://www.goland.org/thalimesh/). Interesting enough we also have scenarios that started off as IoT and ended up with needing peer to peer communication between GPCs.

So what we need is a library that will expose standard discovery and communication capabilities in node.js and then hook that up to whatever radios the device we are running on supports. In other words we don't just need to support things talking to GPCs, we need to support GPCs talking to each other.

The idea is that we would expose interfaces in node.js that let one:

* register for events when new peers are detected
* enumerate what peers are around
* receive standalone messages and streams from peers
* send standalone messages and streams to peers

We would then write plugins to support the various wireless technologies we care about on the platforms we can about it. Since this is an open source project other people can drop in their own plugins to extend the work under the same interface.

# HighLatencyMesh Node.js Package
When dealing with offline connectivity we run into a lot of issues with the limitations of wireless technology. Especially in doors. This means that to get a message from one device to another we need to relay through intermediate devices. Our first approach to this is a store and forward system. We would sync data from one device to another and let it relay itself to its final destination. Thanks to CouchDB's powerful sync capabilities we can even send the same data on multiple paths and not worry about them interfering with each other.

Depending on the storage and bandwidth of the devices involved we can either use a flood mesh (e.g. everyone has a copy of everything) or a directed mesh (where devices create an explicit path of devices to navigate their data using other devices as temporary relays). We expect to support both approaches.

For IoT scenarios where we need to relay through untrusted devices things get a big uglier but that hasn't been an issue yet. But if it is there are ways to even handle that using the replication stream work the PouchDB folks have been working on.

But the core library would handle publishing location information and extend replication in PouchDB to specifically understand that a replication is being relayed to a destination.

For battery based devices we will also need a module to specifically be smart about using radios in a battery smart way. The device can't afford to constantly be communicating. So we will need logic specifically to handle how often and how much to sync.

# PublicKeyIdentity

## Node.js Package
We want to use the Thali approach of allowing two devices to authenticate to each other and communicate security using X.509 and mutual TLS auth. This is all supported in node.js already but we really could use a library to handle a lot of the scut work on generating keys, storing them, configuring TLS mutual auth connections, etc.

## Cordova Plugin
This would be a Cordova plugin that would provide some standardized support for sharing public keys. I suspect our first sharing mechanism will probably be based on something like Bluetooth LE or Bluetooth using a shared code to authenticate the shared key. We ideally want to do this without actually pairing but we shall see. This actually isn't very secure but for many scenarios its good enough. Eventually will also work in our existing support for things like generating and sharing QR Codes.

# AclsAndGroups Node.js Package
We really need a library to let endpoints specify their access control policy using ACLs which are based on groups of public keys. This library would provide a simple implementation to store the ACLs and groups and run the ACL engine using the groups. And yes, in Thali style, it will all be backed by PouchDB (e.g. CouchDB on Node.js).

# Cloud integration
Those wacky IoT folks, as much as they love offline, still really love online and the cloud. They want an easy way to share data with the cloud. The great news is that so many people are already working on this part of the problem we can just surf on their effort. 

# So what's missing for Thali?
The stack of technologies listed here are awesome and get us about 80% of the way to Thali. But there are still missing pieces.
## Tor Support in Node.js
Right now our IoT customers are looking for solutions for offline, but for online they assume they will go straight to their cloud. That's fine as a start. At worst we'll build our own Tor support into Node.js. There are already Node.js packages to help.

But when I start talking threat models with IoT customers they are getting more interested in Tor. Imagine data from things flowing into the cloud. Now imagine an adversary who can't see what's in the data (it's encrypted) but can see how much data is being sent and the data's general shape. This is often enough to detect critical events like production peaks or disruptions. This is often extremely sensitive data that companies don't want in the hands of their competitors. So as traffic analysis threats are being better understood I'm finding customers getting more interested in Tor. I wouldn't be surprised if Tor support doesn't end up an IoT requirement.

## Thali Device Hub (TDH)
The TDH is the center of the Thali vision. It's a central store that apps can put all their data into and so share it with other apps on the device and with other devices. This puts users firmly in control of their data.

The TDH needs everything listed here and some more. It needs logic about local device security so it knows which apps should have access to which data. It needs to be super smart about battery management so it doesn't synch too often when on cell and can prioritize different types of data when bandwidth constrained. It needs logic regarding how to build a user's own personal mesh of devices and how to synch that. It needs logic regarding how to clean up data when storage becomes tight. You can browse around our [issue tracker](https://www.pivotaltracker.com/n/projects/1163162) to get a sense for just how much there is to do for the TDH. Before we dumped the Java codebase we were estimating that it would take 3 people about 6 months to finish everything. So it is a bunch of work but not a mountain of work.

But my hope is that we don't have to do it all at once. Initially we will build the TDH as a re-usable library that can be used by individual apps. Omitting multi-app sharing simplifies a lot of scenarios (at the cost of reducing utility). Then as we build up features we can finally try to release the central library with all the bells and whistles.

# Conclusion
Thali is about the peer to peer web and it turns out that IoT needs the peer to peer web to remain functional when the cloud isn't around. So we are going to use Thali's technologies to power up IoT!
