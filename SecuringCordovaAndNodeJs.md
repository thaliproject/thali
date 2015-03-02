---
title: How to secure communication between Cordova and a local Node.js Server?
layout: default
---
Thali's architecture assumes that apps are written using Cordova and we will provide a Cordova plugin that will support launching a local Node.js process and running Node.js files. In general we expect that the Cordova Webview and the Node.js server will have separate life cycles. But we certainly need them to be able to securely communicate with each other. How can we do that with the least amount of pain?

# Scenario
We have two primary scenarios that drive our requirements for how the WebView talks to the background node.js process.

The first scenario we'll take from PouchDB. PouchDB uses XMLHTTPRequest when talking to a remote CouchDB server. In this case one can imagine the PouchDB instance in the WebView as one server and the PouchDB instance running in Node.js as a separate server. We will use filtered synchs from the WebView PouchDB instance to pull data from the much larger Node.js instance. This provides latency advantages as the data lives in the WebView and doesn't have to be moved around. The key to making this work is that PouchDB in the WebView uses XMLHTTPRequest to make its CouchDB HTTP protocol requests and PouchDB in node.js exposes an express front end to handle such requests. So we need that to work well.

The second scenario is using node.js to host web content and access it from the WebView. We should be able to use the Node.js server as a repository for both static content (pictures, movies, etc.) as well as dynamic content. So we need the usual web features like anchors, images, page navigations, etc. to work from the WebView to node.js in the background.

# Requirements
Our requirements are that XMLHTTPRequest as well as in WebView navigations such as HTML elements with HREF attributes, form elements and Javascript navigations all work securely with the node.js server.

# Threat model
The Node.js server listens on some port. That port is retrieved by the Cordova bridge who then provides it to the WebView who then uses localhost to talk to the node.js server. All done, can we go home now?

The problem is that any local application can talk to localhost. So if there is a “bad application” on the device then we have problems. For example, a bad application running locally could make requests to the node.js server over localhost and if we don't do anything the node.js server won't realize that the requester isn't its matching application.

Similarly there are various attacks where the bad application could get the port the node.js server is using. In that case the client could end up talking to the wrong server. The port hijack attacks are a bit more fiddly. For example, if there is a fixed port then the bad application could use a race condition to grab the port first. If there is a dynamic port the bad application could figure out which port it is and use various attacks to knock the node.js server offline and take over the port itself.

In all cases we end up in a situation where a bad application can intercept, alter, etc. private data. Bad.

# Our preferred solution - HTTPS plus a secret in the URL path
As we will see below there are tons of ways to meet our requirements. But we want a way that is invisible to the WebView programmer and uses well established mechanisms for the node.js programmer.

To secure communication between the WebView and the node.js server we want to use HTTPS. And to authenticate the client we want to put a secret into the URL path.

The node.js server will expect to advertise its local endpoint using a self signed cert and will validate incoming requests by looking for the secret in the path. The node.js server will agree on the cert and the secret via config provided by the Cordova bridge.

From a WebView perspective the programmer gets the HTTPS address from the Cordova bridge and just uses it.

The upside to this approach is that it's really simple. The Webview “just works” and the node.js server does things that node.js servers do like provide HTTPS endpoints and parse URLs.

