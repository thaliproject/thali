---
title: Thali and Couch
layout: page-fullwidth
permalink: "/ThaliAndCouch/"
categories:
    - architecture
---

## Introduction

<dl>
<dt> Thali's Mission</dt>
<dd> Make the web peer to peer</dd>
</dl>

In other words, users should be able to run a web server as easily as they run a web client (Read: browser). This means they can set up the web server on their devices and have it run.

This document is targeted at developers and intended to provide a high level overview of the Thali architecture.

## Authentication

Running a peer to peer web means accepting incoming connections and this means we need a standardized way to authenticate who is making requests. Thali's answer to this problem is the [Httpkey URL Scheme](/HttpkeyURLScheme). This scheme essentially says:

1. Security principals (read: users, but this could also be services, hence the more generic term security principal) are identified with public keys.

1. Public keys are exchanged using TLS mutual auth with self signed certs

There are certainly scenarios where anonymous access is desirable but Thali requires that all communication in all circumstances without exception MUST go over TLS and at a minimum the server MUST present its identity via TLS. So the client can be let off the hook (e.g. not present an identity) but not the server. Note, however, that the server is free to generate as many identities as it needs so its anonymity can also be protected. The use of Tor hidden services (see below) also affords anonymity for the server.

The use of HttpKey means that Thali doesn't work with the open web as we know it today. That is considered acceptable because as recent events have amply demonstrated the CA model underlying the existing web isn't fit for purpose. As the web develops past this model we expect they will end up adapting something like HttpKey. And, of course, if they come up with something better, we'll adopt that.

## Synching, CouchDB and well behaved Thali apps

Synching has turned out to be foundational to Thali. There are several reasons for this:

<dl>
<dt> Most users are expected to have multiple devices </dt>
<dd> Users may have a phone, a few tablets, a few PCs, etc. and we expect users will require that these devices all be in synch with each other. In other words we expect users to find it unacceptable that they edit say a photo on their phone and that photo in its new state isn't available on their tablet.</dd>

<dt> Devices are often offline</dt>
<dd> This can be due to battery limitations, lack of network connectivity at a specific location, software updates, etc. As such to maximize the available of the user's services to 3rd parties (read: their friends) we want to be able to run their services on all of their devices so a friend can connect to whichever of the user's devices is running. For that to create a 'good' experience we end up with the problem in the previous point.</dd>
<dt> Most services we think users will want to use Thali for are inherently synch based</dt>
<dd> Messaging (read: email), presence, blogging, micro-blogging, photo sharing, location sharing, etc. are all essentially synch services where subscribers synch from a source.</dd>
</dl>

Given the loosely connected nature inherent in user devices a synch solution had to be multi-master, that is, it has to assume collisions. To this end we adopted CouchDB as our synch protocol. It is fully HTTP based and about as simple as it's possible for a multi-master synch protocol to be.

The idea from the get go is that each device would have exactly one CouchDB server running on it and all data would be stored in that singleton instance. By having the data in a central store a user's data is separated from the apps that create/read it. This vastly simplifies sharing data between apps (thus allowing apps to add new value) and allows the user the freedom to switch apps. If they don't like a particular calendar program, for example, they can switch to another. Of course nothing is ever free. This centralized approach only works if apps adopt and use standardized formats. But the point is to make it as easy as possible for well behaved apps to behave well. Nothing can stop an app with bad intent from hiding data, intentionally using non-compatible formats, etc. The hope is that such apps will be publicly identified and avoided.

But once we had the CouchDB server it became quite natural to ask - well what about synching with other user's devices? In the grand tradition of "When all you have is a hammer, everything looks like a nail" it seemed sort of obvious to use the CouchDB server as the primary communication mechanism. That is any Thali apps that need asynchronous high latency communication (e.g. our previous list above) can do so through the CloudDB instance. So the idea is that the Thali App tells the singleton CloudDB service to either allow certain public keys (aka Security Principals) to synch certain data and/or for the CloudDB server to pull data from remote CloudDB services.

## Thali, the Internet of Things (IoT), local discovery and high latency meshes

As explained in detail [here](/ThaliAndIoT) it turns out that Thali's technologies are extremely useful for IoT scenarios, especially industrial scenarios. This has led us to reprioritize two technologies that were always part of the Thali vision but now are getting done earlier.

