---
title: Thali and Tor Hidden Services
layout: page-fullwidth
permalink: "/ThaliAndTorHiddenServices/"
categories:
    - architecture
---

# Introduction

Right now using Tor Hidden services is hard for us because HSs are clearly designed for stationary services not mobile ones. To help figure out what would make things better we identify our key scenarios and requirements here. The hope is that by being very clear on what we think the issues are we can help to drive getting them address.

# Terminology

<dl>

<dt> HS </dt>
<dd> Tor Hidden Service </dd>
<dt> OP </dt>
<dd> Tor Onion Proxy </dd>
<dt> OR </dt>
<dd> Tor Onion Router</dd>

</dl>

# Scenarios

## Walkabout

A user is hosting a HS on a mobile device. The user is actively moving around. First they start on cellular while driving into work and then switch to wi-fi when they get into their office building and then back to cellular when they walk outside and back to wi-fi when they go back into the building. During this time clients are actively talking to the HS on the mobile device.

## Popup

A user is hosting a HS on a mobile device. The mobile device is battery constrained and so doesn't want to make excessive use of the network because it is a major source of battery drain. Therefore the HS running on the device only makes itself available during defined windows of time. Say 10 minutes out of the hour. So a client wanting to talk to the HS will first just try to connect. If they cannot then they will assume the HS is in battery saving mode and will try again during the window when the HS is supposed to be available.

# Requirements

To make the language actionable we are using IETF RFC style MUST/MAY/SHOULD language.

Note that requirements are a lot like ponies, everyone wants one but not everyone can get one. This list should be treated as a wish list and paired down based on interest, capabilities and priorities.

Each requirement is given an identifier, the identifier isn't necessarily meaningful but is made to make it easier to refer to the requirement.

<dl>
<dt> ReqContinuousTCP</dt>
<dd> Tor MUST provide a mechanism to enable a client and HS to maintain their TCP connection even as the HS (or client) actively change their local network addresses.</dd>

<dt> ReqQuickDiscovery</dt>
<dd> A HS MUST be able to frequently switch its underlying network address without impairing its ability to be discovered by clients.</dd>
</dl>

<dl>
<dt> ReqCleanDisconnect</dt>
<dd> A HS MUST be able to make it clear that it is not available for connection to clients.</dd>
</dl>

# Features

## Sticky Descriptors & Expiration

Right now if a HS intentionally takes itself offline (per the popup scenario) its old descriptor will stick around for awhile. We've already seen cases where a HS goes offline, comes back online and a client manages to get the old descriptor not the new one. The result being that the client spends a long time trying to connect to a discarded descriptor. This behavior makes the ReqQuickDiscovery requirement impossible to meet.

A possible solution to this problem is to allow HSs to put explicit expiration dates in their descriptors. A HS that knows its mobile may use an expiration date that only lasts minutes. If this expiration date is honored both by directories and by client caches then old descriptors will quickly get flushed from the system. Of course the cost of this is increased descriptor traffic to the directory servers.

<dl>
<dt> FeatureDescriptorExpiration</dt>
<dd> Tor MUST extend the HS descriptor to include an explicit expiration date after which the descriptor MUST be discarded both by the HS directories and client caches.</dd>
</dl>


## Discarding Descriptors

<dl>
<dt> FeatureDeleteDescriptor</dt>
<dd> Tor MUST provide a command to instruct HS directories to delete a descriptor immediately.</dd>
</dl>

It might be possible to do this by advertising a hidden service with no rendezvous points. The spec calls out that this should be legal but I haven't figured out a way to actually do it using the TorCTL protocol. So this request might actually really be a request to enhance the control protocol to explicitly specify advertising hidden services with no rendezvous points. What's nice about the 'no rendezvous points' approach is that it would work with the existing protocol but would require updates to the clients to both be able to send the empty descriptor and to honor its meaning.

It's worth noting that FeatureDeleteDescriptor is not a substitute for FeatureDescriptorExpiration. The reason is that mobile clients and laptops can lose connectivity unexpectedly and/or be shut off without warning. So we can't rely on them being able to send out an empty descriptor before going offline. As such we still need expiration to handle that case.

## Clean local cache

A nice short term hack would be to provide a way to remove the descriptor for a HS from the local client cache. This is essentially an extension to TorCTL.

<dl>
<dt> FeatureDeleteLocalDescriptor</dt>
<dd> TorCTL MUST be extended to include a command that allows any cached descriptors for a named HS to be deleted and retrieved afresh</dd>
</dl>

## Switching connections to the first hop OR

Now I'm completely leaving my comfort zone. I haven't done enough work with the Tor control protocol to be confident in what I'm about to say. But I need to start somewhere so please go easy on me. :)

<dl>
<dt> FeatureSwitchFirstHop</dt>
<dd> The Tor protocol MUST support a OP changing the TCP connection its using to the first hop OR without losing any state or connections.</dd>
</dl>

So the idea is that the OP can detect that it has switched networks and reconnect to the first hop OR with all the existing state information and just keep on going. Presumably we would have to set the TCP time outs for end to end connection to be generous to make this work but in theory it should work just fine. Also to the extent that HSs set up connections in the same way as clients, that is, they build them towards their rendezvous point, then any protocol to switch the IP of the OP to the first hope should 'just work' for HSs as well.

Honestly I'm not sure if this doesn't already work and maybe we just need to update the OP to support a 'switch network' command? That is, what would an OR do if it lost the TCP connection to the OP and then got a new TCP connection with all the right keys, channel IDs, etc.? One suspects that the OR would tear down the channel as soon as it lost the connection. If so, that is the behavior we need to change. Perhaps we need either a time out or the ability to explicitly tell the OR that the OP is about to change connections?
