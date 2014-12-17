# Thali and the Internet of Things

# Introduction
Thali is about building a peer to peer web, so what does that have to do with the Internet of Things (IoT)? The answer turns out to be 'a lot'. And it's thanks to this convergance that we have found a great way to fund our work on Thali.

The point of intersection is - communication. It turns out that in many IoT scenarios we need the ability for 'things' to keep communicating with each other and with General Purpose Computers (GPCs) (phones, tablets, laptops, PCs, etc.) even when there is no connectivity to the capital "I" Internet.

This immediate brings up a whole bunch of problems that are very familiar to Thali.

* How do two devices (I'm using that term in the broadest sense to include GPCs as well as 'things') securely communicate and authenticate to each other in a peer to peer manner?
* When offline devices typically can only communicate on an opportunistic basis. For example, someone walks by a 'thing' with a phone in their pocket and the thing, seeing the phone in say Bluetooth LE range, wants to synch with it. How should that sync occur?
* When offline two devices who need to communicate can't because their radios can't see each other directly. But there might be other devices between them. How can the two devices relay their communications via the intermediate devices?
* How does one build logic that can be shared across all devices (and the cloud) so groups of things can keep functional regardless of connectivity?
* How does one build apps for GPCs in a consistent, cheap way that enables rich connectivity with things?

This is all meat and potatoes for Thali so building these as general purpose libraries we can fund the Thali work.

# Node.js Everywhere
As [previously mentioned](nodeondevices) a key decision made in Thali land is that we are going to switch from Java to Javascript. The driver for this decision was the desire to have Thali code run literally 'everywhere'. However the belief is that only two languages truly run absolutely everywhere - C and Javascript. We picked Javascript.

But Thali does most of its work in the background, not in a web browser. So we need a packaging framework for Thali logic that is based around listeners and waiting for events. This is, of course, what Node.js is perfect for and as such we have picked Node.js as our core platform.

It so happens that Node.js is super popular amongst the IoT crowd. No, we really didn't know that when we picked Node.js. But now that we do, let's ride the wave!

But what we are finding is that all of our IoT customers need to communicate with GPCs and because this communication is intended to work when offline (e.g. peer to peer) it needs to be event driven. Which means, yes, we need Node.js on mobile clients for our IoT customers. So we can use IoT to get funding to get Node.js running on Android, iOS and WinRT!

# Cordova Everywhere
As mentioned above all the IoT customers we are talking with need to build apps for GPCs (especially phones) to interact with their things. Yes, they want to use Node.js to handle events, but they also want to use HTML 5/Cordova for the front end. In fact when we talk to them about the [vision](http://www.goland.org/html6packagedapps/) for Cordova+Node.js they get really excited and want to adopt it.

So yet again, it looks like we can use IoT customers to pay for a feature we actually invented for Thali. That is:
* Getting Cordova on mobile to support Node.js as a plugin
* Getting Cordova to run on desktops

These would be two different open source projects, one to add in a node.js plugin and one to get Cordova running happily on desktop OS's (almost certainly on nodewebkit).

# AdHocWireless Node.js Package
For our IoT customers they need all devices (e.g. both GPCs and things) to be able to talk to each other. And they need to be able to do it over a variety of transports. They need GPCs to talk to things using Bluetooth, Bluetooth LE, Wi-Fi, Wi-Fi direct, Alljoyn, etc. And yes, [nothing new here for Thali](http://www.goland.org/thalimesh/). Interesting enough we also have scenarios that started off as IoT and ended up with needing peer to peer communication between GPCs. So we really need a library that will expose standard discovery and communication capabilities in node.js and then hook that up to whatever radios the device we are running on supports.

The idea is that we would expose interfaces in node.js that let one:
* register for events when new peers are detected
* enumerate what peers are around
* receive stand alone messages and streams from peers
* send stand alone messages and streams to peers

We would then write plugins to support the various wireless technologies we care about on the platforms we can about it. Since this is an open source project other people can drop in their own plugins to extend the work under the same interface.

# HighLatencyMesh Node.js Package
When dealing with offline connectivity we run into a lot of issues with the limitations of wireless technology. Especially in doors even 900 Mhz systems have real limitations on range. This often means that to get a message from one device to another we need to relay through intermediate devices. Our first approach to this is a store and forward system. We would sync data from one device to another and let it relay itself to its final destination. Thanks to CouchDB's powerful sync capabilities we can even send the same data on multiple paths and not worry about them interfering with each other.

This mostly requires a way to flood publish information about which devices can see which other devices and then sending replicate requests with a destination. No big deal really.

For IoT scenarios where we need to relay through untrusted intermediaries things get a big uglier but that hasn't been an issue yet.

But the core library would handle publishing location information and extend replication in PouchDB to specifically understand that a replication is being relayed to a destination.

For battery based devices we will also need a module to specifically be smart about battery. The device can't afford to constantly be communciating. So we will need logic specifically to handle how often and how much to sync.

# PublicKeyIdentity
## Node.js Package
We want to use the Thali approach of allowing two devices to authenticate to each other and communicate security using X.509 and mutual TLS auth. This is all supported in node.js already but we really could use a library to handle a lot of the scut work on generating keys, storing them, configuring TLS mutual auth connections, etc.

## Cordova Plugin
This would be a Cordova plugin that would provide some standardized support for sharing public keys. I suspect our first sharing mechanism will probably be based on something like Bluetooth LE or Bluetooth using a shared code to authenticate the shared key. We ideally want to do this without actually pairing but we shall see. This actually isn't very secure but for many scenarios its good enough. Eventually will also work in our existing support for things like generating and sharing QR Codes.

# AclsAndGroups Node.js Package
We really need a library to let endpoints specify their access control policy using ACLs which are based on groups of public keys. This library would provide a simple implementation to store the ACLs and groups and run the ACL engine using the groups. And yes, in Thali style, it will all be backed by PouchDB (e.g. CouchDB on Node.js).

# Cloud integration
Those wacky IoT folks, as much as they love offline, still really love online and the cloud. They want an easy way to share data with the cloud. The great news is that so many people are already working on this part of the problem we can just surf on their effort. So we can leverage projects like [Nitrogen.io](http://nitrogen.io/) (full disclosure - Nitrogen is built by the team I work for) to get us into the cloud.

# So what's missing for Thali?
The stack of technologies listed here are awesome and get us about 80% of the way to Thali. But there are still missing pieces. 
## Tor Support in Node.js 
Right now our IoT customers are looking for solutions for offline, but for online they assume they will go straight to their cloud. That's fine as a start. At worst we'll build our own Tor support into Node.js. There are already Node.js packages to help. 

But when I start talking threat models with customers they are getting more interested in Tor. Imagine data from things flowing into the cloud. Now imagine an advisary who can't see what's in the data (it's encrypted) but can see how much data is being sent and the data's general shape. This is often enough to detect critical events like production peaks or disruptions. This is often extremely sensivitive data that companies don't want in the hands of their competitors. So as traffic analysis threats are being better understood I'm finding customers getting more interested in Tor. I wouldn't be surprised if we can't get Tor support paid for, in the end, via IoT.

## Thali Device Hub (TDH)
The TDH is the center of the Thali vision. It's a central store that apps can put all their data into and so share it with other apps on the device and with other devices. This puts users firmly in control of their data.

The TDH needs everything listed here and some more. It needs logic about local device security so it knows which apps should have access to which data. It needs to be super smart about battery management so it doesn't synch too often when on cell. It needs logic regarding how to buidl a user's own personal mesh of devices and how to synch that. It needs logic regarding how to clean up data when storage becomes tight. You can browse around our [issue tracker](https://www.pivotaltracker.com/n/projects/1163162) to get a sense for just how much there is to do for the TDH. Before we dumped the Java codebase we were estimating that it would take 3 people about 6 months to finish everything. So it is a bunch of work but not a mountain, more like a mole hill.

But my hope is that we don't have to do it all at once. Initially we will build the TDH as a re-usable library that can be used by individual apps. Then as we build up features we can finally try to release the central library with all the bells and whistles.

# Conclusion
The good news is that when we look at our ultimate goal, the TDH, we see that it needs all the functionality on this page. So by building IoT we are building Thali. So let's get going!
