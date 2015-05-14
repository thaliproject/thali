---
title: Thali's Stories
layout: default
---

# Stories
The following is a list of stories we need to complete in order to meet Thali's first major deliverable. Each story has a number that shows its dependencies. So, for example, we have a story 0 that just about everything depends on. We could then have stories 0.0 and 0.1. These would be two stories that can be done in parallel that both depend on story 0. So story 0.1.0 would be a story that depends on stories 0 and 0.1. 

There is nothing particularly magical about these stories. They certainly won't survive contact with the real world. But they show a possible plan of attack. In the real world they will almost certainly get remixed, changed, whatever. That's fine. But by having this list we at least have some idea of where we are going and how we think we are going to get there.

In addition there are dependencies between stories that are shown as not being dependent. For example, ACLs, notifications and quotas are all shown as being parallel stories but in fact all depend on having an eventual consistency hosting framework (see below for details). Rather than creating tons of tiny stories I've laid things out in a general "big picture" flow with the understanding that all dependencies aren't fully shown.

# Story dependency chart

The following displays all the stories listed below in dependency order:

```
Stories
    0 - Turn on the lights
        0.0 - Bring in the public keys
            0.0.0 - ACLs
                0.0.0.1 - ACL Role Membership Changes
            0.0.1 - Identity Exchange
            0.0.2 - Notifications
                0.0.2.0 - Secure Notifications
                0.0.2.1 - Improving Notification Performance
                0.0.2.2 - Toasts
            0.0.3 - Attachments and Quotas
                0.0.3.0 - Fix Attachment Perf
        0.1 - Android BLE Central
            0.1.0 - Android BLE Peripheral
                0.1.0.0 - Android/iOS interop
        0.2 - BLE Hub
        0.3 - Using Wi-Fi Infrastructure
        0.4 - Using MyFi on Android or Wi-Fi Direct Pairing?
        0.5 - Testing Framework
    1 - Securing WebView to local Node.js Communication
    2 - Supporting Internet based communication
    3 - Native performance measurements
        3.0 - Thali performance
            3.0.0 - Load Testing
    4 - Fuzzing
```

In theory any items listed in parallel can be done in parallel. So this is NOT a linear structure. We will pick which stories to tackle based on resources, priorities and mood.

# 0 - Turn on the lights
In this milestone we just want to get the pieces sorta working together. We are explicitly not worried about security or performance.

The deliverable for Milestone 0 is a Cordova based application running on Android and iOS that can synch content in a single shared database using local P2P. Our app will be a postcard application. Users can create a postcard and share it with those around them. Our focus is on asynchronous sharing. The idea is that when someone creates a postcard the people they want to share it with might not be immediately available so we will want to synch when we can.

The specific functionality is:

__JXCore Cordova Plugin__ - We have the JXCore Cordova plugin working on both Android and iOS. There is no security however between the Webview and the node.js plugin.

__JXCore LevelDown Plugin__ - We have the ability to run PouchDB on top of LevelDown in Node.js on both Android and iOS using JXCore.

__JXCore Api__ - We need the following APIs from JXCore:

* An API to tell us if we are on Android or iOS.
* An API to tell us where to store persistent data related to the application

__Native Discovery__ - On Android and iOS we need native code that will:

* advertise a service ID, a human readable string and a connection string that the receiver can use to connect to the advertising device. To keep things simple for milestone 0 we don't need to support large strings. Advertising should be on Wi-Fi direct on Android and BLE on iOS.
* stop advertising
* provide a callback that can provide notifications whenever a user is discovered. The callback should provide the user name and the connection data.
* provide a callback API that can specify when a user has gone out of range.
* A binding of the previous APIs to node.js via JXCore
* For this release on iOS we can disable discovery when the app is in the background.

__High bandwidth JXCore Binding__ - On Android and iOS we need the ability to switch from discovery to a higher bandwidth transport. We have to:

* Support talking to the native platform's high bandwidth transport from node.js's HTTP layer, this would be unpaired bluetooth on Android and multi-peer connectivity framework on iOS.
* We need a way for someone wanting to advertise a service to open a HTTP port in node.js to listen in on, have that listener properly connected from node.js to the underlying native transport and to get an identifier for the listening port that can be advertised via discovery.
* We need a way for a client that discovers someone to pass in the connection data they got from the discovery announcement and use it in Node.js's HTTP client to connect to the remote party.

