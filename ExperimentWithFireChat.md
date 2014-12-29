---
title: Experimenting with FireChat
layout: default
---

On 12/29/2014 I had a couple of Android devices hanging around so I decided to try out FireChat and see what I could see. 

### Setup
I have a Samsung Galaxy S4 (Samsung) and a Nexus 7 (Nexus).
Note that the Samsung does not have a SIM.
Both devices are running their stock OS's.
Each device has its own unique FireChat account.
I kept the devices hooked to the Internet via WiFi.
I made each device a follower of the other device's account.

### Experience
As soon as I installed FireChat each device displayed a notification that there was another device around.

I went to the nearby chat room on the Samsung and sent a message that was picked up by the Nexus. But no subsequent messages in the nearby chat room ever made it. Either from the Samsung to the Nexus or vice versa.

I then went to the 'following' area and posted messages on both the Samsung and Nexus that did successfully make it across.

But all further attempts to use the nearby chatroom failed. Only that one message, from the Samsung to the Nexus, ever made it across. Otherwise I could only communicate using the 'following' area not the Nearby chatroom.

I still get notifications that FireChat users are nearby but communication does not actually work.

### Setup 2
To make things more interesting I threw in my personal Android device, a HTC One running CyanogenMod.
I ran FireChat in privacy guard mode but I did give it access to my location.

### Experience 2
Even before signing in (as with the previous setup) it detected that other FireChat users are nearby.

Before following anyone I went to the Nearby chat room where it showed 3 people chatting.

I sent a message out from the HTC and both the Samsung and Nexus instantly got it!

I then sent a message from the Samsung in the Nearby room. The Nexus did not get it but the HTC One did.

I then had the Nexus send out a message. The HTC One got it but the Samsung did not.

### Tests
#### Sending device right next to receiving devices
In this test the device listed on the left is trying to send a message to the devices on the right.

|         | Nexus | Samsung | HTC One |
|---------|-------|---------|---------|
| Nexus   |       | Occasionally      | Always     |
| Samsung | Occasionally    |         | Always     |
| HTC One | Always   | Always     |         |

#### Sending device is in the next room from the receiving devices

|         | Nexus | Samsung | HTC One |
|---------|-------|---------|---------|
| Nexus   |       | Occasionally    | Always     |
| Samsung | Occasionally      |         | Always     |
| HTC One | Always      | Always     |         |



Devices a full floor away from each other





### Setup 2
To just make sure I push things a bit I intentionally 'forgot' the local WiFi access point on both devices. If they are going to communicate it is going to have to be directly.

### Experience
