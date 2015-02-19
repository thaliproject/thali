---
title: Exploring Android to Android P2P
layout: default
---
This is an investigation of getting multiple Android handsets, 4.x series, to successfully talk to each other.

# Acknowledgements
Lots of the data here is taken from Michael Rogers of the [Briar Project](https://briarproject.org).

# Warning
This document assumes the reader is reasonably familiar with BLE, Bluetooth, Wi-Fi Infrastructure mode and Wi-Fi Direct. The [mesh mess](http://www.goland.org/thalimesh/) article provides some of the background.

# Code
We are writing code to try and answer the questions below in conjunction with Michael Rogers. Our fork is available [here](https://github.com/thaliproject/android-peer-discovery/).

# Scenario - Group Chat
Users A, B and C all want to communicate to each other for a group chat using their Android devices. There is an assumption that the users have authenticated to each other and that their communications are secure. There is also an assumption that communication will work even when there is no Internet connectivity.

I am going to assume that there is an identity exchange. It could be done locally over something like Bluetooth or remotely over email. But we assume there is a point of exchange where configuration data can be shared. The real focus of this investigation is how can we enable handsets to communicate when they have something to say?

It turns out that there is a wide range of connectivity scenarios and we need to address them all.

## Everyone is on the same local Wi-Fi infrastructure
In this scenario users A, B and C all have their devices registered to either the same access point (AP) or to APs that are part of the same local network.

In theory we could then allow the devices to find each other by using either broadcast or multi-cast via a protocol like mDNS or SSDP and then establish point to point connections to allow communication.

In practice many networks block broadcast and multi-cast as well as disallow devices to directly communicate to each other using local addresses over the local network. This blocking is done ostensibly for security reasons so that members of the same network can't just scan and attack each other.

This means we should experiment with mDNS and SSDP support for discovery. But we have to accept that it likely won't work and we need to have a fallback. If the local network is connected to the Internet then we can potentially use an external infrastructure to enable discovery and connectivity. We typically use TOR for that.

When dealing with a locked down Wi-Fi infrastructure we can also ignore the Wi-Fi infrastructure and treat this like the ad-hoc asynchronous mesh scenario below.

## There is no local Wi-Fi infrastructure
In this situation the devices are just 'hanging out' without any kind of centralized Wi-Fi infrastructure. So we can only communicate using local radios. We assume that users are moving around and won't always be in range of each other. This is different than the [FireChat](ExperimentWithFireChat) scenario where the assumption is that if user A can't see C then B can and the message can be relayed. In our case we assume there are long periods of time when users are not in range of each other. Therefore our group chat scenario is essentially asynchronous. Someone can add an entry to the group chat but there will be an indeterminent delay before anyone else sees their message. We therefore assume we are using asynchronous opportunitistic meshing.

What we mean is that two devices, both presumably in stand by mode in their owner's pockets, will detect each other when in radio range and then try to exchange updates. There are three radio technologies that just about all Android devices have - BLE, Bluetooth and Wi-Fi. We need to explore all of them to see if alone or in combination they can be used.

# Technologies to investigate
## Wi-Fi Direct
In theory this is the answer to our non-Internet connected discovery scenarios. In practice, not so much.

The main issue is that Android requires user confirmation before joining a Wi-Fi Direct group. This is a real challenge when synching is supposed to happen when the device is in the user's pocket. On the other hand, if one only has to join a group once and then can reconnect without explicit joining in the future then we might be able to live with this experience.

It seems like Android refers to discovering other Wi-Fi direct groups as peer discovery while it discusses using SSDP or mDNS over Wi-Fi direct as service discovery. I think what's going on is that since SSDP and mDNS both use UDP Android is at least willing to allow those protocols to run even over a Wi-Fi direct connection that has not been confirmed. While to run a TCP connection (or potentially even a UDP connection over a port other than the ones used for SSDP and mDNS) one has to get user confirmation.

For our Wi-Fi Direct investigation what we need to know for Android is:

* Can we discover peers while in stand by mode?
* Can we be discovered by peers while in stand by mode?
* Can we discover services while in stand by mode?
* Can we be discovered by peers looking for a service on our device while in stand by mode?
* How long does a device have to be nearby before we will discover it?
* What is the power consumption for supporting advertising ourselves and being discovered while in stand by mode?
* How long does it take from discovering a device to delivering the first byte?
* What is the effective bandwidth between devices?
* Once we join a particular peer and get user confirmation, do we have to get user confirmation for future joins?
* There are at least two race conditions that Michael found in the API, we need to identify them and see if there are more.
* Can the client use Wi-Fi Infrastructure and Wi-Fi direct at the same time? It's supposed to work, but we should check.

## Bluetooth
Bluetooth technically has a two phase life cycle. In the first phase one pairs two devices together. In the second phase one discovers when a paired device is near by and connects.

By default in Android when switching a device into discovery phase one has to get approval from the user and then a count down begins during which time the device will make itself discoverable to other Bluetooth devices. The reason for the count down is that discovery is apparently battery intensive. More modern releases of Android theoretically support an unlimited discovery time but some devices (Sony Xperiod tipo) have bugs that limit the discovery time.

We aren't super fans of normal Bluetooth discovery because it requires explicit user interaction but we could live with it if we have to.

Another complication of discovery phase is that it appears that while in discovery phase one can't connect using the Bluetooth radio. In other words one can be in discovery phase or in connection phase but not both.

There is a fairly nasty work around to all of this in Android land. It turns out that if one somehow knows another Bluetooth radio's address and service UUID (which we can set) then it's possible to actively ping for that device's presence. One literally has to keep calling into the Bluetooth library with the UUID asking 'is it there'? Because of how Bluetooth works it's likely that this query doesn't actually cause any radio activity. Bluetooth has its own protocol for detecting other devices and using that data to update a DB and it seems like the Android api is just querying that local DB. Or maybe not. This is something we need to measure.

Android also supports directly connecting to an unpaired Bluetooth device if one knows the devices address. This means that if we discover another device we can talk to it. The connection won't be encrypted but that's o.k. because we have our own encryption layer.

So we really need to do two different investigations.

Vanilla Bluetooth

* How ugly is the UX for pairing?
* How do we detect other paired Bluetooth devices when the device is in stand by mode? Can we register for notifications? Do we have to do some kind of polling?
* How long does a device have to be in range before we typically will 'see' it?
* How do we advertise ourselves to other paired Bluetooth devices when the device is in stand by mode?
* What is the power consumption for advertising ourselves and being discovered by other devices when in stand by mode?
* How long does it take from detecting another device until the first byte is successfully delivered?
* What is the effective bandwidth between devices?

Hacky Bluetooth 

* Can we use the polling mechanism to discover other devices when we are in stand by mode?
* How long does a device have to be in range before polling will 'see' it?
* How do we advertise ourselves to other Bluetooth devices who know our UUID while in stand by mode?
* What is the power consumption for advertising ourselves and being discovered using polling while in stand by mode?
* How long does it take from detecting another device until the first byte is successfully delivered?
* What is the effective bandwidth between devices? 

## Bluetooth Low Energy (BLE)
BLE is unfortunately not useful given our requirement that we run on 4.x Android devices. The problem is that for a device to be discoverable it must support the BLE peripheral profile. In Android 4.x there is only support for being a BLE consumer, not a BLE peripheral. So this means that an Android device can connect to a BLE peripheral but can't actually be a BLE peripheral.

In Android 5.0 this limitation was fixed. So in theory an Android 4.x device can detect an Android 5.x device but not vice versa.

We will eventually need to support BLE both because it has powerful advantages in terms of battery power that make it a great discovery mechanism for Android 5.x but also because it's about the only discovery mechansim that works well with iOS.

But for the moment we are focused on 4.x to 4.x communication and without the peripheral profile BLE just isn't useful.
## Wi-Fi Infrastructure Mode
Android provides APIs that let one automatically switch Wi-Fi Infrastructure Mode access points. This is potentially a nifty hack as it means we could do discovery of other devices using Wi-Fi Direct (without, apparently, user interaction) but then actually connect to the other device using Wi-Fi Infrastructure mode. This works because all Wi-Fi Direct Groups are also Wi-Fi Infrastructure Mode access points. There is a hitch however. All Wi-Fi Direct Groups have to have passwords and it appears that Android randomly generates those passwords. Without the password the client using Wi-Fi Infrastructure Mode can't access the server's Wi-Fi Direct Group.

Potentially we could advertise the passwords as part of service discovery over Wi-Fi Direct or we could use a different technology like Bluetooth to move the password.

To make things more complex the client's Wi-Fi Infrastructure Mode can only connect to a single access point (AP) at a time. So we have to disconnect whatever AP is currently being used and switch it to a different AP to communicate. Ick. This can cause all sorts of problems.

Also Michael said that he ran into issues where a device was both advertising itself via Wi-Fi Direct while accepting incoming connections from legacy clients. We need to investigate that more.

If Bluetooth or Wi-Fi Direct don't hack it though we may have to go down this route.
## TOR
We already know that TOR works just fine in Android land. Heck we have a whole [project](https://github.com/thaliproject/Tor_Onion_Proxy_Library) whose only purpose is to make this easy on Android. But the problem is that we really never did much if any perf or battery draw work. We really need to know:

* How long does it take, on average, to establish a hidden service listener?
* Once a hidden service listener is created how long does it take on average for a client to connect?
* What is the average latency for a connection from client to service?
* What is the battery draw for keeping a hidden service listener alive? E.g. using keep alives assuming an Internet connection is available?
* How hard is it to create a hidden service listener while the device is on stand by using a timer to wake up our service?

## mDNS vs SSDP
When we are on a Wi-Fi AP with other devices and if multi-cast is supported then we will want to perform discovery. But this brings up a bunch of questions such as:

* How do we detect when broadcast/multi-cast isn't supported?
* How long does it take to discover another device using mDNS vs SSDP once the device is available?
* How long does it take from discovery until the first byte is sent using mDNS vs SSDP?
* How much battery draw does it take to use mDNS vs SSDP when running in stand by for discovery purposes?
* Apparently some Android devices don't support multicast at all and won't warn you. How do we detect his?

## Wi-Fi Ad-hoc
Wi-Fi Ad-hoc is just plain no fun. It requires disconnecting from the Wi-Fi Infrastructure Mode AP completely and switching to a different Wi-Fi mode. To the best of my knowledge Android doesn't natively support switching to ad-hoc. One has to root the device first.
