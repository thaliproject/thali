---
title: Bluetooth Primer for Thali Developers
layout: page
sidebar: right
permalink: "/bluetooth_primer_for_thali_developers/"
categories:
    - documentation
header:
   image_fullwidth: "header.png"
---
# Bluetooth primer for Thali developers
Anyone writing a Thali app that is going to run on Android has to deal with the peculiarities of Bluetooth. This knowledge will be necessary to write an effective implementation of the thaliPeerPoolInterface. This section is intended to provide an overview of the key parts of Bluetooth that matter to Thali.

## Bluetooth discovery and Bro Mode
Bluetooth supports a discovery service called SDP. This is effectively a server that runs on the Bluetooth device. It answers queries for a specific service. If the service is supported by the device then the SDP server on the device will respond with the information needed to establish a connection to that service. So Thali, for example, has its own SDP UUID for its service and any Bluetooth device that supports Thali will answer queries for that service with the appropriate connection information. In Thali's case this information will be a port over which the remote service can establish an unencrypted RFCOMM connection. RFCOMM is a protocol that provides TCP/IP like semantics. Communication is bi-directional and reliable.

The SDP server will only answer queries that are directly addressed to its MAC. So to talk to a Bluetooth device's SDP server one has to know the device's MAC. So this begs the question, how the heck does say Phone A find Phone B's Bluetooth MAC so it can send it a SDP query for the Thali service?

Before Android Marshmallow the answer was fairly straight forward. Phone A would discover Phone B over BLE. Then Phone B would send its Bluetooth MAC over BLE to Phone A who would then use the Bluetooth MAC to make a SDP query and establish the RFCOMM connection.

