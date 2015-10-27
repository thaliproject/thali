---
title: HTML 5 Raw Sockets Investigation
layout: page-fullwidth
permalink: "/HTML5RawSocketsInvestigation/"
---

Raw Sockets are an HTML 5 feature that allows one to open a raw TCP or UDP socket in the browser. The socket can be opened incoming or outgoing and there is even support for SSL (well, sorta, see below). As a foundation for building a peer to peer service in the browser it's pretty exciting. So why isn't Thali using it?

There are a couple of issues we see with Raw Sockets:

<dl>

<dt>Listener Model</dt>

<dd>Although it's possible to open a socket and listen for incoming connections it's unclear how this model works when the browser isn't in the foreground, especially on mobile applications. So we aren't sure we can run an 'always on' service using raw sockets. At least not yet.</dd>

<dt>Security Model</dt>

<dd>
The good news is that raw sockets support SSL, the bad news is that it seems to only be the CA variant. In Thali for an outgoing connection we have to be able to specify the client cert we want to send and the server cert (or really the server key) we expect to receive. We haven't found any hooks in Raw Sockets to enable this. We have the same problem on the listener end, there doesn't seem to be anyway to say 'please present the following cert chain'.
</dd>

<dt>Server Model</dt>

<dd> We are largely based on CouchDB and in the browser we specifically depend on PouchDB. To the best of our knowledge there doesn't currently exist a HTTP server written in Javascript that runs over Raw Sockets we could slip underneath PouchDB. Now, to be clear, this is doable. All the pieces needed to write a HTTP server in Javascript over Raw Sockets are there. It just hasn't happened yet. </dd>
</dl>

Our best guess is that eventually HTML6 or whatever will just include Node.JS in the browser along with a model for how to think about long lived background processes. When that happens we will have to rethink how we have built Thali. But remember, the core of Thali is the protocol (CouchDB + Mutual SSL Auth + TOR) so what language any particular implementation is written in is of secondary importance to the mission of getting people bought into the protocol stack.