The first is local discovery. That is, using BLE, Bluetooth and Wi-Fi to discover local devices in areas without any cellular of centralized Wi-Fi support.

The second is high latency meshes. Thali always wanted to support high latency meshes because they are perfect to be run through mix networks and thus provide a higher level of security than Tor is intended to provide. But it turns out that the exact same technology is extremely useful in industrial IoT scenarios where those little mobile Wi-Fi hotspots called humans carrying phones make for a good network to move data around.

## Firewalls & NATs

As a practical matter firewalls and NATs make it between difficult and impossible to host services behind them. The good news is that there are standardized efforts to address these problems, the bad news is that these efforts are either often unsupported by real world firewalls/NATs or not fit for our purposes (e.g. see [here](/StunTurnICEInvestigation)). However there is one solution that is free and widely deployed that does meet our needs in many, although not all, cases - [Tor hidden services](https://www.torproject.org/docs/hidden-services.html.en ). So we will be adopting Tor hidden services as our foundational mechanism for exposing server endpoints on devices.

## Device Discovery on the Internet

Another problem Thali has to address is how does one device find another? In practice user devices change their IP addresses as they switch between network types (e.g. cellular, Wifi, wired) and locations. Traditionally DNS would handle this problem but as a practical matter DNS requires money in order to register a name and faces security challenges that make it problematic (yes, even with DNSSec). However our adoption of Tor hidden services for dealing with Firewall/NATs also provides us with a solution to our discovery problem.

As explained in the link above Tor hidden services allow one to map from a public key hash to a network socket. In other words in Tor hidden services a public key hash plays the same role as a DNS name does in DNS. So users can generate identities and leverages those identities as a discovery mechanism.

## Architecture Overview

Because Thali is finding communities, like IoT, who need parts of its capabilities we are trying to refactor ourselves into a useful set of libraries. See [here](/ThaliAndIoT) for our current thinking on how we will break Thali up into useful sub-pieces. The rest of this section applies to technology we hope to deliver after we have delivered on the IoT scenarios, specifically, the Thali Device Hub.

<dl>
<dt> Thali Device Hub</dt>
<dd> A server application running locally on a user's device that hosts a CouchDB server and manages replicating the content of that server with other authorized entities. It's purpose to provide a central store of user data so all applications can get to all data. A central coordinator for replication so that limited battery and network can be leveraged in the best way across all the types of data on the phone. A central Tor onion proxy so multiple don't have to run on the device. A central enforcer for access control and a central enforcer to protect the identity store.</dd>

<dt> Thali Application</dt>
<dd> A local application running on the same device as a Thali Device Hub</dd>
</dl>

```
                     -------------------------------------------------------------------
                     |                    Local Thali Device Hub                       |
                     |                                                                 |
Local Thali App -->  | Local CouchDB Singleton Service  <-- Local Replication Manager  |
                     |           /|\        |                                          |
                     |            |         \                                          |
                     |            |          ----------------------------              |
                     |            |                                     |              |
                     |            |                                    \|/             |
                     | Local Tor Hidden Service Proxy           Local Tor Client       |
                     -------------|-------------------------------------|---------------
                                 /|\                                    |
                                  |                                     |
                                   [ Tor Hidden Service Infrastructure ]               
                                  |                                     |
                                  |                                    \|/
                                       Remote Authorized Entities            

```

## Talking to the Thali Device Hub

For now, mostly to make things easy, all communication between an application and the CouchDB instance on the same device will occur via the network using localhost. This means, amongst other things, that a [httpkey URL](/HttpkeyURLScheme) will be used. Yes, it means that we will be running a full SSL stack locally. Yes this is a bit silly and some platforms provide much better tools to handle this. But for right now the simplicity of using the same stack locally and remotely is worth the inefficiency.

This means that any application that wants to talk to the CouchDB singleton service from Thali needs to:

1. Get a cert that the singleton service will recognize

1. Find out the port the singleton service is on

How this works in practice will depend on the specific OS. We investigate the issues at some length [here](/SecuringCordovaAndNodeJs). The difference however in that in the previously linked article the concern is how to connect a Cordova WebView and its associated node.js server. In this case we are talking about how to connect any arbitrary application on the device and a central Thali Device Hub.

### Access control on the Thali Device Hub

Each database will have associated with it a Read ACL and a Write ACL. Anyone with a write ACL on a database is allowed to create views on that database.

Each view will have associated with it a Read ACL.

ACL membership will either be individual principals or groups. Both principals and groups will be stored in a DB in the Thali Device Hub.

### Replication - The Replication Service

For now any database created on a device will be replicated to all other devices by the replication service. Due to the often unique requirements of Thali replication including handling disconnected remote entities, Tor, httpkey, quotas, etc. we will almost certainly have our own bespoke replication service that can manage these issues.

For now all replication will be 'last writer wins'. That is, if there is a conflict whichever entry has the newest time stamp will win.

None of this is enough of course. We will inevitably need databases that are local only, that have different conflict resolution policies, that deal with quotas (which we aren't going to implement immediately), etc. But one step at a time.

For a lot more details on TDH replication please see [here](/TDHReplicationManager).

### Discovering users

An obvious question is - how the heck do people communicate? Are folks supposed to memorize 4k RSA keys? Or maybe just the hashes? In general we expect discovery to happen in a number of ways:

#### Email
Yes, email. Seriously. No it's not secure. But it's not quite as insecure as it sounds. We expect Thali to encourage (in reasonable situations) different users to compare notes on the identities they know. This includes fun things like comparing [pet names](http://www.erights.org/elib/capability/pnml.html). So in most cases if someone does a MITM on the identity exchange it will eventually be discovered.

#### In person with QRCodes
This involves two users who want to exchange identities having their devices display QRCodes and then pointing their device's cameras at each other. On balance this is probably the most secure way to exchange identities but when we implemented it we found it too be too fiddly for practical use. We do have ideas on making it better though.

#### Comparing shared value
This is what's known as a coin-flip or commitment protocol. A common example is Bluetooth's secure simple pairing protocol with numeric comparison. Two people choose to pair and each of their devices display a number. The two users then make sure the values are the same and if so accept the identity exchange. This approach is not as secure as QRCodes. If the compared number is say 6 digits then there is a 1/1,000,000 chance of an attacker successfully attacking the identity exchange. But it's good enough for most scenarios and much easier than QRCodes.

See [here](http://www.goland.org/coinflippingforthali/) for details on how we can implement this system.

#### In person with Near Field Communication (NFC)
In theory we could also exchange via NFC. There are potential MITM attacks so it's less secure than QRCodes. In practice we have stayed away from NFC because of the difficulty in identifying the NFC pair points on two devices. Watching two people trying to exchange data between different phones using NFC seems even more painful than QRCodes.

#### Via Groups
We expect that it will be normal for people to share group membership data with their friends and that this data will be stored, compared and made available for discovering new identities.

#### Directories
In certain scenarios, corporations are an obvious one, a user might have a directory of identities it trusts within a particular context. E.g. if I'm going to communicate to someone at work and the corporate directory says what their key is then I'm going to trust it.

We will be providing a standard JSON format hosted in the Thali Device Hub where a user can provide information about themselves that they want to publish. This will include things like their messaging endpoint (e.g. where they can receive unsolicited messages).

## Thali Device Hub

There is some common UX that the Thali Device Hub needs to support. Minimally it will need a UX to handle pairing devices as well as exchanging keys with friends. It will need some kind of status page to help users when things go wrong. It will probably also need some kind of storage management UX to deal with stores that get too big.

[TDH Replication Manager](/TDHReplicationManager) provides more details on how we manage replication.

## Thali Application

A Thali Application is a web application and so it's expected to be able to talk both directly to other Thali Device Hubs (and potentially other Thali Applications if they want to expose a direct server interface) as well as to its local Thali Device Hub. The protocol in all cases is the same, HTTP over TLS. Yes, even local Thali Apps talk to their local Thali Device Hub that way. We might eventually provide another mechanism for local to local communication but for now we want to keep things simple.

This means that the Thali Device Hub's "api" is its protocol interface, so anything that can speak CouchDB and authenticate via TLS mutual auth (plus Tor if they aren't on the same device as the Thali Device Hub they want to talk to) can play. So we don't have any restrictions on things like programming language.

But we also don't want to make people go through all the pain of setting up things like mutual SSL Auth validation and Tor handling (for off device connections). So we provide a number of 'out of the box' enabled libraries to make writing Thali Applications easier.