__Node.js Native Discovery__ - We need bindings in Node.js that expose the discovery and high bandwidth capabilities of the platform. So we need APIs that:

* start advertising including a service ID, a user name and a connection point that the node.js programmer can subscribe to in order to receive discovery and disconnect events.
* Discovery events should call back with a user name and a connection point.
* Disconnect events should call back with a user name.
* stop advertising

__Replication Manager__ - The replication manager will hook into the discovery events. Any time a new person is discovered the manager will automatically connect to them and do a pull synch into PouchDB. The replication manager will also connect to the changes feed on the local pouchDB instance and anytime there is a change caused by the local user then the replication manager will do a push synch to anyone it knows about via discovery.

__Postcard App__ - The postcard app consists of a single PouchDB database that records all postcard. In this milestone each postcard is a single JSON document (no attachments, yet) whose key is a GUID and that contains a "from" field with the human readable name of the sender and a "text" field with free form text. The functionality is:

* When the app boots it has to look into the location for persistent data specified by JXCore and see if there is a postcard DB. If there isn't then it must be created.
* When the app boots up it must search its _local DB for a record with the id "Me" and the value contains a field "name" with the user's name. If the value is not present then the app must prompt the user to enter a name.
* Once the app starts it will create a listening endpoint and then call the start discovery function with an agreed upon service ID, its users name, its connection endpoint info and a callback to handle discovery events.
* Whenever someone is discovered the app must immediately connect to them and do a push and pull synch.
* The app must also hook into the changes stream of the local postcard DB and anytime there is a change created by the local user the app must do a push synch with all the folks it has discovered who it hasn't been notified have gone away.

# 0.0 - Bring in the public keys!

In this release we will switch to using public keys for identity.

__Crypto__ - We need the following APIs:

* Generate a public/private key pair and return a PKCS#12 encoded file containing them
* Take a PKCS#12 encoded file and return a public key object
* Take a public key object and return a X.509 SubjectKeyInfo encoded binary string
* Take a binary string and return a SHA 256 hash that just returns the top 16 bytes
* Take a binary string and return a base 64 encoded version
* Take a PKCS#12 encoded file and return a self signed X.509 cert
* Show how to submit the PKCS#12 file to create a TLS listening endpoint
* Show how to submit the PKCS#12 file as a client to create a mutual TLS auth connection
* Show how to validate that a server presented the expected self-signed cert
* Surface in the environment of a connection the validated client public key (if any)

__Public Key Hash__ - This refers to taking the X.509 SubjectKeyInfo serialization of a public key, calculating its SHA 256 hash and then taking the top 16 bytes of that hash. This value is used to uniquely identify devices.

__PostCard App__ - Most of these changes for this release are actually in the postcard app more than in the underlying code. But debugging everything will be the real challenge. The changes are:

* Remove the code that looks for a user's name in _local.
* When the app boots up it must search for a local file that records a PKCS#12 file and if the file isn't present than the app has to create a public/private key pair and record it in the PKCS#12 format in the defined location.
* When the app boots up it must check its local Postcard App DB to see if it has an entry with the key "addressbook-[the device's public key hash]" and a record that contains "name" with a value that is the user's name. If such a record does not exist then the app must prompt the user for their name and create the record.
* When the app starts advertising itself it must advertise its public key hash, not its name.
* When connecting to a discovered endpoint via PouchDB the connection must validate that the proper public key was presented by the server and the client must present its public key.
* When receiving a connection via PouchDB the connection must be validated to ensure that a proper public key and signature was used.
* Whenever the user creates a new postcard they have to specify who the postcard is to. The possible values will be retrieved by a search on "addressbook-*" excluding the users own identity. Any time the app synchs with another app it will get that apps addressbook entry. So now when a postcard entry is saved its format includes "from" and "text" as in the previous milestone but also adds "to" with an array of public key hashes.

# 0.0.0 - ACLs

In this release we will introduce ACLs.