The downside is that this approach opens the WebView to a [cross site request forgery attack](https://en.wikipedia.org/wiki/Cross-site_request_forgery) if the application accepts URLs from external sources. But this attack only works if the requests have side effects. If a WebView is using requests with side effects and it accepts external URLs (or even scripts) then it needs to use the same defenses against these attacks as any website would use. There is nothing magical about the peer to peer web, it's the web and all the usual security issues still apply.

## Node.js Developer's Experience

Our expectation is that we will provide a standard library for Node.js authors. That library will provide the Node.js author with a cert that it should use when creating an endpoint for use with its matched WebView as well as the secret value the client will use in the path. In other words all HTTPS requests to the node.js server from the WebView will be of the form “https://localhost:[port]/[secret]/”. 

Because we need to be flexible in terms of what port we run on our expectation is that when the node.js server wants to open a connection for the local WebView it will take any port it can get and then call the provided Node.js library to tell it what port it's listening on with the provided cert. It's then up to the underlying Cordova code to relay that port to the client WebView (if necessary, see below).

## WebView Developer's Experience
The Cordova bridge will expose a Javascript API in the WebView where the WebView coder can retrieve the HTTPS address for the local node.js server.

# Frequently Asked Questions
## Won't the WebView reject the node.js server's X.509 cert?
By default the answer is yes. The Node.js server doesn't have a cert that will root to any of the globally recognized CAs. So our solution depends on being able to fix this. There are two different ways to fix this, one or the other of which seem to be supported by all existing WebViews. One way is to provision the Node.js server's cert in the WebView's cert store. In that case the HTTPS request will work. The other option is that some WebViews allow the Cordova bridge to intercept all requests. In that case the Cordova bridge can do its own SSL authentication and validate the node.js server's cert. Also note that WebViews that support intercepting requests allow us to give a fixed port to the WebView and then rewrite the port to whatever the node.js server is using.

## A secret in a URL? Isn't that a really bad idea?
Yes, it is. URLs tend to leak and if the URL with the secret somehow leaks to the bad guy then the bad guy can impersonate the client. But there is always a trade off between security and usability. In this case it's pretty hard (but obviously not impossible) to leak the URL. Alternative solutions, explored below, like HTTP headers, TLS mutual auth and cookies all have trade offs that make them less than perfect for our purposes. 

I wouldn't be surprised however if we don't eventually switch to a model where we use both a HTTP header secret as well as a path secret. The path secret used for browser elements and the HTTP header secret for XMLHTTPRequest. Although my hope is that we'll find out that it's not that difficult to implement mutual TLS auth and we will use that instead.

## How about using a client secret in a cookie?
We can do fun things like use local Javascript in the WebView to set a cookie for localhost with the secret. That way the secret doesn't live in a potentially leakable URL. The problem is that cookies are generally scoped to domain, not port. It's true that some browsers have implementations that will sometimes honor a port scope but this is not universal. The only scoping honored beyond domain, at least universally, is protocol (e.g. HTTP vs HTTPS). So any bad application with a localhost service that can get the app to send it a request for any reason (however innocuous) will also send along its secret.

Cookies also open up the possibility of a [cross site request forgery attack](https://en.wikipedia.org/wiki/Cross-site_request_forgery) (CSRF) where an attacker passes a URL to localhost and the right port into the app, somehow and the request is accepted because it will go out with the secret in the cookie.

There is a work around but I'm not sure if it will work on all WebViews. What we could do is at run time generate a 128 bit cryptographically secure random number. We could then map that number into a domain name safe character set. Typically there is a 33% bloat for using encodings like UUENCODING so that would turn out 128 bit value into a (128 bit/8 bits/Byte * 1.33) = 22 character value. Which can easily fit into a single domain name segment. So long as the local OS allows an app to set the equivalent of its own HOSTs file and so long as that HOSTs file cannot be viewed by other apps then in theory we could use a cookie.

CSRF attacks won't work because the attacker can't guess the domain and no other localhost servers will get the cookie because they can't guess the domain either.

But I'm not sure how many environments not just let us set a value in the HOSTS file but then make it so that no other apps can see that host file.

## How about using TLS mutual auth?
This would be ideal in many ways as it would securely authenticate the client and the server. The problem is, how do we set this up so it works from the WebView? Especially the client cert part? This is doable in various ways in various WebViews. As discussed separately some WebViews allow all requests to be intercepted and re-written. In that case the Cordova wrapper could intercept requests, look for ones to the node.js server and then rewrite them to make the request locally using TLS mutual auth. And of course node.js supports TLS mutual auth so there should be no problem on that end. This solution would work seamlessly, where it works. But it's not clear to us if it works everywhere. But the good news is that for WebViews that do support it, they should be able to use this mechanism on the WebView side without any problem. It's on the node.js end that there will be problems. If this mechanism turns out to be reasonably easy to do for some WebViews then I think we'll change our node.js API to specify if the Node.js server needs to validate a secret in the URL or a client cert.

I suspect that we'll push pretty hard over time to switch to using TLS mutual auth. The crypto overhead is annoying but it cleanly solves port hijack scenarios without leaking any data.

## How about use a secret in a HTTP header?
A really nice and easy solution that doesn't suffer from CSRF attacks like cookies or leakage like URLs is using a HTTP header to supply the secret. In the case of something like XMLHTTPRequest we can even do this invisibly to the WebView programmer. We just hijack all of XMLHTTPRequest and replace it with our own wrapper that intercepts requests to the node.js server and leaves the rest alone. We can then silently supply the secret and call it a day.

The problem is, how does this work with href attributes in the HTML or forms and such? Yes, some WebViews let us re-write requests but in that case we would probably just use TLS mutual auth since it's more secure in that it doesn't put any secrets on the wire.

## Couldn't we just use one secret in a HTTP request header and a separate secret in a HTTP response header and get rid of TLS all together?
The whole HTTPS thing is fiddly. It requires a WebView that either allows messing with its local cert store or intercepting requests. It would be nice if we had a way to create a secure connection that didn't require HTTPS. It's worth keeping in mind that we can generally treat localhost connections as being secure in the specific sense that once a connection is made nobody on the machine (who isn't root) can see what is being transmitted. So we don't need encryption to secure the localhost content. We really just need a way to do a secure handshake.

So in theory what we could do is put in a HTTP request header with secret Sreq and a HTTP response header with secret Sres. If the node.js server gets a request without Sreq it refuses it. If the WebView gets a response without Sres it rejects it.

There are several problems with this approach.

First, it only works for XMLHTTPRequest where we can set headers. It won't work for HREF attributes or forms.

Second, if the node.js server port is hijacked then it leaks data from the client. The attacker will see what is in the request even though any response from the attacker won't be honored by the WebView.

Third, in the case of race conditions the attacker might actually beat the node.js server to the desired port, get Sreq in a request and then drop out and let the node.js server take the desired port. In that case the attacker now has Sreq and can impersonate the client.

We can get rid of the third problem by using something like HTTP Digest. Assuming the node.js server uses unguessable nonces on each new connection then Sreq will never go on the wire in the clear and so can't be hijacked by the attacker.

But in practice we can't do much about the second problem. Even if we introduced a separate hand shake phase (for example, we do a GET to a bogus address and then do mutual validation of Sreq/Sres) it's impossible in practice for the WebView code to know if it's on the same connection it was before. In other words the node.js server could start working, the WebView could do the handshake and then an attacker could knock the node.js server offline and take it's port. The browser code won't realize that it's now talking on a new TCP/IP connection (one it hasn't done the handshake on) and will pass data happily.

And the first problem we really can't do anything about with this type of approach unless we want to rewrite the HTML to turn the HREFs into javascript calls.

## Couldn't we use RPC rather than localhost to communicate between the WebView and Node.js?
The answer is - yes.

Right now Cordova uses an RPC to move requests out of Javascript in the WebView to the local Cordova bridge code. We can certainly set up something similar with Node.js. In this case all communications are using in-process RPC and so are secure. The bridge code literally has the WebView and node.js objects in memory and can relay requests back and forth.

To make this work on the WebView the WebView has to call a local Javascript library that is actually backed by the Cordova bridge. This would be easy to do with something like XMLHTTPRequest and we might even be able to hook it into something like a Javascript page navigation but what are we going to do with HREF's in HTML or form submissions? We could try fancy tricks like re-writting the HTML to turn these into Javascript requests but that's kind scary.

Also in practice these bridges often can't move binary data around. They often have to turn content into strings. We actually tried this with a WebView to Java bridge and the performance implications for things like pictures and movies were pretty dire.

This also begs the question of how we surface the data in Node.js. Are we going to create a pseudo-network endpoint? We could do that but it seems like a lot of work.

## Couldn't we just pin the request and response ports and skip the rest of this nonsense?
In theory if we knew what port the client was going to use and what port the node.js server was going to use then we could use the Cordova bridge to exchange this information and as long as everyone stays on their assigned port then we should be fine.

The problem is that in practice we have no clue what port the client is going to run on and it's very difficult for us to know if the server is still running on its assigned port.

The browser HTTP stack is going to grab any port it can and there really is no way to know ahead of time what port that will be or how long it will use it. So as a practical matter there is no way for the Cordova bridge to know what client port to tell the Node.js code to look for.

There are also various attacks where a bad application could knock the node.js server off its agreed on port and take the port over itself. Yes, this could eventually be detected but in the meantime the bad application is slurping up data.

If there is a WebView that supports intercepting requests then this would all change but at that point just use TLS mutual auth, it's a tiny bit more work (since we use it everywhere for Thali in node.js it's not extra work there) and it solves all sorts of hijacking attacks.
