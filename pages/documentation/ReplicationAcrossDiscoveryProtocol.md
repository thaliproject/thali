---
title: Replication Across Discovery Protocol
layout: page-fullwidth
permalink: "/ReplicationAcrossDiscoveryProtocol/"
header:
   image_fullwidth: "header.png"
categories:
    - documentation
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

So instead I'm going to go with a design I don't like as much but which is much simpler. In this design B will tell A how far it has sync'd. A will then know what B has sync'd and will know if it needs to keep advertising for B so B can finish. This requires no changes to our system beyond adding a document to our database (see below).

## How does A know how far B has sync'd?

There are two fairly obvious ways to handle this. One way is to play around with the CouchDB protocol. The other way is to invent our own protocol. As I argue below I suspect we would do better to introduce our own protocol.

### Hacking up the CouchDB protocol

You can read an overview of the core of the protocol [here](https://people.apache.org/~dch/snapshots/couchdb/20140715/new-docs-build/html/replication/protocol.html#algorithm) but the key first step of the protocol and the part most relevant to this conversation occurs in `_local/<uniqueid>`. _local is a magic document key prefix that all CouchDB databases must have. Any documents with a key under _local are never sync'd. When a requester synchs with a database they create a document using a `<uniqueid>` that they use to record information about the replication, such as the last ID they have replicated to.  In other words, the exact value we need! But there are complications.

The first one is `<uniqueid>` itself. There is a voodoo protocol that is explicitly not inter-operable for generating the id. In our case we can cheat a little because we require authentication so we know who generated which unique ID. But the fun part is that there can be multiple such uniqueIDs from the same requester if they are using different profiles to replicate against the destination database. So which one are we supposed to read? All of them? Pick the latest?

Even better, as far as I can tell there is no standard for what the contents of `_local/<uniqueid>` are supposed to look like. See [here](http://docs.couchdb.org/en/latest/api/local.html#db-local-id) for example. This is o.k. because the database the document is written on doesn't need (outside of Thali) to know what's in there.

But all of this means that if we want to use CouchDB's replication protocol to fix this we would have to define a bunch of things that aren't defined currently and that doesn't sound warm and fuzzy from an interoperability perspective. We would like to "just work" with any existing CouchDB implementation (we aren't always doing P2P replication, sometimes we do the old fashioned Internet based kind too).

So all of this makes me want to leave `_local/<uniqueid>` alone.

### Our solution

Since we aren't using `_local/<uniqueid>` we can't rely on PouchDB's replication to just do everything for us. But PouchDB helps out anyway by providing us which sync number it has gotten up to on the remote database via both the change and complete events on its replication object. So we can track the highest number and every once in awhile write it over to the remote database as a document under _local.

The full ID of the document will be `_local/thali_<peer ID>` where thali is just the string 'thali' and peer ID is the device's public key (which is currently the only public key we have) encoded as a Base64 URL safe encoded value. See PubKe syntax as defined [here](https://github.com/thaliproject/thali/blob/gh-pages/pages/documentation/PresenceProtocolForOpportunisticSynching.md#generating-the-pre-amble-and-beacons) and then base64 url safe encode it.

Inside that document we will create a JSON object which contains the property "lastSyncedSequenceNumber" with a number as a value recording the last sequence on the host device that the remote device claims to have synch'd to. Note that unrecognized properties in this document MUST be ignored.

Now the host device can track changes on that document for each of the peers it cares about and if the number recorded there is lower than the current sequence number then the host device can advertise for those peers.

#### Isn't this inefficient for shared databases?
Imagine we have peers A, B and C. They all share exactly the same database and are synching it between each other. Now imagine a situation where A has synch'd with B and vice versa. Some time later, during which time neither A nor B made any changes to their own local databases, both A and B sync with C. 

The end result is that A and B actually still have identical databases but there is no way for them to know that. All A knows is that it's in sync with C and that the last sequence number recorded on it by B isn't high enough. The same will go for B in regards to A. So now both A and B are going to advertise for each other.

The good news is that the CouchDB protocol will quickly figure out what happened and nothing more than a changes document should be sent. The bad news is that in many cases it's relatively expensive to set up a peer to peer link and it really sucks to do so when you aren't going to achieve anything useful (e.g. A and B will discover that their databases are already identical). But fixing this is actually quite complex.

We can think of a CouchDB database as essentially being a list of keys with an array of revision IDs. This is what is moved in the changes document. So if we could somehow fingerprint the whole then we could check that fingerprint and if they are identical then we can skip synching. This is doable but it's expensive and requires all sorts of fun data structures (I'm thinking a merckle tree) and so probably isn't worth the effort. The point of this being that we could have a single relatively short value we could use in discovery to figure out if there is any work to do.

There are other approaches like listing who one has sync'd with lately and how far (much easier to implement) but this has privacy implications and again it's not clear if it's worth the effort.

So we'll see, if this problem is causing real problems then we cna probably figure something out.

#### Can't B effectively force A to advertise for it forever?
Yes. B can just never update the `_local/thali_<peerId>` document and so A will advertise for B forever. In the long term, since advertising space (and battery life) isn't infinite we expect devices to pick a subset of folks they could notify to actually advertise. So if B never seems to pick anything up then it's likely to get put lower and lower on the list until it falls off.

Now if B does something dirty like synch data but not update `_local/thali_<peerId>` this is potentially detectable by A. It can see that B is making GET requests but the sequence number never changes. This would mark B as a bad player.

But the more interesting question is - who cares? The main use of this attack is to let B (or its confederates, if B is willing to share its private key) find A. But B could accomplish this just as easily by just advertising for A.

So it's not clear if this attack actually accomplishes anything.