__Crypto__ - We need a function that will return a binary stream with the requested number of cryptographically secure random bytes.

__ACL Engine__ - We need an ACL engine that sits in front of the PouchDB endpoint and checks every request to every possible endpoint. It will run off its own ACL database that can be programmed using a local REST API that is only hosted on localhost. This database will offer:

* An isAllowed function supplied by the user to specify what requests may go through
* Will plug into both high level endpoints (like GET on db/doc) but will also plug into compound commands like bulk_get, changes, all_docs, etc. and call isAllowed individually on their component parts.

__Eventually Consistent ACL Update Mechanism__ - This is a framework we will provide that the developer can drop code into. The framework runs automatically at application start and should run after every mutation to the database it is watching. It will use the changes feed from the last sequence ID it read to see changes and send the associated data to the submitted user function. It is that user function that will then update the ACLs. Once the user function returns then the framework will persistently record the sequence ID it has worked up to. If there is a crash then the framework can and will apply the user function to values that it has applied to before. So all changes made by the user function have to be idempotent.

__Postcard App__ - There are a number of changes to the app specifically:

* When a postcard is created its id must consist of "postcard-[cryptnum]-[public key hash]" where cryptnum is a cryptographically secure random number generated by whomever created the postcard entry. Public Key Hash identifies who the postcard is from. The postcard format will no longer contain a 'from' field since it is now redundant. It will just contain 'to' and 'text' fields.
* When a postcard is received the isAllowed code MUST check that either the public key hash on the ID belongs to the caller or that the caller is listed in the 'to' field of the postcard (the later allows people to forward postcards from others). The check has to also make sure that the receiving device is listed either in 'from' or in 'to'.
* On all reads the isAllowed code MUST check that the reader is either in the 'from' field (yes, people can receive their own cards, it happens if the card was received from someone else) or the 'to' field, otherwise the request must be rejected.
* The postcard app must register a handler in the eventually consistent ACL update mechanism to review all changes and make sure that there are ACLs for each postcard individually listing everyone in the 'to' and 'from' fields.

# 0.0.0.1 - ACL Role Membership Changes

In PouchDB a DB records a sequence ID for each record. That secure ID is guaranteed to always be increasing (although it might not necessarily be an integer). So imagine that sequence ID Alpha is a postcard from user A to a group called "Friends" which currently only contains User B. User C then shows up and syncs with A up to A's sequence ID Gamma. After that user A decides to add user C to the "Friends" group. 

Because C is now a member of "Friends" it is now possible for C to get the postcard in sequence ID Alpha. But C won't ask for sequence ID Alpha since its synch records show that it has synch'd through sequence ID Gamma. How would C know to go back to the past?

There are a couple of ways to solve this problem. The easiest, but least performant, has to do with how synch progress is recorded in CouchDB. When C synch'd with A, C will create a record in _local (which is never synch'd) recording which sequence ID from A's database it has synch'd up to. That record would say "Sequence ID Gamma" and is why C will never see the ACL change to sequence ID Alpha. So in theory we could do something really hamfisted. Any time a role membership is changed we can delete *all* the _local synch records for everybody. This will force them all to do a full re-synch. This isn't quite as bad as it sounds because in practice the first step in a synch is checking a list of all available records. Any records that have already been synch'd will not be re-synched.

There are tons of obvious optimizations here. We could just delete synch records for people who are directly affected by the role change. Because of nested groups this can sometimes be a little tricky to figure out but not too hard.

We could try to detect if the role membership change actually affects any records that have already been synch'd.

Etc.

A related issue is - what happens when someone is removed from a role? Do we want to force the person to delete any records they no longer have access to? That is actually tricky. It would mostly likely require us to both delete their synch record in _local (so they will do a full re-synch) and it would require "lying" about the records that they were removed from and presenting them as deleted when they are not. This can get one into real trouble if the person is ever added back to the role (i.e. if the original removal was due to an error) in which case we would have supposedly immutable changes, changing! In other words a record we said was deleted is suddenly not deleted, what sequence IDs will be used here? Mostly likely we would have to delete the record for real and then immediately recreate it with a different role. Does your head hurt? Because mine does. My feeling is that you can't erase the past in quite this way. So if someone is removed from a role the system will refuse to resynch with them the data they don't have access to anymore, but that is it.