Unfortunately Google in Android Marshmallow now makes it impossible for phones to find out their own Bluetooth MAC addresses. To work around this we introduced Bro Mode. The gory details are available [here](http://www.goland.org/thalilocalp2psurvivingmarshmallow/#toc-Section-3). But the short version is that Bluetooth has the idea of a 'discoverable' mode. When the phone is in this mode it will announce its presence and its true MAC address to the whole world. So in our example if Phone B doesn't know its own address then it would need to switch itself to discoverable mode. This mode is a problem for two reasons. The most important reason is that the only way to switch an Android phone to discoverable mode is to get the user to hit OK on a system provided UX. So forget anything happening in the user's pocket. Second, being in discoverable mode eats battery.

To handle these situations we will have Phone A and B negotiate if they should enter Bro Mode over BLE. Only if the Thali app is in the foreground will Phone B request Bro Mode. If Phone B requests Bro Mode and Phone A (automatically subject to some battery limitations to keep phone A's battery from being eaten by too many bro mode requests) agrees then Phone B will trigger the system UX and if the user hits ok then Phone B will be in discoverable mode and can be seen by Phone A. Phone A will switch itself to 'discover' mode. Discover mode, which lets Phone A find other devices, also uses extra battery. But once Phone A is in discover mode it can see Phone B who is in discoverable mode. At this point Phone A will call the SDP server on Phone B and ask it for the Bro Mode service. The effect of doing this will be to let Phone A see the true MAC address for Phone B. Now Phone A will establish an RFCOMM connection with Phone B and send it its MAC address. Note that pairing was not needed here. So Phone A and B aren't paired. Only switching Phone B to discoverable mode was necessary. There was no visible UX for any of this on Phone A. So it could all happen while Phone A was in its user's pocket.

The only good news is that once Phone B knows its MAC address it never has to ask for it again.

But this explains why we have the Bro Mode interfaces and why developers have to deal with them. Sorry. But this is completely out of our hands.

## Piconets
Unlike shared band protocols like Wi-Fi, Bluetooth uses something called frequency hopping spread spectrum. In frequency hopping spread spectrum rather than always sending signals over the same frequency a device will hop from frequency to frequency, in Bluetooth's case 1600 times a second. This works because the 2.4 Ghz band is a 'band', as in, a collection of frequencies. Specifically between 2400 MHz to 2500 MHz and broken by Bluetooth into 79 channels. So each transmission hops from channel to channel within the band. 

To make this work there is a device called a master who forms a piconet. The master of a piconet decides what pattern to use in hopping frequencies. The idea being that if two or more piconets are run by two or more masters in the same physical location they will pick different frequency hopping patterns and thus reduce how often they interfere with each other. Frequency hopping also means that Bluetooth is less likely to interfere with or be interfered by other users of the 2.4 Ghz ISM band.
 
So if Phone A establishes a Bluetooth connection to Phone B then typically what will happen is that Phone A will create its own Piconet and invite Phone B to join. If Phone B does join then Phone A will tell Phone B what frequency hopping pattern is being used and it will also decide when Phone B is allowed to transmit on the Piconet. For design reasons a Piconet can have one master and up to 7 active slaves.

The amount of bandwidth allocated to a Piconet is fixed. So the more slaves there are in a Piconet the less bandwidth each member of the Piconet has available to them. This heavily influences how to design a thaliPeerPoolInterface. For example, let's say that a Thali application is trying to synchronize reasonably large amounts of data between peers. Now let's imagine that Phone A, B and C get within range of each other. Let's also imagine that Phones B and C both have data for Phone A. Given that we can't be sure how long the phones will be range (someone can walk away) we have to decide what's more important, getting one sync, say between A and B completed, or getting a little data from all syncs (e.g. allowing A to sync simultaneously with B and C) but possibly not finishing. If the former then the policy should restrict to only one sync at a time. If the later then the policy should allow for multiple parallel syncs.

This logic even applies in the case that Phone A and B both have data for each other. If they both simultaneously start to sync with each other the effective Bluetooth bandwidth will be cut in 1/2 because they are both sharing the fixed bandwidth of the Piconet.

So while, say, a text chat app probably wants to allow a lot of simultaneous sync's in order to ensure low latency propagation of messages a big database sync app probably wants to turn sync all the way down to just one connection at a time.

## Scatternets
Scatternets is just a phrase for Bluetooth hardware that supports being part of more than one Piconet at a time. Typically modern Bluetooth hardware supports at least being a master of one Piconet and a slave in another Piconet.

In theory Scatternets are great because they increase bandwidth. After all, each Piconet has a fixed bandwidth regardless of how many members it has so if one is a member of two Piconets then one should have twice the bandwidth!

Of course this brings us back to our shared antenna problem. Because there is just one antenna if a Bluetooth chip is part of two Piconets then it has to share time to be on those Piconets across the single antenna. And so, in practice, Scatternets don't increase bandwidth.

But they do increase complexity.

The reason has to do with a bias that is programmed into Bluetooth controllers. By default a Bluetooth chip always wants to communicate with other Bluetooth devices on its own Piconet of which it is the master. The reason for this is control. The typical Bluetooth scenario is that a phone is controlling say a headset and some speakers. The phone needs to make sure that the phone and speakers get a guaranteed minimum bandwidth or the sound on both devices could be interrupted. By putting the devices on a Piconet that the phone is the master of the phone can decide how much bandwidth each device gets. That is, the master of a Piconet has to give each device permission to talk, so the master can favor some devices over others. This allows the phone to make trade offs, such as if bandwidth is low due to high collisions with other transmitters the phone may prioritize bandwidth to the headset over the speakers.

But if the phone joins say a Piconet that the speakers are a master of and so does the headset then the phone isn't in control anymore.

Imagine, for example, that Phone A is the master of a Piconet that contains both Phone B and Phone C. Now imagine that Phone B decides it wants to talk to Phone C. In theory the most sensible way to configure this topology given antenna limitations if for Phone B to talk to Phone C over the Piconet controlled by Phone A. But that isn't what is going to happen. Instead Phone B will invite Phone C to join its Piconet. This will force both Phone B and Phone C to now be members of at least two different Piconets. This doesn't help anybody as it cuts everyone's bandwidth.

To complicate things further, when Phone B invites Phone C, Phone C will want to be master too for the reasons already discussed. So this starts what amounts to a leadership election where Phone B and C will settle between them who is going to be the master. But none of this logic ever takes into account the actual topology of the network. So Piconets proliferate.

About the only good news is that Bluetooth is smart enough that if Phone A has opened a link to Phone B and then Phone B wants to open a link to Phone A, they will do so on the same Piconet.

What this means for people building peer pools is that you really want to try and avoid having too many connections because they will cause more Piconets to be generated. Remember, a Piconet can only have 1 master and 7 slaves so even in an ideal case it probably doesn't make sense to support more than 7 devices simultaneously.

## Counting connections.

It's tempting to just count devices but that probably won't give the desired results. For example,
as already pointed out if Phone A opens a socket to Phone B and then Phone B opens a socket to Phone A that effectively cuts bandwidth in 1/2. This assumes both A and B are going to saturate their sockets with data. After all, they are all sharing the same fixed amount of bandwidth for a Piconet. So if we just counted devices then Phone A would count Phone B once. But in reality, in terms of available bandwidth, Phone B might need to be counted for one 'share' of bandwidth (if A is only talk to B or vice versa) but if both A and B are simultaneously sharing information then we need to count B twice, one for each direction of content it is part of, incoming and outgoing.

So this argues for not counting devices so much as counting connections, regardless of their direction (e.g. incoming or outgoing sockets).

It's worth pointing out that by connection we mean Bluetooth level sockets, not TCP/IP connections. When Phone A talks to Phone B over Thali a single Bluetooth socket is formed. We then put a multiplexer in front of that socket and we run all the TCP/IP connections through the multiplexer. So the pool doesn't really care how many TCP/IP connections there are, just how many Bluetooth sockets there are.

The pool has total control over how many outgoing Bluetooth sockets will be created since the pool owns scheduling all outgoing requests.

The challenge is incoming connections. Right now the pool has no direct control over those, but it does have indirect control. The pool can hook into the TCPServersManager and get notified anytime a new incoming connection is established. If the connection isn't something the pool manager wants then it can order the mux killed which will kill the listening port which will automatically cause the Bluetooth incoming socket to also be killed. This isn't a terribly elegant solution. Ideally we would add a native interface that specifies how many Bluetooth Sockets total, in either direction, should exist. Let's say that number is 2. Let's further say that at the moment there are 2 incoming connections and the pool wants to now create an outgoing connection. The pool can choose which of the 2 incoming connections to kill and then create the outgoing connection. There is, however, a race condition. Between the time the pool kills one of the incoming connections (by killing the listening port) another incoming connection might sneak in before the pool has time to establish its outgoing connection. So in theory if we wanted to prevent that situation we would have to create an API that separately specifies how many incoming and outgoing connections there are supposed to be and then the pool would call that native API as it changes things around.

Another alternative is to handle this at the discovery layer. Potentially the device could let potential connectors know ahead of time if there is a slot for them. Although one can imagine the wonderful race conditions that will lead to. We would have to introduce some kind of time based token. Yuck.

For now it seems easier to start with the simplest approach, which is that pools control connections by hooking into the TCPServersManager to monitor incoming connections and uses the pool itself to manage outgoing connections. If we need more, we'll build more.
