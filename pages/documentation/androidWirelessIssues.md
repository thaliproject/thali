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

Getting peer to peer working on Android has been and continues to be a heck of a challenge. This document outlines a number of major areas where we had issues/challenges with Android and explains our rationale for how we dealt with them

# The goal
There are two (or more) people with Thali Android devices within radio range but there is no Wi-Fi or Cell. We want to enable those people to communicate even without a formal Internet infrastructure. We have actually built this in Thali. Today we use BLE for discovery and Bluetooth for moving larger amounts of data. But the results are pretty bad due to endless issues with the radios on Android devices. Below I walk through the steps we have taken to try to enable P2P on Android without Internet, explore our failures and issues and look at how we would like things to work.

# How we handle local discovery and moving data
For an Internet free P2P scenario the first step is discovery. Who is around us? Do they have something interesting to say to us? Thali has spent a lot of time and energy trying to create a discovery mechanism that doesn't violate user's privacy. But this has interesting consequences. For one thing a discovery announcement can grow to up to 4K. Most discovery mechanisms aren't designed to handle that amount of data. So in many cases we had to send a small discovery announcement which just shared “We are a Thali device” and then we had to establish an expensive and slow to set up high bandwidth connection in order to move the rest of the 4K of discovery data. Only at that point would we know enough to know if there was a reason for us to keep talking. This is far from ideal because it's slow and uses battery.

# Discovery - Wi-Fi Direct vs BLE
One of the first problems we ran into is - how should two Android phones discover each other as a prelude to communicating bulk data? We are specifically talking about a scenario in which there is no cell or local Wi-Fi. So the phones are on their own.

## Wi-Fi Direct
Originally we adopted Wi-Fi Direct's discovery infrastructure. Wi-Fi Direct discovery is a two step process. First there is peer discovery in which a phone advertises its MAC, a phone type ID and a phone identifier (that we can't set programmatically). This is then followed by Wi-Fi Direct Service Discovery where a phone can advertise what services it uses. We like Wi-Fi Direct because support for Wi-Fi Direct has been in Android for quite some time (since Jelly Bean for the APIs we need) and so in theory using Wi-Fi Direct meant we could perform discovery using older handsets.

After literally months of testing Wi-Fi Direct's discovery infrastructure against numerous Android phones we came to two conclusions:

1. Wi-Fi Direct Peer discovery works pretty well
2. Wi-Fi Direct Service discovery doesn't work very well at all

We ran into endless problem with Wi-Fi Direct Service discovery including but not limited to:

* Phones from different manufacturers were highly likely to either never find each other over Wi-Fi Direct Service Discovery or if they did find each other it could take many minutes (in other words, way too long for our scenarios where people are either walking by each other with phones in their pockets or have the phones out and are expecting to transfer data as the other person waits)
* The Wi-Fi Direct Service Discovery stack had a habit of just stopping to work after some period of time. And once it stopped working the only way to get it working against was to reboot the phone.
* We encountered random disruptions in the working of the main Wi-Fi stack when we used Wi-Fi Direct Service discovery for any extended period of time. This caused havoc in our test infrastructure where we use Wi-Fi to coordinate tests.

In addition we ran into limitations in terms of how much data we could transfer in a Wi-Fi Direct Service Discovery. The practical limit (depending on what version of Android one was using) was somewhere around 750 bytes but we really needed 4k.

So Wi-Fi Direct Service discovery was unworkable in practice and less than ideal due to data size limitations in theory.

Now we could have fallen back to Wi-Fi Direct Peer Discovery. But this wasn't a good solution either because we weren't sure if the instability we saw in the Wi-Fi stack also applies to Wi-Fi Direct Peer Discovery. Also Wi-Fi Direct Peer Discovery made ad-hoc connections impossible. For example, using peer discovery the only way to know if another phone is a Thali phone is to already know their Wi-Fi MAC address. In that case when the MAC address is discovered via Wi-Fi Direct Peer discovery we could match it against our list of known partners and start negotiating a connection (see the next section for the fun details on that one). But let's imagine there is Joe and Jane. they each have phones, they have done [identity exchange](http://www.goland.org/thaliidentityexchangeprotocol/) and they now know each other's MAC addresses. So in the future when either phone sees the other over Wi-Fi Direct peer discovery they could recognize each other. But now imagine that Joe gets a new phone and Jane comes to work with a laptop. Now what? They devices won't recognize each other even though the owners trust each other and presumably (though a feature we haven't implemented yet) have keys that have been signed by the other device's keys attesting to their identity. This isn't a complete show stopper and there are some (nasty) work arounds but given the issues we were already seeing with Wi-Fi Direct all up we decided that another route would perhaps be prudent.