__Postcard App__ -
* We need to add UX for creating groups of users and connecting that to roles
* We need to add UX for specifying a group rather than individual users on a postcard
* We need to add UX for changing the membership of groups

# 0.0.1 - Identity Exchange

Now we introduce the idea that identities have to be explicitly exchanged.

__Crypto__ - We need:

* AES-CMAC (although I really wonder if we couldn't just use a HMAC-SHA-256 instead?)
* An implementation of our key exchange protocol as given in appendix A of http://www.goland.org/coinflippingforthali/ using HTTP, this will require defining a simple REST protocol with PUTs and defined endpoints. It will also require turning off discovery and re-enabling it with a different service ID specific to identity exchange and advertising the user's name (in clear text). The code needs to have a callback to return all the service ID endpoints it receives and their names. The app code can then decide how to display that information to the user. The code then needs to be able to receive an instruction from the app as to which identity that was discovered it should try to do an exchange with. This will then trigger the full exchange algorithm. The code will then return the confirmation ID at the end. An alternative approach is to always advertise the Thali ID but have a characteristic (for BLE) or additional text record entry (for Wi-Fi Direct) that advertises a name when identity exchange is active.

__Postcard App__ - Time for some more UX:

* We must no longer have the address book entries in the postcard database. Instead we will create a separate database that is not accessible outside of localhost to record addresses. Each entry will have its public key hash as its key and a JSON document with a name field and value.
* We have to have a dedicated address book tab that lists all the people the device knows about as read from the address book database.
* We have to have an identity exchange UX where a user can add a new user to the address book. This must trigger the identity exchange process. During this process the UX code will receive a list possible identities to do an identity exchange with. It has to then call back the code to tell it what identity to do the exchange with. Then finally the app will get back a 6 digit confirmation code that the user has to check with the user device. If not confirmed then discovery has to start all over again. If confirmed then the user has to be prompted to add the discovered user to the address book and to confirm and be able to change the name to be associated with the discovered key.
* The code to suggest 'to' values now has to be re-connected to the new address book database.

# 0.0.2 - Notifications

We will switch from announcing a device's identity during discovery and instead announce the identity of the devices we are looking for. This is a first step into implementing the full crypto discovery protocol. To make this work we have to know who we need to discover which involves adding a new infrastructure that supports determining who needs to know about changes.

__Change Notification Infrastructure__ - We need to use our eventually consistent framework to check each mutation and see who needs to know about it. The user provides a function that is presented with the mutation and returns a list of who should be notified about it as public key hashes. This is then recorded along with sequence ID. We will then check to _local synch records to see if the public key hash has synch'd passed that sequence ID. If they have not then we will pass their identity to the discovery engine who will advertise their public key hash. We will also have another process that regularly runs (and should be connected to changes but we can't rely on changes) and checks all the _local records for synchs to see if the identified public key hash has finally synch'd passed the ID we have recorded for them. Once they have we can remove that public key hash from our notification list. We do need a feature where the discovery engine can determine that discovery is being used for something else (like identity exchange) and waits until discovery is switched back to notification.

__Postcard App__ - 

* The app has to provide a function to the change notification infrastructure that will publish the values in 'to' other than the current devices public key hash.
* The postcard app also has to hook in discovery to the notification engine so it can drive what is being discovered.

# 0.0.2.0 - Secure Notifications

Now we will implement the full secure notifications protocol. This includes encrypting token contents as well as putting in place a protocol to securely negotiate a high bandwidth connection. The spec is available at http://thaliproject.org/presenceprotocolforopportunisticsynching. We still have to finish up the transport bindings. This could potentially be split into two stories, one to deliver the crypto and the other to do the transport bindings.

# 0.0.2.1 - Improving Notification Performance

Imagine that user A created a postcard intended for users B and C. It turns out that user A and B's devices were within range of each other and user A gave the postcard to user B. Later on user A's device see's user C's device and synchs the postcard. At this point user C's device would start advertising for user B's device because it has content (the postcard) to share. But of course user B already has that particular postcard! So what we want is to implement a protocol that lets A tell C what it has given to other folks.

