---
title: TDH Replication Manager
layout: page-fullwidth
permalink: "/TDHReplicationManager/"
---

# Motivation

One of the core principles of the Thali model is 'externalization of data'. That is, data should live independently of apps so that the user can share the data across devices and applications how they want. So in general it is a non-goal for apps to keep their data locally. We want them to externalize it and we want them to externalize it specifically to the Thali Device Hub (TDH).

In fact, our general desire is that all communication between Thali devices be mediated by the TDH. This is not required. Thali apps have their own keys and are free to establish their own relationships. But our default assumption is that the TDH is the data hub for each device and all information communicated between apps on the same device and between different devices is expected to transit the TDH.

To make this model workable we need to enable Thali apps to manage the TDH's synch behavior.

The replication manager's job is just to handle deciding who should synch with the TDH, when and how. It is not the replication manger's job to determine who is allowed to synch (the security infrastructure will handle that) or how much they can synch (the DOS/data management infrastructure with handle that). So this spec will just focus on the core responsibilities of the replication manager.

# Background Reading

This spec assumes the reader is reasonably familiar with the CouchDB synch model. That is the difference between one time and continuous synch as well as push vs pull. [This](http://guide.couchdb.org/draft/consistency.html) is a very high level overview of CouchDB sync but I would also read [this](http://wiki.apache.org/couchdb/Replication) and the links it has at the bottom.

# Synching Scenarios

## Moving data between a Thali app and the TDH

Thali apps have to be able to pull data in and out of the TDH. This is handled via the CouchDB REST API and can involve either synch or query. In general this scenario is outside of the purview of the replication manager.

## Synching between TDHs

Synching between TDHs is what the replication manager exists to handle.

### Mesh Synch

A key scenario is a user who owns multiple devices and forms them into a mesh. In that case each TDH in the mesh needs to synch data with all the other TDHs. This should happen automatically.

### Chat App

A Thali app provides a chat room for the user to talk with their friends. The chat app provides the public key for each user to the replication manager along with the name of the chat database and asks the replication manager to establish continuous replications with all of those users. The chat app expects the replication manager to find the users (e.g. what transport, which device, etc.) and synch with them. When the app is in the foreground it expects that its replications will be very high priority since the user is waiting for them. When the app is moved to the background it accepts that its replications should become lower priority. The lowering of priority would be expressed in several ways. First, synchronization would stop being continuous and become periodic. Second, some contents (like attachments, such as pictures users post to the chat) would be synch'd with even lower priority, if at all. While in the background the app would still be able to be notified (via a local continuous replication) when new chat entries came in and show the user a notification. If, however, the user doesn't click on any of the notifications then eventually the app may decide to lower its synch priority even further or even just turn off the synchs all together. Additionally, if the app stops running all together (say on the desktop) then its synchs would stop running completely until the app came back. And if the app is uninstalled then its synch would be removed.

An interesting question for this app is - should it be push or pull based?

### Calendar App

A user opens their calendar app and creates a new appointment with a friend. The appointment is entered into the user's local calendar database. The calendar app has registered with the replication manager all the users who have any kind of permission to see the user's calendar. The calendar app will also want to be able to create filtered versions of the data that restrict what information can be synch'd. For example some users may only be authorized to see free/busy time, others might be able to see everything that is not marked as private (which would just show up as free/busy), etc. Note however that the person invited to the appointment may not have any permissions to the users calendar. It's highly likely that instead the calendar app will send a message to that user's Thali inbox with the invite. This is essentially a queuing problem and it's not the replication manager's job to manage how that invite gets delivered. The calendar app will also register with the replication manager a number of pull replications for calendars the user has permissions to.

In general the calendar app will want its synchs to be periodic and occur with reasonable priority at all times. If the calendar app is in the foreground it might ask the replication manager to execute its synchs immediately to get a refresh but generally calendar synching is high latency with relatively rare changes so it doesn't make sense to continuously replicate. What makes more sense is periodic replication. But even when the calendar app isn't in the foreground it will still want its replications to continue since rendezvous with other TDHs can be challenging (especially if the remote TDHs are purely mobile and largely on cell and so not frequently available).

### Cell only TDH

A TDH on a mobile device that primarily connects to the Internet via cell is going to be hard to get a hold of. It simply cannot afford to always be online. It needs to carefully ration its network usage, both for cost (many cellular carriers, especially in the US, cap usage and charge hefty fees for usage over the caps) and battery reasons. So we need to be able to manage priority and frequency for both outgoing and incoming synchs. We also need to provide some way for other TDHs to up their chance of finding the mobile TDH.

Note, of course, that for users that have a mesh of devices we will want some way for them to indicate that perhaps the PC that is always plugging into the wall and connected to a wired Internet connection is preferable to the user's phone.

# Synch configurations

There are a number of settings that can be configured when synching. This section is intended to capture those settings.

## Synch Frequency

<dl>
<dt> One Time</dt>
<dd> A synch (push or pull) happens exactly once and is not repeated</dd>
<dt> Continuous</dt>
<dd> A synch is considered 'live', that is, the TDH will constantly monitor the remote TDH for any changes and instantly update them. One can think of this conceptually as keeping a connection always open to the remote TDH (although some implementations don't handle it that way). Typically continuous replications don't survive a server reboot.</dd>
<dt> Periodic</dt>
<dd> This is not part of the official CouchDB model so we would have to add it. But unlike a continuous replication this does not keep a connection open. Instead it just repeats one time replications on some schedule. </dd>
</dl>

## Synch Direction

Synch can be either push or pull.

## Synch Priority

How many synchs are we willing to run simultaneously? If the number is lower than the number of queued synchs then how do we prioritize which synchs to run?

This also applies for incoming change requests. We probably want to be able to prioritize incoming requests based on the kind of data they are handling. A request for say any mail messages may be higher priority than a request for the latest party photos. Given limited battery and network bandwidth managing priority for incoming requests matters as much as managing outgoing.

## Synch Filtering

When synch'ing there are a number of potential filter options. See [here](http://docs.couchdb.org/en/latest/api/database/changes.html) for a list of potential filters that can be applied when getting changes. Note that CouchDB doesn't provide an official way to specify these filters on a replication request so we will have to add something.

Also note that filtering goes both ways. A filter on a pull replication means "I only want to see X" while a filter on a push replication means "You only get to see X". The replication manager needs to be able to apply filters it has been given. And of course both can apply at the same time. That is, a remote TDH can make a filtered request on the change feed and what the local TDH produces on the change feed can itself be filtered based on the requester's identity.

## Synch Routing

When a Synch request is made either a specific URL can be specified or alternatively the request could just specify the identity of the user and or group to be synch'd with and it's up to the replication manager to resolve this. For example, the replication manager could check the address book to see what TDHs the user has and try them in some order. The replication manager would also be expected to handle things like having a user available say only on wi-fi direct or via Tor or both and make choices about which mechanism to use to move data. The same things for groups, the replication manager could look up all the members of the group and then sync the database with them.

That way an app can just say "Sync database calendar with all members of the calendar group" and the manager will handle this as the group membership changes, as member locations (e.g. devices) change, etc. Makes life much simpler for the app.

## Synch Failure Management

What happens if a synch with a particular TDH hasn't succeeded in a very long time? What happens if a synch request gets flat out rejected? How does the app stay abreast of this? We need some way for the replication manager to communicate with apps about what is going on with their requested synchs.

## Synch Rendezvous

Mobile devices, especially if they are on cell, can't afford to constantly be available. So they need to be able to publish their own policy that says things like "I will try to connect if I'm on cell at 0:10 minutes after each hour and will be available for 5 minutes. Or whatever. The replication manager would use this data when trying to replicate with hard to reach TDHs.

## Synch Recycling

Determines that a synch is no longer needed. This could happen because an application specifically asks for a synch to stop or because the application that requested the synch either no longer has the necessary permissions involved in the synch or is no longer installed.

It's highly likely that for robustness we need to put expiration on all syncs and delete them at the end of the expiration period. It's fine for apps to just regularly re-submit the syncs that they want so this shouldn't be a heavy burden.

## Synch Policy

In the scenarios above the apps want different things to happen with regard to synching (in terms of priority, filtering, etc.) depending on their status (foreground, background, running, etc.) Is it up to the app to make these changes itself or is this something the replication manager should detect?

## Synch Review

Although this is ideally something that no sane user should ever (EVER) have to look at we almost certainly need some kind of UX to let users review all the registered synchs and their configuration.

# What to implement

Every feature we add increases the distance between us and shipping. So we want to find the absolute minimum set of features we can get out the door with and then only add new features based on real world experience. Or as the old saying goes, the product is done when there's nothing left to cut.

So what if there isn't a replication manager at all? What if each app is responsible for managing its own replication? In practice this is likely to be a mess. First off, at least in Android land apps sorta run forever, even apps in the background can periodically (and cheaply) get a shot at a few cycles. But on the desktop this isn't the case. Similarly if Android is rebooted then the apps aren't running. So apps (especially for things like email and calendar) which really need to be synching all the time having the apps manage the replication isn't going to result in a good user experience. It means until the apps get started again there will be no updates of the data they need.

There is also the issue of prioritization. Bandwidth is at a premium and so we do need to make choices. That's hard to do if every app is running its own synchs independently of each other. And if the apps are going to coordinate then we just created the replication manager via the back door.

So it looks like we really do need a replication manager. But let's see how little we can do.

## Replication persistence

Our first feature is the ability to intercept a replicate request (we already have the right hooks in CouchBase) and persist the silly thing. Hopefully this is no more than a write to a local database.

## Replication manager

This is the code that actually reads from the replication database and starts to issue push and pull synch requests. Initially it can be dumb as a post. It just reads whatever is there and does it. No policy. The main issue here is to figure out what kind of synchs we want to support. I would suggest that we only initially support periodic synch, not even continuous. This is not super efficient but it's easy because basically the replication manager just fires off the request and forgets about it until its time to do it again at whatever period has been specified.

## Mesh

We need a bit of code to make sure that we periodically sweep the address book, find entries for the users other devices in the same mesh and make sure that the replication database has orders to do a full synch with them. The main challenge here is that we need a way to specify in the replication request that we need *all* remote DBs synch'd.

## Figure out who to replicate with library

We need a library that can, for a given identity and database name, figure out the best way to replicate. The idea here is that each user will have a 'me' database that their friends can synch that will say things like "Here are all my TDHs and here is the priority order and their individual availabilities". So this library would use that data to figure out which TDH to replicate with. The library would also manage connectivity failures and try the next entry in the list.

## Rendezvous

We should then enhance the library to be able to deal with the rendezvous feature. This is actually kind of tricky because it eats battery. Imagine a user needs to synch with 15 different DBs and each is available over a different time period? The device could drain its battery spending an hour communicating. We need to think carefully about this.

## Filtering change requests

This is really about testing. That is, making sure that CouchBase supports the features we need.

## Filtering change responses

Again, this is really a CouchBase feature but we may need to add it.

## etc.

There is obviously more we can do but this is probably a pretty good start.