## Bluetooth 
Bluetooth also has a discovery protocol but it works in a fundamentally different way than Wi-Fi Direct. In theory (but not practice it would seem) Wi-Fi Direct discovery could be left on all the time. We tested the battery drain and it wasn't too bad and it actually got better thanks to some improvements that Google made in power management in newer version of Android. But Bluetooth discovery (we will go into a lot more detail about this later) is designed to be run for a short period of time and to eat a lot of battery. Even worse, to make a device discoverable over Bluetooth requires an OS provided system dialog. Every single time. This immediately ruled out scenarios where we wanted to do discovery and data exchange in the background.

Now Bluetooth actually supports a work around similar to Wi-Fi Direct peer discovery. If one somehow knows a peer's Bluetooth MAC then it's possible to directly poll for them. You basically can just send out "are you there?" requests (SDP lookups) using the MAC. But this turns out to be very slow. As former Thali team member Jukka Silvennoinen outlined in an in depth [article](http://www.drjukka.com/blog/wordpress/?p=121) we were only able to scan 5 addresses or so per second. And scanning with Bluetooth is a battery drain.

So Bluetooth for discovery was out.

## Bluetooth Low Energy (BLE)
So this left us with BLE. In theory (heard that one before?) BLE was perfect for us. It's designed to sip battery and to do discovery efficiently. But the problem is that to use BLE right we needed to be both a peripheral and a central simultaneously. A peripheral is what BLE calls devices that can be discovered over BLE and a central is what BLE calls devices that can discover peripherals. Before Bluetooth 4.1 most devices could be a central or a peripheral but not both at the same time. In fact, in Android land, before Bluetooth 4.1, Android phones that had BLE hardware could only be centrals, not peripherals. A discovery system in which devices can scan but can't actually be found (since you have to be a peripheral to do that) isn't terribly useful.

So this meant that moving to BLE also required us to only support phones that have Bluetooth 4.1 hardware. As of the time this article is written (early 2017) only high end and fancier mid end phones support Bluetooth 4.1 hardware. But we decided to accept that limitation because our belief is that by the time Thali hits the mainstream most phones will have 4.1 hardware. If that isn't true then we will have to reconsider trying one of the other alternatives.

BLE's discovery is similar to Wi-Fi Direct discovery. There is an advertising packet, which can contain 31 octets of data we can set. This is then optionally followed by connecting to a GATT server. GATT servers allow us to move characteristics which are essentially name/value pairs. The values are fairly short but there are tricks available to move larger amounts of data. The main limitation on GATT servers is that BLE is a very slow protocol. In our [tests](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/82) if we turned power all the way up it took around 500 ms to connect to a GATT server and then around 530 ms to move 4K of data. Not ideal but not horrific.

Right now however we don't use the GATT server. This is because we had run into some reliability issues with the GATT server in our early testing. But our later work (as outlined in the test link above) showed that at least on the devices we were testing the GATT server works reasonably well.

# Establishing high bandwidth connections - Wi-Fi Direct vs Wi-Fi vs Bluetooth
Using BLE for discovery is nice but BLE has extremely low bandwidth, typically 7 times less than Bluetooth which already has at least 10x less bandwidth than Wi-Fi. Since our scenarios call for moving potentially megabytes of data around this wasn't going to fly.

## Wi-Fi Direct
Again, in theory, Wi-Fi Direct was perfect for our needs. It could be run to the side of the existing Wi-Fi stack. It allowed for arbitrary communication and it's channel was 10x or more faster than alternatives like Bluetooth. But we never even got to the point of figuring out if using Wi-Fi Direct groups would cause us the same indigestion as Wi-Fi Direct discovery because joining a Wi-Fi Direct group required that the user hit OK on a system specified dialog. Immediately this killed off any hope of working in the background (is the user supposed to pull their phone out of their pocket and hit o.k. every time they have a connection??!?!) and it produced an awful experience in the foreground (why am I getting this dialog?!?! What do I do if I said no but I meant yes?).

## Wi-Fi
We could play a nasty little trick. What we could do is have one phone create a Wi-Fi Direct group. This creates an access point that traditional Wi-Fi stacks could connect to, you just needed to know the SSID and the password. We could then have the other phone connect to the first phone's Wi-Fi Direct group not via the Wi-Fi Direct stack but using traditional Wi-Fi. We just send the SSID and the password over BLE. This would require no system UX at all.

There are two problems however with this approach. The biggest problem is that this approach makes us more than a little queasy from a security perspective. When the second phone connects to the first phone as a Wi-Fi Access Point all communication going over Wi-Fi on the second phone, including other applications, will now go to the first phone. In the case that there is an application running in the foreground on the Android phone which isn't Thali we couldn't use this feature at all since it would completely mess up the foreground app's Internet connection. Note to mention letting the other phone see everything the second phone is doing!

I still think we'll end up implementing this approach at some point. The bandwidth and range should be much better than Bluetooth. And the security implications are no worse than connecting to some random Wi-Fi endpoint. Even the issue of interfering with the app in the foreground isn't a killer. As we will discuss in gory detail later on Bluetooth screws with Wi-Fi bad enough that we probably can't synch on a phone while another app is in the foreground anyway (see [issue 41](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/41).

## Bluetooth
For now our default solution is Bluetooth. Bluetooth unfortunately is slow and unreliable. In our perf tests we were lucky to see 0.179 MB/s (aka 1.432 Mb/s). But with some devices we were really lucky if we saw 0.006 MB/s (aka 0.048 Mb/s). Thanks to a mountain of tricks and bug fixes Bluetooth now seems to mostly sorta kinda work on specific devices but even then it sometimes just fails for no apparent reason.  And that is without addressing [Bro Mode](http://www.goland.org/thalilocalp2psurvivingmarshmallow/). 

# The antenna problem
Wi-Fi (usually), Bluetooth and BLE all run over the exact same frequency band, the 2.4 GHz Industrial, Scientific and Medical (ISM) band. However all three services typically have to share the same antenna. That is bad because only one of the services can actually use the antenna at a time. So if multiple services are running at the same time they have to switch to time division multiplexing. Which is a fancy way of saying that each service has to take turns using the antenna. But by definition this means that each service isn't listening all the time and so will miss things. Those misses cause errors and slow things down. So, for example, in theory a Bluetooth connection should be able to get up to 3 Mbps raw bandwidth. But using a Bluetooth connection while also using Wi-Fi will typically reduce that bandwidth to somewhere between 100 - 300 Kbps or a 10x - 30x bandwidth decrease.

This is why the effective bandwidth over Bluetooth sucks so badly. This is also why moving data over Bluetooth will also mess with Wi-Fi's bandwidth as well. This is why we work so hard to use Bluetooth as little as possible.

This also argues that we should eventually experiment with things like turning off Wi-Fi and BLE when we are doing a Bluetooth transfer. This could be reasonable in cases where either we are the app in the foreground or there are no apps in the foreground. Even then we need to see if we can figure out if anyone is using Wi-Fi before we do this. But if we do it could give us a significant speed enhancement. This idea is recorded in [Issue 40](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/40).

# What we really want
In a perfect world we would probably use BLE's GATT server for discovery and Wi-Fi Direct for high bandwidth connectivity. In this perfect world the permission dialog for Wi-Fi Direct would be removed so we can form groups at will without user interaction. This perfect world would also see a lot of interop testing to really show that the Wi-Fi Direct group mechanisms worked properly and reliably even when there are a large number of phones in the same area with a constantly changing membership as phones enter and exit. To put a cherry on top of this perfect world the device would have at least two, maybe even three, 2.4 GHz antennas. One antenna would be just for BLE. The second would be just for Wi-Fi Direct connections. The third antenna would be for Wi-Fi infrastructure mode. That way each connection would have maximum bandwidth.

If we can't remove the permission dialog from Wi-Fi Direct then the second best thing would be three antennas for sure. One for BLE, one for Bluetooth and one for Wi-Fi. We would then have actual, meaningful, interop testing of Bluetooth to make sure it can reach its maximum theoretical bandwidth (e.g. close to 3 Mb/s) and actually works robustly without constantly crashing (as we currently experience Bluetooth). Similar testing for BLE would also be nice.

# Appendix

## Wi-Fi Direct

[Dr. Jukka Silvennoinen](http://www.drjukka.com/) wrote a lot of articles explaining the challenges he ran into with Wi-Fi Direct. I have collected the ones I know about below. They outline in gory detail a lot of the problems we have faced.

* [Initial Investigation with Wi-Fi Direct, problems and work arounds](http://www.drjukka.com/blog/wordpress/?p=24)
* [Some perf data for Wi-Fi Direct](http://www.drjukka.com/blog/wordpress/?p=29)
* [Details on how the UX required by Wi-Fi Direct killed Wi-Fi Direct for Thali](http://www.drjukka.com/blog/wordpress/?p=35)
* [Some work arounds that don't fully work for the UX issues with Wi-Fi Direct](http://www.drjukka.com/blog/wordpress/?p=39)
* [Some more improvements for Wi-Fi Direct](http://www.drjukka.com/blog/wordpress/?p=41)
* [Details on pairing issues with Wi-Fi Direct, more UX problems really](http://www.drjukka.com/blog/wordpress/?p=46)
* [How service discovery in Wi-Fi Direct just stops discovering](http://www.drjukka.com/blog/wordpress/?p=52)
* [Wi-Fi Direct Power Consumption](http://www.drjukka.com/blog/wordpress/?p=95)
* [Tricks for keeping Wi-Fi Direct Discovery continuously working](http://www.drjukka.com/blog/wordpress/?p=109)
* [Details on battery consumption for Wi-Fi Direct discovery based on availability or lack there of, of peers](http://www.drjukka.com/blog/wordpress/?p=124)
* [How much data we can send during Wi-Fi Direct discovery](http://www.drjukka.com/blog/wordpress/?p=127)
* [Details on handling Wi-Fi Direct discovery using UPnP and how much data we can send](http://www.drjukka.com/blog/wordpress/?p=129)
* [Wi-Fi Direct discovery using DNS-SD TXT records and how much data we can send](http://www.drjukka.com/blog/wordpress/?p=131)

## Bluetooth
* [Measuring Bluetooth Perf](http://www.drjukka.com/blog/wordpress/?p=100)
* [Why we can't use scanning for known MACs over Bluetooth](http://www.drjukka.com/blog/wordpress/?p=121)
* [Google's inadvertant attempt to kill Bluetooth for P2P on Android](http://www.goland.org/thalilocalp2psurvivingmarshmallow/)
* [How Google can achieve its Bluetooth security goals without killing Bluetooth for P2P on Android](http://www.goland.org/thalilocalp2psurvivingmarshmallow/)
* [Lots of perf results for Bluetooth, especially in combination with other 2.4 GHz radios like Wi-Fi and BLE](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1000)
 *  [here is a quick summary of what the results mean](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1077)
* [Battery consumption tests on Android](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1294)
* [Bluetooth battery tests](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1418)
* [A re-run with some new scenarios of the previous perf results](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1535)

# BLE
* [First look at BLE on Android](http://www.drjukka.com/blog/wordpress/?p=134)
* [BLE power consumption](https://github.com/thaliproject/Thali_CordovaPlugin/issues/1362)
* [Testing how well the GATT server works in practice](https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary/issues/82)

# Learning more about how we do discovery

Our discovery mechanism is probably one of the only moderately interesting technical aspects of Thali. For those curious to learn more you can read:

* [A user's bill of rights for Thali local discovery](http://www.goland.org/localdiscoverybillofrights/)
* [A one pager explaining how Thali discovery works](http://thaliproject.org/BriefDescriptionOfDiscoveryProtocol/)
* [A formal definition of our discovery protocol](http://thaliproject.org/PresenceProtocolForOpportunisticSynching/)
* [How we bind the discovery protocol to different transports like Wi-Fi, BLE/Bluetooth and iOS's multi-peer connectivity framework](http://thaliproject.org/PresenceProtocolBindings/)
