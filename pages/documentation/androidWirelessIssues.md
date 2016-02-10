---
title: Android Wireless Issues
layout: page
sidebar: right
permalink: "/androidWirelessIssues/"
categories:
    - documentation
header:
   image_fullwidth: "header.png"
---

Getting peer to peer working on Android has been and continues to be a heck of a challenge. This document outlines a number of major areas where we had issues/challenges with Android and explains our rationale for how we dealt with them.

# Discovery - Wi-Fi Direct vs BLE
One of the first problems we ran into is - how should two Android phones discover each other as a prelude to communicating bulk data? We are specifically talking about a scenaro in which there is no cell or local Wi-Fi. So the phones are on their own.

## Wi-Fi Direct
Originally we adopted Wi-Fi Direct's discovery infrastructure. Wi-Fi Direct discovery is a two step process. First there is peer discovery in which a phone advertises its MAC, a phone type ID and a phone identifier (that we can't set programmatically). This is then followed by Wi-Fi Direct Service Discovery where a phone can advertise what services it uses. We like Wi-Fi Direct because support for Wi-Fi Direct has been in Android for quite some time and so in theory using Wi-Fi Direct meant we could perform discovery using older handsets.

After literally months of testing Wi-Fi Direct's discovery infrastructure against numerous Android phones we came to two conclusions:

1. Wi-Fi Direct Peer discovery works pretty well
2. Wi-Fi Direct Service discovery doesn't work very well at all

We ran into endless problem with Wi-Fi Direct Service discovery including but not limited to:

* Phones from different manufacturers were highly likely to either never find each other over Wi-Fi Direct Service Discovery or if they did find each other it could take many minutes (in other words, way too long for our scenarios where people are either walking by each other with phones in their pockets or have the phones out and are expecting to transfer data as the other person waits)
* The Wi-Fi Direct Service Discovery stack had a habit of just stopping to work after some period of time. And once it stopped working the only way to get it working against was to reboot the phone.
* We encountered random disruptions in the working of the main Wi-Fi stack when we used Wi-Fi Direct Service discovery for any extended period of time. This caused havoc in our test infrastructure where we use Wi-Fi to coordinate tests.

So while Wi-Fi Direct Service discovery was perfect for our needs in theory, in practice it wasn't going to work.

Now we could have fallen back to Wi-Fi Direct Peer Discovery. But this wasn't a good solution either because we weren't sure if the instability we saw in the Wi-Fi stack also applies to Wi-Fi Direct Peer Discovery. Also Wi-Fi Direct Peer Discovery made ad-hoc connections impossible. For example, using peer discovery the only way to know if another phone is a Thali phone is to already know their Wi-Fi MAC address. In that case when the MAC address is discovered via Wi-Fi Direct Peer discovery we could match it against our list of known partners and start negotiating a connection (see the next section for the fun details on that one). But let's imagine there is Joe and Jane. they each have phones, they have done [identity exchange](http://www.goland.org/thaliidentityexchangeprotocol/) and they now know each other's MAC addresses. So in the future when either phone sees the other over Wi-Fi Direct peer discovery they could recognize each other. But now imagine that Joe gets a new phone and Jane comes to work with a laptop. Now what? They devices won't recognize each other even though the owners trust each other and presumably (though a feature we haven't implemented yet) have keys that have been signed by the other device's keys attesting to their identity. This isn't a complete show stopper and there are some (nasty) work arounds but given the issues we were already seeing with Wi-Fi Direct all up we decided that another route would perhaps be prudent.

## Bluetooth 
Bluetooth also has a discovery protocol but it works in a fundamentally different way than Wi-Fi Direct. In theory (but not practice it would seem) Wi-Fi Direct discovery could be left on all the time. We tested the battery drain and it wasn't too bad and it actually got better thanks to some improvements that Google made in power management in newer version of Android. But Bluetooth discovery (we will go into a lot more detail about this later) is designed to be run for a short period of time and to eat a lot of battery. Even worse to make a device discoverable over Bluetooth requires an OS provided system dialog. Every single time. This immediately ruled out scenarios where we wanted to do discovery and data exchange in the background.

