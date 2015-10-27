---
title: Building Thali on Top of Firefox
layout: page-fullwidth
permalink: "/BuildingThaliOnTopOfFirefox/"
---

# TLDR

Thali could be entirely built using Firefox technologies and in so doing have a single code base that would literally run anywhere. But the complexity of doing so appears so high that for now it seems better to take a different approach.

# Investigation Results

Firefox supports building add-ons in pure Javascript which are then portable, as is, across all Firefox platforms. They can do powerful things through the XPCOM api that exposes lots of fun functionality like raw sockets. But unfortunately many of the APIs we need are not exposed via XPCOM so we would have to build our own APIs using [js-ctypes](https://developer.mozilla.org/en-US/docs/Mozilla/js-ctypes) which isn’t easy but based on the [Network Security Services (NSS) library](https://developer.mozilla.org/en-US/docs/NSS) and given the example of [https://wiki.mozilla.org/Firefox_Sync Firefox Sync](https://wiki.mozilla.org/Firefox_Sync Firefox Sync) (which is written as a Javascript extension that does its crypto by calling down to NSS via js-ctype) it’s doable. Then, on top of that, we would need to be able to run the server portion as a long living background process with no UX. There is a way to do this in Firefox using a 'headless' version of Firefox called xpcshell. But I was warned on the Firefox IRC channel that not many people really understand xpcshell and most of them don't have time to hang out on IRC so I would be on my own. Note, btw, that once all of this is done I would still need to find a HTTP server and a HTTP client, written in Javascript, to sit on top of the js-ctype APIs. There are, limited, attempts at least at HTTP server, I didn't find a HTTP client but I'm sure it's out there. Writing both isn't brain surgery but it just becomes another set of non-trivial code that I have to write, test and maintain.

The end result is that what Thali needs is doable in Firefox. But the cost and complexity seems to high.

# Required Functionality

The following is the core functionality we are depending on Java for at the moment.

* Generate RSA 2048 or better yet 4096 public/private key and put it in a PKCS12 file
* Generate X.509 cert
* Parse X.509 Cert
* Validate X.509 cert chain
* Open incoming SSL connection using self signed cert (or self signed cert chain) & being able to accept any cert from the client and then get the client cert for further analysis
* Open outgoing SSL connection and be able to specify what cert you expect the server to present (or chain to) and specify what cert you want to present
* HTTP Server that can run over the ssl connection  [http://mxr.mozilla.org/mozilla-central/source/dom/network/tests/unit/test_tcpsocket.js](http://mxr.mozilla.org/mozilla-central/source/dom/network/tests/unit/test_tcpsocket.js) shows a test for opening both a client and server socket but it doesn’t use SSL which is actually the subject of two open bugs.
* HTTP client that can run over the ssl connection
* SSDP or ZeroConf support and/or the ability to send and receive UDP

# Useful resources

[http://mxr.mozilla.org/mozilla-central/ident](http://mxr.mozilla.org/mozilla-central/ident) - Provides a search interface over Mozilla's source code. I've been using it to find functionality.

# Thanks

Much thanks (or, depending on how things turn out, curses) to Oleg Romashin and Scott Blomquist for pointing this path out. And to John-Galt (who ever you are) from #addons IRC on Mozilla for pointing out NSS.