# 0.0.2.2 - Toasts

iOS by default uses BLE for discovery. But this brings up a problem. Imagine that user A has their Thali app in the foreground and user B has their Thali app in the background. In this case both A and B are using iOS devices. In that case B can "see" A and can connect over BLE. But B can't do much more since iOS doesn't support establishing new multi-peer connectivity framework connections with background apps. So if A decides it has data for B or B decides it has data for A then B's phone needs to raise a toast to get user B's attention, get them to take the phone out of their pocket and hit the toast which will bring the Thali app to the foreground and let the multi-peer connectivity framework take over.

Because Android runs in the background we don't need the same notification->toast relationship but hey, it's as good a time as any to support toasts for Android.

__Node.js functionality__ -

* We need a node.js API that we can call with a short string that will show up as a toast that if clicked on will bring up the app. This should run on both Android and iOS.
* We need a function to tell if the app is running on iOS or Android
* We need a function to tell if the app is in the foreground or background

__Postcard App__ - 

* Using long polling for changes to detect when there is a synch with new data and then use the toast API to raise a toast (even when the app is in the foreground) notifying the user that there is new data.
* On iOS if the app is in the background and it receives a discovery notification targeted to it then it needs to raise a toast to get the user to activate the app.

# 0.0.3 - Attachments and Quotas

We need to provide a quota management system. In this case we will specify how much space postcards from any individual, including both JSON and attachments, can take up.

__Quote Notification Infrastructure__ - 

* The front end is a meetsQuota function. It takes as input the request along with size information on the request and size information on the entire database and outputs a yes/no. This is a user supplied function that checks a database.
* The back end is our eventual consistency framework that will call a user function on each change and have it add to the affected users quota. How this is recorded is up to the app. The reason for having two functions is otherwise we can end up docking someone for a write that never hit disk due to some failure. It is up to the user function to decide what to do if the total size of data applied to the user is too much. And yes this potentially could let someone temporarily exceed their quota because we check on the front end two or more times before the eventual consistency framework has a chance to update the person's quota.
* The tricky part is that we need a way to let the user function delete the content of a record without having to remove knowledge of the record and without actually deleting the record. In other words I may want to delete a user's postcard but if I ever synch with the user again I don't want to say that the postcard is deleted because this isn't true. If I marked it as "deleted" then I'm saying that everyone, everywhere should delete this postcard for all time. But what I really mean is "I garbage collected it". So we need something like compaction so that we can do things like leave the revision history but delete all the associated content.
* Although we initially use both push and pull synch just to get things going in general we will be switching over to exclusively using pull synch. So quotas should only apply when a device is pulling data into itself, we shouldn't generally support people pushing data. So this means that we can apply quotas to each record we pull in based on who created it and fail them individually. We should treat quota failures as essentially record read failures on the remote source and continue the synch having marked the failures.

__Postcard App__ -

* We need to add the ability to attach files/photos to postcards. We can restrict to a single file/photo or multiple if we are feeling brave. The real work is in the UX. This 'just works' in PouchDB.
* We need to create a quota DB where the key is a users public key hash and the JSON record contains fields "current", "cleanup" and "maximum". The idea is that when we hit cleanup we will start deleting old content and if we hit maximum we refuse requests. We should have some default quota for users and for the postcard DB as a whole. Note that we will handle the debit based on the 'from' field which might not match who is actually sending the request.
* We need to provide a meetsQuota function that checks for the record for the user and if there isn't one then creates one with the default values. It will check to see if the current update will blow maximum and if not, it will allow it. Similarly it needs to check if a request will blow the DB maximum quota and if so, reject the request.
* We will need a back end notification function that will handle actually debiting users for the content on disk. This function will also handle removing old content from the user (starting with the oldest postcards from that user) when their quota is exceeded.

# 0.0.3.0 - Fix Attachment Perf

We know that attachment perf in PouchDB is pretty awful. PouchDB treats attachments as small strings. So, for example, if one is updating a single JSON field in a document then when synching that change all attachments will be resent as base 64 strings! This isn't going to work for our scenarios which often involve moving large attachments.

