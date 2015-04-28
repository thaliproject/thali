---
title: Thali local P2P presence protocol for opportunistic synching
layout: default
---
# Informally defining the problem
Bob creates a post on his phone. The only way for Bob to communicate this post is via local radios (BLE, Bluetooth and Wi-Fi) because there is no Internet infrastructure in the neighborhood. In other words, all we have are mobile ad-hoc networks. Bob addressed the post to Alice, among others. So Bob wants to determine if Alice and the other people on the post are around and if so send them the post. The only way for Bob to figure this out is using his local radios.

The challenge is that Bob is being harassed by someone in his business group and he doesn't want that person able to determine his location. In other words, he doesn't ever want any of his local radios to give away his location to the person.

So how can Bob figure out if Alice and the others are in the area without leaking his identity?

This is just a specific example from a [Bill of Rights I wrote up](http://www.goland.org/localdiscoverybillofrights/) for people using P2P presence. P2P presence is an awesome feature but it's scope for abuse is extensive. Only by designing our technologies to respect user rights can we hope to leverage P2P presence in a way that brings more benefit than harm.

# Formally defining the problem

The previous description is nice but it leaves out some critical details of the problem. So I'm going to try to define the problem a bit more formally here.

A given device x has the following characteristics:

__Kx:__ This is the public key that device x uses to identify its user

__Lx:__ A set of devices, identified by their public key, that the user of device X is willing to expose their presence to. The value of Lx for any x is a secret known only to the device. Under no circumstances is Lx ever (EVER) shared between devices.

__Mx:__ A set of devices, identified by their public keys, that Device X has been asked to synch data to.

__Target Set:__ The target set is defined as M<sub>x</sub> &cap; L<sub>x</sub>. This is the set of devices that Device x has been asked to deliver to that it is willing to deliver to. Any device in Mx that is not in the target set will not be contacted by Device x.

There is a fixed period of time starting at T1 and ending at T2 during which a device will try to deliver any messages it has to any devices in the target set.

The challenge is that messages can only be delivered point to point. That is, if Device x has a message for Device y then Devices x and y must form a direct wireless connection to transfer the information.

Devices find each other using local discovery. Local discovery only works over a finite distance and has a very low bandwidth (in the case of BLE the maximum data rate is 35 KB/s and often much less). For the sake of this problem we will assume that it is impossible for two devices to identify each other just by their BLE signal. That is, we are constantly randomizing the BLE device address and we have sprinkled magic pixie dust to prevent radio fingerprinting. So the only way two devices can discover each other's identity over local discovery is if they explicitly communicate it.

During the time window T1 - T2 a random set of devices will come within range of each other. There is no way to predict ahead of time who those devices will be and if any of them happen to be in a given device's target set. In addition there is no reliable way before time T1 for all devices, or even the devices in a particular device's target set to coordinate together. It's possible, for example, for device x to have an entry in Mx for a device it's literally never talked to before. This would happen, for example, if at some point before T1 Device x sent a message to Device y addressed to y and z. Device y has never seen Device z but now it has a message it needs to forward to z if it runs into that device.

The goal then is to figure out how to enable devices to discover if someone who is in their target set is within range without disclosing their identity to anyone who is not in the target set and without disclosing the contents of Lx beyond the obvious fact that if Device x and y talk then x must have been in Device y's target set and y must have been in Device x's target set.

# Beacons
See the perf work I'm doing below. Still not done.

# Hot List
Exy = Timestamp + (HMAC-MD5(Sxy, Timestamp))?

The obvious problem being that you have to generate a HMAC for every entry in the address book and see if any match. So with a large address book that is just not going to work. But in most cases Dunbar's number saves us. Most people don't regularly work with more than a tiny number of people and pretty much can't work with more than 150 or so people meaningfully. So this means that most folks can have a "hot list" of frequently used addresses and if someone knows they are on someone else's hot list then they can just use that list. This is an optimization but it's a powerful one.

# Q&A
# Why can't users just announce their keys publicly? Maybe encrypt them with a group key?

Looking at our informal definition we can imagine that one way for Bob to find Alice is for Alice to just announce her public key over BLE. In fact, if everyone would just announce their public keys over BLE everything would be so much simpler! Everyone can see exactly who is around, figure out if they have something for them and then transmit it. The problem is that now everyone is announcing their identity to anyone who will listen.

The usual work around at this point is some kind of group key. For example, imagine that what we announce is someone's personal key but first we encrypt it with some group key that everyone at work has a copy of. This way locations will only be understandable to employees, nobody else will know who is announcing what.

The problem of course is that both Bob and his harasser are employees of the company and so the harasser will have the group key and be able to discover Bob.

There are variants on the group key approach but they tend to fail for reasons having to do both with the ad-hoc nature of communication in our scenario as well as for space reasons.

For example, imagine that Alice creates a new discussion group with Bob and they agree on a group key just for that discussion. Later on Alice decides to add Bob's harasser to the discussion group and shares the group key with the harasser. Since this is an ad-hoc networking scenario there is absolutely no guarantee that Alice ever had the opportunity to tell Bob about this. And presumably Alice has no idea that Bob has put a ban on his harasser. So if Bob just looks for the group key, thinking it's Alice, he could be inadvertently advertise his location to his harasser.

And, of course, our discovery channel is quite limited. So if there are lots and lots of groups there won't be enough bandwidth to announce them all.

# Comparing Addressbook Size Invariant Options - HMAC vs AES-GCM vs ECIES
E(HMAC)xy = Ke + Timestamp + (HMAC-MD5(Sey, Timestamp) + HMAC-MD5(Sxy, Timestamp))?

E(HMAC-AES-CBC)xy = Ke + Timestamp + IV + (HMAC-MD5(Sey, Timestamp) + AES-CBC(Sey, MD5(Kx) + HMAC-MD5(Sxy, Timestamp)))?

E(AES-GCM)xy = Ke + Timestamp + IV + (AES-GCM(Sey, MD5(Kx) + HMAC-MD5(Sxy, Timestamp))?

E(ECIES)xh = Ke + Timestamp + IV + (ECIES(Sey, MD5(Kx) + HMAC-MD5(Sxy, Timestamp))?

In each of the three options the first thing we have to do is use the ephemeral key with the receiver's key to generate an Ellptic Curve Diffie Hellman key. For the performance test we used secp256k1 as our curve. We picked this solely because it was a 256 bit key thus providing roughly AES 128 equivalent strength and it was supported by bouncy castle on Android which was our test environment. The test device is a Nexus 7 phone running Android 4.4.4.

In each case the ECDH key is generated once regardless of how many identities are advertised in the presence announcement. That same key is then used to try to check each value. To test how expensive generating this ECDH key is I generated one key per test run and ran the test 1000 times. The cost to generate a single ECDH key was:

| Min | Median | Max |
|-----|--------|-----|
| 18 ms | 25 ms | 64 ms |

So we know that right away the overhead for each of these approaches in time is around lets say 30 ms per announcement. Just keep in mind that it doesn't matter how big the announcement is because this cost is fixed regardless of how many entries are in the announcement.

In the size table below we will ignore both Ke and Timestamp since they are the same for all options.

| Approach | Fixed Sized Element | Size for each announced identity |
|----------|---------------------|----------------------------------|
| HMAC     | 0                   | 16 + 16 = 32 bytes               |
| HMAC-AES-CBC | 16              | 16 + (32 bytes unencrypted -> 48 bytes encrypted) = 64 bytes |
| AES-GCM  | 16                  | 32 bytes unencrypted -> 48 bytes encrypted |
| ECIES    |                     |                                  |

To compare each approach I generated 20 entries in a single presence announcement. The last entry will match against the receiver. The receiver has an address book with 10,000 entries in it. The only reason for even specifying the number of entries is to give us a meaningful comparison between the cost of the HMAC approach versus everything else. We will intentionally match on the last entry in the address book to provide the worse case scenario. Each test was run 100 times.

| Approach | Min | Median | Max |
|----------|-----|--------|-----|
| HMAC/HMAC| 1264 ms | 1299 ms | 1854 ms |
| HMAC/AES-CBC| 25 ms |  35.5 ms |  75 ms |
| AEC-GCM | 27 ms   | 55 ms | 72 ms |
