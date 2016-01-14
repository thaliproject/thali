---
title: Replication Across Discovery Protocol
layout: page-fullwidth
permalink: "/ReplicationAcrossDiscoveryProtocol/"
header:
   image_fullwidth: "header.png"
categories:
    - technologyinvestigations
---

In Thali local discovery based replication when a peer has information for other peers it will raise notification beacons for those other peers. Without those notification beacons there is no reliable way for the other peers to find the first peer. This presents a problem however when there is a screw up. 

For example, peer A advertises for peer B. Peer B sees the advertisement and starts a replication (remember, we are PouchDB based). Peer A goes out of range before peer B finishes replicating. Peer A doesn't have a good way of knowing this (the values regarding replication that Peer A puts into the _local DB on peer B are opaque) and so it doesn't know if it should continue advertising a beacon for Peer B or not. If it thinks peer B is finished (and peer B isn't) then it won't advertise a beacon and peer B won't be able to find peer A again when it gets into range.

Another example is a race condition where peer A raises a notification beacon for peer B and while peer B is synching another change occurs. If peer B isn't using live replication then it won't know about the new change and depending on how peer A is tracking things it may or may not know if peer B knows about the new change. If peer A thinks peer B knows about the change but peer B doesn't then peer A won't advertise a beacon and peer B will again be lost.

To get around these situations at a minimum A needs to know what is the last sequence number that B knows it is supposed to sync up to. But this has to be a handshake. In other words B has to say to A "I know I'm supposed to replicate at least up to sequence ID X". By getting this message directly from B now A knows that B is aware of the need to replicate that far. That way A now knows it doesn't need to raise a notification beacon for B unless it has data for B beyond that sequence ID, unless of course the synch fails.

In other words A advertises for B, B shows up, confirms to A that is knows where it's supposed to sync up to and then there is a failure (like A or B walking away) in the middle of the sync. If A doesn't keep advertising for B how will B ever find A again to finish the synch? Remember, we have a privacy sensitive system where we hide people's location unless they are actively seeking a specific peer out.

There are two potential solutions to this problem. One solution is that B doesn't confirm to A where it's going to sync up to but rather confirms how far it has already synch'd. Then A tracks where it wants B to sync to and as long as B hasn't gotten there it advertises for B. The other solution is that B does confirm how far it knows its supposed to sync to A and if it doesn't manage to get there and needs to contact A then B will advertise for A. This is the inverse of how we normally do things. Typically if A is advertising for B then the semantics are "I A, have data for you, B". But with this second approach we would also be able to say "I B, want data from you, A".

Because beacon space is precious (we often have to move it over bandwidth limited transports like BLE) I have a preference for the second option. That is, B will confirm to A how far it knows it needs to sync to. A will then only advertise again for B if it actively has new data for B. B will try to sync and if it doesn't sync as far as it needs to then it will advertise for A. This reduces the number of tokens A has to advertise and lets B manage more carefully how many tokens it wants to advertise. That is, B can decide that it needs data from C, D and E more than A and choose for the moment not to advertise for A. I also like the idea that we put the burden on the device wanting the data to get the data.

Unfortunately implementing my preferred choice requires changing the token format to put in some kind of flag saying "I'm looking for you so I can sync data from you." Then there has to be a separate mechanism by which A, in this case, can send its location to B so B can use it to synch with A.

None of this is hard but we really have to start doing less or this project is never going to ship.

So instead I'm going to go with a design I don't like as much but which is much simpler. In this design B will tell A how far it has sync'd. A will then know what B has sync'd and will know if it needs to keep advertising for B so B can finish. This requires no changes

## How does A know how far B has sync'd?

There are two fairly obvious ways to handle this. One way is to play around with the CouchDB protocol. The other way is to invent our own protocol. As I argue below I suspect we would do better to introduce our own protocol.

### Hacking up the CouchDB protocol

You can read an overview of the core of the protocol [here](https://people.apache.org/~dch/snapshots/couchdb/20140715/new-docs-build/html/replication/protocol.html#algorithm) but the key first step of the protocol and the part most relevant to this conversation occurs in `_Local/<uniqueid>`. _Local is a magic document key prefix that all CouchDB databases must have. Any documents with a key under _Local are never sync'd. When a requester synchs with a database they create a document using a `<uniqueid>` that they use to record information about the replication, such as the last ID they have replicated to.  In other words, the exact value we need! But there are complications.

The first one is `<uniqueid>` itself. There is a voodoo protocol that is explicitly not inter-operable for generating the id. In our case we can cheat a little because we require authentication so we know who generated which unique ID. But the fun part is that there can be multiple such uniqueIDs from the same requester if they are using different profiles to replicate against the destination database. So which one are we supposed to read? All of them? Pick the latest?

Even better, as far as I can tell there is no standard for what the contents of `_Local/<uniqueid>` are supposed to look like. See [here](http://docs.couchdb.org/en/latest/api/local.html#db-local-id) for example. This is o.k. because the database the document is written on doesn't need (outside of Thali) to know what's in there.

But all of this means that if we want to use CouchDB's replication protocol to fix this we would have to define a bunch of things that aren't defined currently and that doesn't sound warm and fuzzy from an interoperability perspective. We would like to "just work" with any existing CouchDB implementation (we aren't always doing P2P replication, sometimes we do the old fashioned Internet based kind too).

So all of this makes me want to leave `_Local/<uniqueid>` alone.

### Our solution

Since we aren't using `_Local/<uniqueid>` we can't rely on PouchDB's replication to just do everything for us. But PouchDB helps out anyway by providing us which sync number it has gotten up to on the remote database via both the change and complete events on its replication object. So we can track the highest number and every once in awhile write it over to the remote database as a document under _Local.

The full ID of the document will be `_Local/<peer ID>` where peer ID is the device's public key (which is currently the only public key we have) encoded as a Base64 URL safe encoded value. See PubKe syntax as defined [here](https://github.com/thaliproject/thali/blob/gh-pages/pages/documentation/PresenceProtocolForOpportunisticSynching.md#generating-the-pre-amble-and-beacons) and then base64 url safe encode it.



The easiest way to do this would be to use our old friend _Local. Except this time we will specify how the ID is generated and what the format of the document is. Note that this doc has nothing to do with normal CouchDB replication and so won't interfere with anything.

Now the obvious way to create our ID under _Local on the destination DB (A in our example) would be to use a base64 URL safe encoding of B's public key. But there is a problem here that is worth thinking about. Right now we have a one to one relationship between a public key and a device. But it's really obvious that in the long term we will want to break that. We will want to have a set of devices that all share the same identity. But each of those devices won't necessarily have synch'd the same information with each other. So B's phone might have synch'd with A up to a certain point but B's laptop might not. Ideally B's phone and laptop would sync with each other but they might not be in the same place.

Our answer for this is that we will require devices to have unique keys in addition to users. Eventually when we put in personal meshes we will have to enumerate the device IDs. We will still store doc`s using the device's public key and it will be up to A to collect together all the devices from the same user and check across them to see if there is a need to advertise for that user's public key as a beacon.

So what this means for us is that for now we will make the document's ID in the device's public key (which is currently the only public key we have) encoded as a Base64 URL safe encoded value. See PubKe syntax as defined [here](https://github.com/thaliproject/thali/blob/gh-pages/pages/documentation/PresenceProtocolForOpportunisticSynching.md#generating-the-pre-amble-and-beacons) and then base64 url safe encode it.

For the JSON document the contents will be a field called "goalSync" and the value will be a number recording the sequence number that the remote peer is committed to syncing up to. Again, please note that in practice the remote peer will try to sync as far as it can. But by recording the number here the local peer at least knows a sequence number it can be sure the remote peer knows about and use that for the purpose of raising beacons.

Any unrecognized fields in the document MUST be ignored.

When accepting a request to create this document the target server MUST require proof that the caller is the owner of the public key supplied in the ID.

## How does A find out that B is looking for it so B can finish synching?

The scenario here is that B starts a sync with A and doesn't get to finish. So now it needs to find A again so it can finish the sync. In theory B could just advertise for A and when A connects to do a pull sync (presumably finding nothing) B could use the connection to do its own synching. This may sound odd to anyone who isn't familiar with the awful hacks at the center of our P2P stack that has us re-using the same underlying Bluetooth or MPCF connection to send data in both ways. But this won't work over WiFi. At best we would learn the remote peer's IP but not what port they are listening for connections over (which is a dynamic value).

The general approach would be to allow B to raise a notification beacon whose semantics are "I need to contact you to sync some stuff." If A was interested then it would provide information to B on how to contact it.

I'm thinking that we could extend the beacon format to add in a single byte of flags.



THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900. THIS IS A BUNCH OF MEANINGLESS TEXT TO MAKE STACKEDIT STOP JUMPING UP AND DOWN. PLEASE SEE https://github.com/benweet/stackedit/issues/900.
