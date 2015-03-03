---
title: Thali
layout: default
---

**Thali** is an open-source software platform for creating apps that exploit the power of personal devices and put people in control of their data.

You build Thali apps for Android, Windows, Linux, and Mac using HTML5 or native technologies. Thali apps are:

- **Secure.** Always communicate on encrypted channels.
- **Synchronized.** Use and replicate JSON data and binary attachments. 
- **Server-optional.** Communicate with peers that may, but need not, live in the cloud.


# Why Thali? 

Personal devices can store our data, exchange it with trusted peers, and sync to the cloud. For most developers, cloud sync is a given. Thali developers use the cloud when needed, but can alternatively use Thali in peer-to-peer mode. That means app deployment can scale cheaply and users' data can live primarily on devices they control.

# Thali Scenarios 
**Internet Of Things** How do devices find and communicate with each other regardless of the availability of a wi-fi or cellular infrastructure? It turns out connectivity is a big problem for IoT (see our [scenarios](nodeondevices)) and the peer to peer web is here to help. See [here](http://thaliproject.org/ThaliAndIoT) for our current product plans.

**Peer-to-peer social** For conventional social apps that are cloud-based and ad-supported, users' data is a product that's sold. For Thali apps, users are customers who own and control their data. Here are some [scenarios](PeerToPeerSocial).

**Secure communication** Thali apps always use mutual SSL authentication, so users know they're always communicating with trusted parties on encrypted channels. And because Thali uses the Tor network, they also know that their communication resists surveillance. Here are some [scenarios](SecureCommunication).

# How?
On the wire we create our peer to peer web using:
* CouchDB's HTTP based synch protocol, 
* public keys for identity, 
* mutual TLS auth for authentication and secrecy, 
* Tor Hidden Services for traffic analysis protection and NAT/Firewall penetration,
* BLE/Bluetooth/Wi-Fi for local peer to peer communication, and
* Our soon to be invented HTTP based protocol for high latency mesh formation

Our software is based on:
* Cordova,
* a Cordova plugin to provide local Node.js support,
* PouchDB,
* OpenSSL, and
* the Tor Onion Proxy

# Who?

Thali is being actively developed by Brian Lambert, Jukka Silvennoinen, Matthew Podwysocki, Srikanth Challa, Wenhui Lu and Yaron Y. Goland for Microsoft. We love all sorts of [contributions!](WaysToContribute) The previous page also contains lots of great ways to track what we are doing.








