---
title: Web RTC Investigation
layout: page-fullwidth
permalink: "/WebRTCInvestigation/"
header:
   image_fullwidth: "header.png"
categories:
    - technologyinvestigations
---

#### TL;DR

WebRTC's data protocol is a UDP based alternative to TCP and HTTP. It is being built into browsers but it is not currently compatible with the existing TCP/HTTP infrastructure. It's benefit in terms of browser support and performance do not appear to offset its disadvantages that it requires building and/or retrofitting all the existing HTTP software.

#### WebRTC & Thali

WebRTC is a set of APIs intended to allow primarily video and data sharing for scenarios like streaming music and video chat. Because of the latency sensitivity of these scenarios there is a strong need for peer to peer functionality so the APIs support peer to peer communication along with technologies to deal with Firewall and NATs. It also turns out that WebRTC supports a [data channel](http://tools.ietf.org/html/draft-ietf-rtcweb-data-channel). The purpose of this wiki page is to ask the question - could we use WebRTC's data channel as the primary communication channel for Thali?

To answer this question we first have to understand the WebRTC data channel protocol stack.

#### Stream Control Transmission Protocol (SCTP)

This is standardized in [RFC 4960](http://tools.ietf.org/html/rfc4960). SCTP was originally designed for telephony signaling but it's actually much, much more. It's essentially a full blown replacement for the kinds of capabilities we normally associate with both TCP and to a small extent HTTP. It provides a model for a connection that can be made up of a number of streams that each have their own unique characteristics like being ordered or reliable. It also supports multi-homing (the ability to specify multiple addresses for data delivery which can be thought of as being the same, this is very nice for scenarios where connectivity is intermittent or where endpoints are frequently changing networks).

SCTP is an abstract model so it can be 'mounted' or 'run' over different transports. For example, it could be bound to IP directly but there is also a definition for a binding to UDP.

#### Datagram Transport Layer Security (DTLS)

This is standardized in [RFC 6347](http://tools.ietf.org/html/rfc6347). For our purposes one can think of it providing the exact same security guarantees as TLS 1.2 but in a manner that is compatible with protocols like SCTP that support out of order delivery, retry, etc. This is necessary because TLS assumes it runs over a transport like TCP that provides these guarantees and SCTP doesn't work quite that way.

#### WebRTC Data Channel Protocol

Now we can get back to where we started, the WebRTC data channel protocol. This isn't really a protocol, it's a profile. This is something one sees when grand infrastructures of specs are built with infinite options and combinations. It doesn't so much try to build something new as provide instructions on how to use a ton of existing stuff (in this case a combination of RFCs and drafts). It defines the underlying firewall technology (ICE), transport (UDP), stream substrate (SCTP), security (DTLS) along with a variety of options for MTU discovery, connection negotiation, etc. The spec isn't done yet and really understanding it requires reading a lot of other specs. Even for all that it leaves a lot of fundamental questions unanswered. It mentions proxying HTTP content but doesn't say how. It mentions pairing channels to create bi-directional connections (since SCTP channels are unidirectional) but doesn't say how. Maybe those questions will be answered in yet more specs.

#### Firewalls/NATs

SCTP and by extension WebRTC faces the same problems with Firewall and NATs that everyone else does and so ends up using ICE. This is reasonable but as I explain in [Stun/Turn/ICE Investigation](StunTurnICEInvestigation) this is not a good option for Thali.

#### Quick UDP Internet Connections (QUIC)

This is a related effort by Google to create a TCP like protocol over UDP with TLS capabilities. I put it here just to remember it exists but it doesn't really change any of the conclusions of this paper other than it could be an interesting transport for SCTP.

#### What I think this means for Thali

A fundamental rethink of TCP is long overdue and the ideas in SCTP frankly harken back to a variety of MUX protocols that have been made over the years (net.tcp anyone?). This is good. We are well overdue for a fundamental rethink of TCP and HTTP in an application protocol context. But I am very concerned that the SCTP stack is just too complex. It reminds me too much of the kind of 'nobody can say no' standards that end up being an overly complex mess. Maybe I'm wrong. Maybe the level of complexity and the flying wing of specs involved in the WebRTC data channel truly represent the minimum possible complexity for a system like this. Certainly if you look at the mountain of RFCs defining TCP (which in a very real sense the WebRTC data channel and related specs are trying to replace) you would think so.

But in the meantime the value Thali would get right now from the data channel does not, in my opinion, offset the complexity. We would have to abandon Tor for firewall transversal and traffic analysis protection since Tor only supports TCP. We would have to refit just about every existing code base out there like node or couch or what have you to run over this new infrastructure. That is all doable. One can easily imagine (and in fact the data channel spec explicitly refers to this possibility) putting in place proxies that would present a HTTP interface on one end but actually transit over Web RTC. We can work with the Tor community to maybe have them put in support for the UDP channels being used in WebRTC data channel. None of these problems is insurmountable. But I just don't see enough benefit to Thali for the change to be worthwhile.

#### Acknowledgements

Thanks to Randell Jesup, who is in no way responsible for the content of this wiki page, other than pointing out that I had the wrong link to his draft and telling me about QUIC.
