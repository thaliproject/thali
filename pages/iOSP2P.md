---
title: Exploring local P2P on iOS
layout: page-fullwidth
permalink: "/iOSP2P/"
---
# Code
[https://github.com/thaliproject/ThaliBubbles](https://github.com/thaliproject/ThaliBubbles)

# Scenario - Group Chat
Users A, B and C all want to communicate to each other for a group chat using their iOS devices. There is an assumption that the users have authenticated to each other and that their communications are secure. There is also an assumption that communication will work even when there is no Internet connectivity.

I am going to assume that there is an identity exchange. It could be done locally using Bluetooth or remotely over email. But we assume there is a point of exchange where configuration data can be shared. The real focus of this investigation is how can we enable handsets to communicate when they have something to say?

# iOS's P2P Communication technologies
In theory iOS devices typically support at least three local radios, BLE, Bluetooth and Wi-Fi. In practice however an application running on iOS trying to talk to another application running iOS really can only directly use BLE or a framework called "Multi-Peer Connectivity Framework". iOS also supports normal Wi-Fi aka Wi-Fi infrastructure mode.

## Wi-Fi Infrastructure mode

When people talk about 'Wi-Fi' this is usually what they mean. In this mode the iOS device connects to a local Wi-Fi access point.

In this scenario users A, B and C all have their devices registered to either the same access point (AP) or to APs that are part of the same local network.

In theory we could then allow the devices to find each other by using either broadcast or multi-cast via a protocol like mDNS or SSDP and then establish point to point connections to allow communication.

In practice many networks block broadcast and multi-cast as well as disallow devices to directly communicate to each other using local addresses over the local network. This blocking is done ostensibly for security reasons so that members of the same network can't just scan and attack each other.

This means we should experiment with mDNS and SSDP support for discovery. But we have to accept that it likely won't work and we need to have a fallback. If the local network is connected to the Internet then we can potentially use an external infrastructure to enable discovery and connectivity. We typically use TOR for that.

When dealing with a locked down Wi-Fi infrastructure we can also ignore the Wi-Fi infrastructure and treat this like the ad-hoc asynchronous mesh scenario below.

## Bluetooth Low Energy (BLE)
BLE has two main modes, central and peripheral.

A central is a BLE device that is looking for other BLE devices. There are good reasons this is not called a 'client' but I'm not going to go into those here.

A peripheral is a BLE device that is advertising itself and can be discovered by a central.

### Central
An iOS application can register a particular service ID (think of it as a type) that it is looking for. If the iOS BLE hardware detects a peripheral advertising the desired service ID then it will notify the application. If the application is in the foreground when this happens then it can respond to the discovery however it likes.

The good news is that an application can register to be notified when a service ID is discovered even when the application isn't running at all. If iOS detects the requested service ID then it will trigger the app and let it handle the discovery. But at this point if the app isn't in the foreground then its options are very limited. It gets around 10 seconds to do whatever its going to do and its only networking options are either to trigger a HTTP download (which won't work here since the two devices don't share an IP network at this point) or to talk over BLE. But it cannot, for example, start a Multi-Peer Connectivity Framework session. So basically it can't move any significant amount of data.

### Peripheral
An iOS application can also register itself as a peripheral. To do this the application specifies what service ID it wants to advertise. iOS will then advertise that service ID for as long as the application is in the foreground. Once the application goes to the background then by default iOS will stop advertising the service ID. At that point nobody looking for that service ID can find the iOS device.

In other words, if two users are both running a Thali application on iOS. Both are within BLE range of each other. Both have their phones in their pockets so the Thali apps are not in the foreground. Then they are not going to find each other. In this case what is happening is that both phones are centrals, meaning they are looking for the desired service ID, but neither are peripherals (because the apps aren't in the foreground) and so neither is advertising the desired ID. This means the kind of background discovery and data exchange we take for granted in Android (see [here](/AndroidP2P)) won't work in iOS.

There is apparently a small work around to this problem but it's both limited and has security implication. The work around is that it is possible for an Application to get iOS to act as a peripheral even when the application is not in the foreground. But in that case iOS will not advertise a service ID. Instead it will only advertise its network address. So if two iOS devices have communicated before then in theory they could try to register to discover each other based on their BLE network addresses. But every time an iOS device is restarted it will pick a new BLE network address. So to make this work one has to program an application to remember what BLE network address it used in the past and force that to become the new address after each reboot. Of course running around with a permanent ID when one didn't have to isn't an awesome idea. And even if the two devices do discover each other just using the network address their communication options are still limited.

## Multi-Peer Connectivity Framework

iOS devices also support something called the Multi-Peer Connectivity Framework. This is actually a proprietary set of APIs provided by Apple that run on a combination of Bluetooth and Wi-Fi. Using these APIs one can perform both discovery and establish reasonably high bandwidth connections (say 1 Mb/s) directly between devices. In general the Multi-Peer Connectivity Framework will only run while the application is in the foreground. If the app has established a connection over the Multi-Peer Connectivity Framework while in the foreground and then is switched to the background then iOS will allow existing connections to remain running for around 3 minutes before killing them. The idea being that the application does get a little time to clean things up before it loses access to the Multi-Peer Connectivity Framework. But note that an application cannot start a new connection over the Multi-Peer Connectivity Framework while in the background.

# The local P2P experience over iOS

As a general rule local P2P is just not going to work on iOS unless at least one iOS device is in the foreground. Imagine users A, B and C are all within radio range of each other, all have a Thali app and all have it turned off. In that case nothing is going to happen.

Now imagine that user A pulls the phone out and activates the Thali app. In that case user A's phone can become a peripheral and advertise the Thali service ID. Both phones B and C will see this and will be able to establish a BLE connection to user A's phone even though they are both in the background (or even halted).

Let's say that users A and B have data for each other. In that case they can discover that over the BLE connection and user B's code can trigger a toast to notify user B that there is data immediately available. If User B takes the phone out and hits the toast then the Thali app will run and it can use the Multi-Peer Connectivity Framework to transfer data at reasonably high speeds.

But let's have the same scenario with a twist. Users A, B and C are in a room. Only User A has the Thali app running. User A's phone is then discovered by B and C. But in this scenario the people with data for each other are not A and B but rather B and C. We can actually use User A's phone as a hub. Both B and C can send their advertisements to A and A can repeat the advertisements. This would allow B and C to discover each other via A. Once B, for example, discovers C and knows it wants to talk to C. Then B will have to send a BLE message to A who will forward it to C. At that point both phone B and C will raise toasts telling their users to pull out their phones and bring the Thali app into the foreground. If both users do that then B and C can directly communicate. More importantly, B and C can directly communicate via the Multi-Peer Framework and hopefully chat at a speed less glacial than supported by BLE.

Note that while the BLE channel is limited it's not that small. So, for example, if user B pulls the phone out of the pocket to talk to C and C doesn't go into the foreground then B could detect this and send snarky text messages over BLE to C's phone asking C to please pull out the phone. Because, hey, snarky messages always work! In other words, BLE has enough bandwidth to be a reasonable SMS style chat application.

# Using Android to help iOS
We think that iOS and Android should be able to talk just fine over BLE. But since we haven't actually run a real experiment on this yet we can't be completely sure. But if BLE does work between the two then at least Android devices can help out their iOS brethren by acting as BLE hubs as described above.

# Communicating between Android and iOS
Unfortunately Apple has its own proprietary version of Bluetooth that won't work with Bluetooth SIG compliant implementations like Android. Apple also doesn't support Wi-Fi Direct.

So if an Android and iOS device are going to communicate then the only way it is going to happen is either over BLE (slow) or myfi.

Myfi is just another name for an Android phone turning itself into a Wi-Fi Infrastructure Mode Access Point. In fact Android even provides the right APIs to automatically make the phone turn on Myfi.

The problem is, iOS does not have any Wi-Fi Apis. There is no way to switch the Wi-Fi access point iOS is using programmatically. So the only way that the iOS user can connect to the Android phone's Myfi AP is via user intervention.

In other words, the Android phone has to turn on Myfi and then send its AP name over BLE to the iOS phone. The iOS phone then has to display a dialog to the user saying "Dear user, please hit the menu button, then go to settings, then go to general, then go to wi-fi and then choose the wi-fi access point named 'abcdefg'".

Yes, seriously.

That is literally the best we can do for Android/iOS communication.

The good news is that this should actually work. Once the iOS device connects to the Android phone then they can communicate directly and with good bandwidth.

In theory the iOS device might remember the myfi AP and in the future if the Android device re-activates the myfi AP then the iOS device could automatically connect. Maybe. It depends on exactly how iOS decides which AP to connect to. Does iOS remember both the MAC and SSID or just the SSID? I'd imagine the later or corporate networks with multiple access points using the same SSID wouldn't work. How does it choose which SSID to connect to and when, if ever, to switch? In other words, if someone is somewhere where there is no connectivity then presumably a recognized SSID will be connected to. But what if the iOS device already has a Wi-Fi connection? The situation gets to be even more fun if there are multiple Android Thali apps running all trying to be myfi endpoints.

And lets say that we figure out all the kinks and somehow the SSID automatic reconnect trick works really well. If everything is based on SSID then how do we stop bad guys from just using the same SSID and getting to man in the middle all of the iOS users traffic? Sure, we could throw a password in the mix but are we now going to really ask the user to leave the Thali app, go to Settings->General->Wi-Fi, pick the right endpoint name and type in a password?

There is a lot of work to do here.
