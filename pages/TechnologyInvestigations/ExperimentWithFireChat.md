---
title: Experimenting with FireChat on Android
layout: page-fullwidth
permalink: "/ExperimentWithFireChat/"
categories:
    - technologyinvestigations
---
# Executive Summary
FireChat running offline on three Android devices was able to successfully communicate without any Internet connectivity using what appears to be a combination of Bluetooth and Wifi. No user interaction was needed to set up anything (e.g. no Bluetooth of WiFi approval dialogs from the OS) and notifications worked just fine when the apps were in the background. This is a pretty exciting proof that one can do a reasonable mesh application on Android. Oddly, FireChat worked less well when connected to the Internet.

Unfortunately in a follow up study using the same devices plus an iPhone with updated Firechat on all devices the results were a complete mess. Even though the phones were all next to each other and sometimes I would even get notifications that there were other firechat devices around I wasn't able to exchange any messages. I switched all the devices onto local wifi and even then the iPhone could sometimes see the Android devices but the Android devices could not see the iPhone or each other.

# Devices
* Samsung Galaxy S4 running stock OS, no SIM.
* Nexus 7 running stock OS.
* HTC One running Cyanogenmod

# Configuration
Each device has a different FireChat account.
Two of the devices (Samsung and Nexus) are followers of each other, the HTC doesn't follow anyone.

# Internet Connected Results
I ran the first set of tests while the devices were all connected to the local Internet connected WiFi router. The HTC had a nearly 99% success record in communicating with the Samsung and Nexus and vice versa from the Samsung and Nexus to the HTC. But for some odd reason the Samsung and Nexus had a lot of trouble talking to each other. 50% plus failure rates weren't uncommon.

# Turning off the Internet
I made all the devices forget the local WiFi router, turned on Airplane mode and then manually turned on WiFi and Bluetooth (no pairing of any kind though).

The first tests all failed 100%. It was only when I forceably stopped the FireChat app and restarted it that I then had 100% success across the board. I went to parts of my house and outside that the old wifi router I recently replaced couldn't get a signal to and I still was able to succesfully communicate.

I tried testing with bluetooth on and wifi off and with wifi off and bluetooth on and those tests all failed.

# So how does FireChat work?
I found very little online about how FireChat works on Android. I tried to turn on/off wifi and bluetooth but the app seems to refuse to work unless both are activated. I tried to separate the devices by a sufficient distance that bluetooth couldn't reach but wifi could but couldn't find a distance far enough that didn't cause issues with experimenting. For example, no where in my house was far enough! I'll grab a friend at work and try some experiments there.

But honestly it just plain doesn't matter how FireChat actually works. That's just an implementation detail. What matters is that one can create a good mesh experience on Android. At no point did we need to have any OS level dialogs bothering the user. It 'just worked' (except for the multiple times when it didn't but you get the idea).

So at this point I'm more interested in playing around with the Bluetooth and wifi APIs in Android than I am in investigating FireChat directly.