Now Bluetooth actually supports a work around similar to Wi-Fi Direct peer discovery. If one somehow knows a peer's Bluetooth MAC then it's possible to directly poll for them. You basically can just send out "are you there?" requests (SDP lookups) using the MAC. But this turns out to be very slow. As former Thali team member Jukka Silvennoinen outlined in an in depth [article](http://www.drjukka.com/blog/wordpress/?p=121) we were only able to scan 5 addresses or so per second. And scanning with Bluetooth is a battery drain.

So Bluetooth was out.

## Bluetooth Low Energy (BLE)
So this left us with BLE. In theory (heard that one before?) BLE was perfect for us. It's designed to sip battery and to do discovery efficiently. But the problem is that to use BLE right we needed to be both a peripheral and a central simultaneously. A peripheral is what BLE calls devices that can be discovered over BLE and a central is what BLE calls devices that can discover peripherals. Before Bluetooth 4.1 most devices could be a central or a peripheral but not both at the same time. In fact, in Android land, before Bluetooth 4.1, Android phones that had BLE hardware could only be centrals, not peripherals. A discovery system in which devices can scan but can't actually be found (since you have to be a peripheral to do that) isn't terribly useful.

So this meant that moving to BLE also required us to only support phones that have Bluetooth 4.1 hardware. As of the time this article is written (early 2016) only new higher end phones support Bluetooth 4.1 hardware. But we decided to accept that limitation because our belief is that by the time Thali hits the mainstream most phones will have 4.1 hardware. If that isn't true then we will have to reconsider trying one of the other alternatives.

Note that BLE isn't all wine and roses. For example, we need to move our [discovery beacons](https://github.com/thaliproject/thali/blob/gh-pages/pages/documentation/PresenceProtocolForOpportunisticSynching.md) over BLE. This is because it's expensive both in terms of time and battery to set up Bluetooth connections. So we only want to create one when we have to (e.g. when we need to move around relatively large pieces of data). So we move discovery beacons over BLE to reduce the use of Bluetooth. However the total size of a typical discovery beacon is bigger than the default BLE data size and so we can't take advantage of what's called "hardware offloading". The idea is that if characteristics (values available via BLE) are small enough they can just be handed to the BLE hardware who can serve them up without having to wake one of the device's cores. But because we are using large data every single time we get a BLE connection we'll have to wake up a core to handle it. It's not clear in practice how big a deal this is as most Android phones have anywhere from 4-8 cores and are typically smart enough to only wake up one at a time as needed. But it's something to keep an eye on.

# Establishing high bandwidth connections - Wi-Fi Direct vs Wi-Fi vs Bluetooth
Using BLE for discovery is nice but BLE has extremely low bandwidth, typically 7 times less than Bluetooth which already has at least 10x less bandwidth than Wi-Fi. Since our scenarios call for moving potentially megabytes of data around this wasn't going to fly.

## Wi-Fi Direct
Again, in theory, Wi-Fi Direct was perfect for our needs. It could be run to the side of the existing Wi-Fi stack. It allowed for arbitrary communication and it's channel was 10x or more faster than alternatives like Bluetooth. But we never even got to the point of figuring out if using Wi-Fi Direct groups would cause us the same indigestion as Wi-Fi Direct discovery because joining a Wi-Fi Direct group required that the user hit OK on a system specified dialog. Immediately this killed off any hope of working in the background (is the user supposed to pull their phone out of their pocket and hit o.k. every time they have a connection??!?!) and it produced an awful experience in the foreground (why am I getting this dialog?!?!).

## Wi-Fi
We could play a nasty little trick. What we could do is have one phone create a Wi-Fi Direct group. This creates an access point that traditional Wi-Fi stacks could connect to, you just needed to know the SSID and the password. We could then have the other phone connect to the first phone's Wi-Fi Direct group not via the Wi-Fi Direct stack but using traditional Wi-Fi. We just send the SSID and the password over BLE. This would require no system UX at all.

There are two problems however with this approach. The biggest problem is that this approach makes us more than a little queasy from a security perspective. When the second phone connects to the first phone as a Wi-Fi Access Point all communication going over Wi-Fi on the second phone, including other applications, will now go to the first phone. In the case that there is an application running in the foreground on the Android phone which isn't Thali we couldn't use this feature at all since it would completely mess up the foreground app's Internet connection. Note to mention letting the other phone see everything the second phone is doing!

I still think we'll end up implementing this approach at some point. The bandwidth and range should be much better than Bluetooth. And the security implications are no worse than connecting to some random Wi-Fi endpoint. Even the issue of interfering with the app in the foreground isn't a killer. As we will discuss in gory detail later on Bluetooth screws with Wi-Fi bad enough that we probably can't synch on a phone while another app is in the foreground anyway (see [issue 41](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/41).

## Bluetooth
For now our default solution is Bluetooth. We'll go into Bluetooth basics in the next section but suffice it to say that Bluetooth has lots and lots and lots of issues. It's slow, it screws with the other radios and thanks to Android Marshmallow we now have to deal with "Bro Mode". If we can get Wi-Fi to work I think we'll switch to that.

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

## The antenna problem
Wi-Fi (usually), Bluetooth and BLE all run over the exact same frequency band, the 2.4 GHz Industrial, Scientific and Medical (ISM) band. However all three services typically have to share the same antenna. That is bad because only one of the services can actually use the antenna at a time. So if multiple services are running at the same time they have to switch to time division multiplexing. Which is a fancy way of saying that each service has to take turns using the antenna. But by definition this means that each service isn't listening all the time and so will miss things. Those misses cause errors and slow things down. So, for example, in theory a Bluetooth connection should be able to get up to 3 Mbps raw bandwidth. But using a Bluetooth connection while also using Wi-Fi will typically reduce that bandwidth to somewhere between 100 - 300 Kbps or a 10x - 30x bandwidth decrease.

This is why the effective bandwidth over Bluetooth sucks so badly. This is also why moving data over Bluetooth will also mess with Wi-Fi's bandwidth as well. This is why we work so hard to use Bluetooth as little as possible.

This also argues that we should eventually experiment with things like turning off Wi-Fi and BLE when we are doing a Bluetooth transfer. This could be reasonable in cases where either we are the app in the foreground or there are no apps in the foreground. Even then we need to see if we can figure out if anyone is using Wi-Fi before we do this. But if we do it could give us a significant speed enhancement. This idea is recorded in [Issue 40](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/40).

## Piconets
Unlike shared band protocols like Wi-Fi, Bluetooth uses something called frequency hopping spread spectrum. In frequency hopping spread spectrum rather than always sending signals over the same frequency a device will hop from frequency to frequency, in Bluetooth's case 1600 times a second. This works because the 2.4 Ghz band is a 'band', as in, a collection of frequencies. Specifically between 2400 MHz to 2500 MHz and broken by Bluetooth into 79 channels. So each transmission hops from channel to channel within the band. 

To make this work there is a device called a master who forms a piconet. The master of a piconet decides what pattern to use in hopping frequencies. The idea being that if two or more piconets are run by two or more masters in the same physical location they will pick different frequency hop patterns and thus reduce how often they interfere with each other. Frequency hopping also means that Bluetooth is less likely to interfere with or be interfered by other users of the 2.4 Ghz ISM band.
 
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