So we have to fix this. There are a couple of ways we could do it:

* We could fix the replicator so it always requests attachments separately from the JSON. This would let us download attachments as binary. But it would make the replicator more complex and run a higher probability of ending up with a doc without its attachments.
* We could fix the CouchDB protocol implementation in PouchDB so it supports atts_since (which will prevent downloading attachments that haven't changed) and MIME/multipart (so we can download attachments inline as binary)
* We could switch to one of the alternative PouchDB synch protocols such as the stream protocol. Right now stream only supports JSON documents but it shouldn't be too hard to turn it into something like BSON. Alternative the new bulk_get method might be enough.

# 0.1 - Android BLE Central

If we want Android and iOS phones to be able to interop at all we need to support Android at least being a BLE central. This means that an Android phone can find an iOS phone that has the Thali app in the foreground. 

__Node.js functionality__ - Expose the BLE central API on top of the Native Android functionality

# 0.1.0 - Android BLE Peripheral

Because it provides us with the widest compatibility across the Android platform we are starting Android discovery using Wi-Fi Direct. But in the long run we would rather use BLE. It both has better battery usage and gives us some cross-platform capabilities with iOS.

__Node.js functionality__ - Expose the BLE periphal API on top of the Native Android functionality

# 0.1.0.0 - Android/iOS interop

First we need to actually test Android and iOS BLE interop and make sure that each can be a central to the others peripheral.

Then we need to figure out how to establish a high bandwidth link between them. If local Wi-Fi Infrastructure or the Internet isn't available then the only other choice is turning the Android device into a myfi endpoint and sending the SSID to the iOS device via BLE and asking the iOS user to manually switch their Wi-Fi endpoint to the given name. This is as bad and as fiddly as it sounds. It also opens up abuse where bad folks could intentionally advertise SSIDs they have seen used in order to sink hole all communications from the iOS device. I'm not 100% sure this attack will work but I strongly suspect it will. We have to check. This is a quintessential man in the middle attack. It won't let any data leak from Thali since we authenticate and encrypt all communications but it would allow the attacker to see things like where the user is navigating their web browser (even if the connection is encrypted thanks to features like SNI). Now how careful are people in general when they connect to Wi-Fi? At this point nobody should be treating Wi-Fi APs as secure. But nevertheless the attack can occur. And because iOS has no APIs to help with Wi-Fi there is really no way for us to help the user out. If only iOS supported Wi-Fi Direct and had a Wi-Fi API! Heck, I'd settle for just having a Wi-Fi API. At least then we could manually track the SSIDs we have discovered and forcibly disconnect them when we aren't using them.

We need to put the app in the middle of discovery. For example, if an Android app finds out that an iOS app is looking for it we have to give the Postcard app the chance to decide if it wants to respond to the discovery request. Similarly on the iOS side if a request comes in to connect to myfi endpoint the Postcard app has to have the chance to decide to ignore it so the user won't see anything.

__Postcard app__ - 
* We need to implement a 'rejected' group which contains people the user of the Postcard app doesn't want to communicate with
* We have to update our ACLs and our discovery infrastructure so that we will not accept discovery requests from or make discovery requests to people in the rejected group. We also have to make sure the front end will reject any high bandwidth requests (especially over wi-fi infrastructure) from anyone in the rejected group.

# 0.2 - BLE Hub

Above we discussed the situation where A and B and A and C wanted to talk. But what happens if B and C need to talk? How can they discover this if B and C are both iOS devices in the background? The answer is that we need A to act as a hub. It needs to essentially multi-cast discovery messages it receives.

Now lets say that B and C have connected to A and A sends B's discovery announcement to C. C sees that it is listed in the discovery announcement. So now C wants to notify B that it is available. If B confirms it is also available then both B and C will want to raise a toast on their respective iOS devices to see if they can get their users to activate the Thali apps in order to enable data exchange.

__Node.js functionality__ 

* We will need a full spec to define the protocol but the basic idea is that if a peripheral receives a discovery announcement from an iOS client in the background then the discovery announcement will be cached for some period of time and automatically replayed to any other iOS clients in the background who connect to the peripheral.
* We will need to define a relay protocol so that one iOS client in the background can send a message targeted at another iOS client in the background. Alternatively we can just use the same flooding strategy for any message being sent. This would mean however that the message would be received by folks whom it is not intended for. Since messages will all be encrypted and authenticated this won't cause bad behavior but it will waste battery.
* The hub logic should be smart enough that it knows to turn itself on anytime it is in the foreground on iOS and be able to run in both foreground and background on Android.
* We need to honor the usual principle that it is up to the app to decide who can discovery it so the usual callbacks to prevent discovering or responding to discovery requests from people we don't want to deal with must still apply here.

# 0.3 - Using Wi-Fi Infrastructure

Let's say that a phone is somewhere that actually has Wi-Fi Infrastructure support and is connected to an access point (AP). In that case we would like to be able to re-use that communication mechanism since it potentially has farther reach, especially if multiple APs are networked together.

We need two things to make this work. First, we need a discovery mechanism. We can use either SSDP or mDNS. We'll pick which ever one we end up using with Wi-Fi Direct. Second, we need to make a direct connection.

Ideally we would hook in identity exchange as well but it's not strictly necessary so I haven't made that a dependency.

The big thing we have to be on the look out for is - how do we tell when it isn't working?

The reality is that many networks, especially corporate networks, intentionally do not support multi-cast. Or if they do, they only support a small subset just intended for things like finding printers. So just because we are advertising on the local AP doesn't mean anyone can hear us.

To make matters even more complex, even if discovery does work that doesn't necessarily mean that we can establish a unicast connection. Many networks intentionally do not allow unicast connections between local addresses for security reasons. So our code has to be ready to handle a situation where we might be able to discover someone via multi-cast but then we can't actually talk to them over unicast!

At that point we fall back to local P2P.

# 0.4 - Using MyFi on Android or Wi-Fi Direct pairing?

When establishing a high bandwidth connection we would ideally rather use Wi-Fi rather than Bluetooth. It's faster and has longer range. The "obvious" way to handle Wi-Fi is via Wi-Fi Direct. It's explicitly designed to run in parallel to normal Wi-Fi so we can use it without disrupting the phone's normal Wi-Fi communication. The problem is the wi-fi permission issue that Jukka has written about.

There are two ways around the permission issue and this story is where we figure out which, if any, we want to pick.

One possibility is for one phone to set up a Wi-Fi Direct endpoint and for the other phone(s) to connect to it as if it were a Wi-Fi Infrastructure AP. This works because all Wi-Fi Direct groups are automatically Wi-Fi APs. We just need to communicate the group ID and password and other Android phones can connect directly. This won't even interrupt Internet connectivity on the phone hosting the group since the normal Wi-Fi AP on the phone will continue to function! But it will interrupt Internet connectivity on the phones that join the AP since they will treat the target phone as being their AP. So we can't leave these kinds of connections open for long. It also brings up security issues since the Wi-Fi AP point can see any Internet traffic the other phones set out.

The other possibility is the Wi-Fi pairing tricks that Jukka has written about. This requires UX but minimizes it. This would also require that we continue to support unpaired bluetooth since we would need it for scenarios where two users only talk in the background and never had a chance to pair. Although to be fair we could add pairing to the identity exchange process. Although eventually we won't require 1:1 identity exchange, eventually we will support transitive trust.

There is a third possibility which is rather than using a Wi-Fi Direct endpoint the hosting phone can use a myfi endpoint. This would disrupt connectivity on the hosting phone but it might be worth doing because we will need this functionality anyway for Android/iOS high bandwidth interop.

# 0.5 - Testing framework

How do we test local P2P? We need to set up a bunch of phones and figure out a lot of details such as how to deploy images. How to run tests. How to collect results. Etc.

# 1 - Securing WebView to local Node.js communication

See http://thaliproject.org/SecuringCordovaAndNodeJs for ideas on how we can make this work. Remember that the node.js plugin is exposing its endpoints as HTTP endpoints on localhost. So any app could talk to them. We need to secure things.

# 2- Supporting Internet based communication

So let's say we have an Internet connection. Suppose cellular is working? Support we are connecting to a local Wi-Fi AP that doesn't allow local connectivity but does allow outgoing connections to the Internet, now what?

The core Thali model calls for using Tor to enable handsets to directly connect to each other. This model can work with Android which supports fun things like running in the background. But its a much harder story with iOS which really doesn't support running arbitrary code for long periods in the background. So if we are going to support iOS we have to have help from machines on the Internet. We need several features to make iOS work. First, we need a way to leave notes for other users that we have data for them. Second, we need a way to serialize data we have for other users (which means we can't use the native CouchDB REST protocol since it's request/response based) and leave it somewhere that user can grab it. There are a number of ways we could do this but they all have complications. The biggest one is that at least in the background the only thing iOS can do is download HTTP or HTTPS links. So even though an iOS app in the foreground could run something like Tor or whatever, a background app cannot. So we have to have a way to leave messages for people that can be retrieved with simple HTTP or HTTPS links. There are services that support anonymous file upload and support download via HTTP/S that could be used to put up large files but they generate random URLs under which they are stored. So if user A wants to share with user B they could store the data on one of these sites but how does user A get the URL to user B? One possibility is just plain ole email. But the user experience will be horrific since unless the user gives the Thali app their email configuration data and name and password there will have to be a user step every time we want to share content. And the same is true on the other side. When receiving a URL the user will have to click on a link to activate the app to pull down the content. So much for background download!

What we need is a free online service that doesn't require sign up that will allow us to upload files with predictable names. That would be enough to allow us to perform discovery, find files from contacts, etc. The ideal solution would be some combination of a file upload site and something like XMPP where we could send messages to inboxes which contains encrypted data with download links and where the XMPP inbox could be downloaded via HTTP. But today we would require some kind of credentials to set this up since there really aren't XMPP services that support creating accounts in a completely automated way. 

If we are willing to have users register for an XMPP account then we probably can figure out a service provider that supports the XMPP HTTP interface so we could use that to download messages.

Note that all of this assumes that we will have some way to serialize synchronization information so it can be relayed through the cloud. There are some projects for PouchDB that provide some of the pieces for this but they aren't complete. So there is work to do there as well.

# 3 - Native performance measurements

We need to measure the native performance for the following scenarios. This will give us benchmarks to figure out how much perf Thali is burning.

For discovery we need to test:

| Android | iOS |
|---------|-----|
| Wi-Fi AP using multi-cast | Wi-Fi AP using multi-cast |
| BLE | BLE |
| Wi-Fi Direct | |
| Unpaired Bluetooth | |

For each of these we need to measure:

* How long does discovery take from the perspective of the searchable device and the searching device?
* What is the average battery consumption/hour for trying to discover without advertising?
* What is the average battery consumption/hour for advertising?

For high bandwidth we need to test:

| Android | iOS |
|---------|-----|
|Wi-Fi AP | Wi-Fi AP|
|         | Multi-Peer Connectivity Framework|
|Unpaired Bluetooth | |
| Wi-Fi Direct | |
| MyFi | |

* How long does it take to successfully transmit the first byte?
* What is the bandwidth that can be successfully sustained, including error recovery?
* What is the battery consumption/megabyte of data transferred?

We then need to re-run these measurements but with multiple devices all at once. We need to understand how perf looks when we have a room full of phones.

Finally we need to measure:

* What is the data rate in MB/s we can stream data from memory to disk?
* What is the data rate in MB/s we can stream data from disk to memory?


# 3.0 - Thali performance

We need the equivalent of the above measurements but this time taken from inside of Thali. That is, discovery starting at the Thali layer. We need to agree on some standard model for data that needs to be synchronized and then run that synchronization to measure average data rates end to end.

# 3.0.0 - Load Testing

We need to create a framework to test how we behave under load. This includes making sure things like the quota mechanisms for discovery and data transfer will trip appropriately. In theory this should all be tested as part of developing our DOS defenses but realistically this kind of work always ends up needing a dedicated framework.

# 4 - Fuzzing

We need to fuzz all of our network front ends which means we need to set up a fuzzing framework and configure it to produce useful content. This won't be easy. Really. Not easy.
