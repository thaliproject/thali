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

During the time window T2 - T1 a random set of devices will come within range of each other. There is no way to predict ahead of time who those devices will be and if any of them happen to be in a given device's target set. In addition there is no reliable way before time T1 for all devices, or even the devices in a particular device's target set to coordinate together. It's possible, for example, for device x to have an entry in Mx for a device it's literally never talked to before. This would happen, for example, if at some point before T1 Device x sent a message to Device y addressed to y and z. Device y has never seen Device z but now it has a message it needs to forward to z if it runs into that device.

The goal then is to figure out how to enable devices to discover if someone who is in their target set is within range without disclosing their identity to anyone who is not in the target set and without disclosing the contents of Lx beyond the obvious fact that if Device x and y talk then x must have been in Device y's target set and y must have been in Device x's target set.

# Beacons
See the perf work I'm doing below. Still not done.

# Hot List
Exy = Timestamp + (HMAC-SHA256(Sxy, Timestamp))+

The obvious problem being that you have to generate a HMAC for every entry in the address book and see if any match. So with a large address book that is just not going to work. But in most cases Dunbar's number saves us. Most people don't regularly work with more than a tiny number of people and pretty much can't work with more than 150 or so people meaningfully. So this means that most folks can have a "hot list" of frequently used addresses and if someone knows they are on someone else's hot list then they can just use that list. This is an optimization but it's a powerful one.

# Q&A
# Why can't users just announce their keys publicly? Maybe encrypt them with a group key?

Looking at our informal definition we can imagine that one way for Bob to find Alice is for Alice to just announce her public key over BLE. In fact, if everyone would just announce their public keys over BLE everything would be so much simpler! Everyone can see exactly who is around, figure out if they have something for them and then transmit it. The problem is that now everyone is announcing their identity to anyone who will listen.

The usual work around at this point is some kind of group key. For example, imagine that what we announce is someone's personal key but first we encrypt it with some group key that everyone at work has a copy of. This way locations will only be understandable to employees, nobody else will know who is announcing what.

The problem of course is that both Bob and his harasser are employees of the company and so the harasser will have the group key and be able to discover Bob.

There are variants on the group key approach but they tend to fail for reasons having to do both with the ad-hoc nature of communication in our scenario as well as for space reasons.

For example, imagine that Alice creates a new discussion group with Bob and they agree on a group key just for that discussion. Later on Alice decides to add Bob's harasser to the discussion group and shares the group key with the harasser. Since this is an ad-hoc networking scenario there is absolutely no guarantee that Alice ever had the opportunity to tell Bob about this. And presumably Alice has no idea that Bob has put a ban on his harasser. So if Bob just looks for the group key, thinking it's Alice, he could be inadvertently advertise his location to his harasser.

And, of course, our discovery channel is quite limited. So if there are lots and lots of groups there won't be enough bandwidth to announce them all.

# HMAC-MD5 vs HMAC-SHA256
I started this work out just using HMAC-MD5 both because of size but more importantly because of performance. CPU is at a premium. I would also (naively) argue that most of the attacks on MD5 wouldn't work properly in our scenario anyway because of the limited data size. But using SHA256 would make it easier for the crypto dieties to be happy with us and it's not really a size issue since we can just truncate the hash. But what about perf? Given that we are using the AES-GCM approach perf really isn't that big a deal to be honest. We just aren't using that many hashes. But for the 'hot list' approach the perf really matters since we have to do a lot of hashing.

So I ran some quick tests. In the first test I checked 20 hashes against an address book with 1000 keys where all comparisons fail. I repeated the test 100 times.

|Algorithm | Min | Median | Max|
|----------|-----|--------|----|
|HMAC-MD5  |131 ms | 136 ms   |230 ms |
|HMAC-SHA256 | 152 ms | 179.5 ms | 454 ms |

For the next test I generated 20 HMACs using 20 different keys. This simulates creating beacons in an announcement.

|Algorithm | Min | Median | Max|
|----------|-----|--------|----|
|HMAC-MD5  |1 ms | 2 ms   |26 ms|
|HMAC-SHA256 | 2 ms | 3 ms | 26 ms|

The perf difference for generating tokens is noise. The slightly bigger issue is with validating them where using SHA256 looks to reduce performance by roughly 30%. That is small enough (and my tests are ham fisted enough) that I suspect the real world perf difference will be noise. So let's make the crypto gods happy and just use SHA-256 with a truncated output to 128 bits to save space on the wire.

# Comparing Addressbook Size Invariant Options - HMAC vs HMAC-AES-CBC vs AES-GCM
E(HMAC)xy = Ke + Timestamp + (HMAC-SHA256(Sey, Timestamp) + HMAC-SHA256(Sxy, Timestamp))+

E(HMAC-AES-CBC)xy = Ke + Timestamp + IV + (HMAC-SHA256(Sey, IV + Timestamp) + AES-CBC(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp)))+

E(AES-GCM)xy = Ke + Timestamp + IV + (AES-GCM(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp))+

I put the IV into the HMAC's (where there was an IV) on the advice of Tolga Alcar who suggested this would add some additional randomness to the HMAC-SHA256 output. Since it's cheap to do and doesn't increase the size of the output this seemed reasonable.

Tolga also said that using the ECDH values (for Sey and Sxy) directly is a bad idea. That the output secret is effectively a point on the curve and that the is a non-uniform space (e.g. the points are predictably on the curve). He suggested looking up NIST SP800-108 and looking for the simplest possible hash based KDF and setting its counter to 1 and using that. Effectively just hashing the ECDH secret so that we map it into a uniform space from a non-uniform space. Bouncy Castle suports KDFCounterBytesGenerator so I can add that in for testing purposes.

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

To compare each approach I generated 20 entries in a single presence announcement. The last entry will match against the receiver. The receiver has an address book with 10,000 entries in it. The only reason for even specifying the number of entries is to give us a meaningful comparison between the cost of the HMAC approach versus everything else. We will intentionally match on the last entry in the address book to provide the worse case scenario. Each test was run 100 times.

| Approach | Min | Median | Max |
|----------|-----|--------|-----|
| HMAC/HMAC| 1691 ms | 1694 ms | 1741 ms |
| HMAC/AES-CBC| 22 ms |  36 ms |  73 ms |
| AEC-GCM | 24 ms   | 52 ms | 59 ms |

# What about ECIES?
E(ECIES)xh = Ke + Timestamp + IV + (ECIES(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp))+

One can reasonably argue that AES-GCM and ECIES are morally equivalent. And I would expect them to produce outputs of comparable size and processing time.

But in practice that doesn't seem to be the case. For example, the ECIES output I generate in my sample code is 133 bytes as opposed to the 48 bytes generated by AES-GCM. But I strongly suspect that this isn't a fair comparison because I'm pretty sure my configuration for ECIES is wrong. I don't think I'm using comparable hash functions or KDFs. My suspicion is that with the right configuration AES-GCM and ECIES would produce similar results.

But I have to admit my limits. I found hacking through the Bouncy Castle code really painful. Docs and examples are few and far between and out of date. I found one [test file](https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/jce/provider/test/ECIESTest.java) and one [mail thread](http://bouncy-castle.1462172.n4.nabble.com/ECC-with-ECIES-Encrypt-Decrypt-Questions-td4656750.html) that even discuss ECIES. Not much else.

So my current thinking is basically that ECIES is not a standard, it's not supported in the same way in different environments, the Bouncy Castle implementation is tricky and the AES-GCM approach seems to work well using widely available crypto primitives. So my plan is to bring both ECIES and AES-GCM to the crypto gurus at work and let them tell me what they think I should do.
