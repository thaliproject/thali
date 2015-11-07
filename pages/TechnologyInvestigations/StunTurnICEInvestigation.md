---
title: STUN / TURN / ICE Investigation
layout: page-fullwidth
permalink: "/StunTurnICEInvestigation/"
header:
   image_fullwidth: "header.png"
categories:
    - technologyinvestigations
---

# STUN/TURN/ICE

Sigh... o.k. this is a bit complicated but mostly because Firewalls/NATs violate the end to end principle and so have made a bloody mess of the Internet for very little real benefit. But I suppose that's another conversation.

Let's say two peers want to talk. In happy land they just send IP packets to each other. In reality one or both peers might be behind a Firewall/NAT and so their packets will never arrive. So this is where the ICE universe steps in. The following are the steps that ICE would take to try to get two peers able to talk.

<dl>

<dt>Step 1 - Try UDP</dt>

<dd>Some firewalls and NATs will allow UDP traffic through.</dd>

<dt>Step 2 - Try UDP with STUN</dt>

<dd>
But wait, what if an endpoint (in the case of a NAT) doesn't even know what it's external address is? In that case it can send a request to a STUN server that will tell the endpoint what address the STUN server sees. This information can then be sent to other peer through some magical mechanism (read a centralized discovery server) who can try that address.
</dd>

<dt>Step 3 - Try HTTP</dt>

<dd>Some firewalls/NATs block UDP but not HTTP (especially on outgoing connections). So if UDP doesn't work then try HTTP.</dd>

<dt>Step 4 - Try HTTPS</dt>

<dd>Some Firewalls/NATs will create a hole for SSL/TLS so try that.</dd>

<dt>Step 5 - Use a relay server</dt>

<dd>
This is where TURN comes in. It specifies how to use a relay server to relay packets between the peers. Each of the peers opens an outgoing connection to the relay server (because all Firewall/NATs allow that) and then the relay server patches the two connections together.
</dd>

</dl>

Notice that all of this depends upon a server infrastructure being available of both STUN and TURN servers. Some companies, like Google, do make such an infrastructure available. But it allows for the collection of an enormous amount of information about the peers. It also requires some fairly fancy tricks in order to support Thali's security model. We can't depend on the HTTPS endpoints or on the TURN server for security. So in essence we would have to tunnel our own TLS/SSL stream (or moral equivalent) over the infrastructure. That's a complexity we really don't need.

So unlike say using Tor hidden services which is based on a pervasive, free and open infrastructure that is designed to make it hard to collect traffic analysis data STUN/TURN depend on a private infrastructure that is a data collection paradise.

I actually suspect that there is room for interesting conversations about how to integrate STUN/TURN into something like Thali in a constructive/secure way. After all Tor is not all rainbows and ponies. There is a massive latency penalty for using Tor (by massive I mean in the 100s of milliseconds which is unacceptable for real time audio/video) and for key scenarios it isn't workable. Although, to be fair, I think you could configure a one hop channel to a Tor Hidden service that could get around this latency penalty at the cost of removing traffic analysis protection. But, in any case, for starting out with Thali I don't think STUN/TURN/ICE is a good solution.
