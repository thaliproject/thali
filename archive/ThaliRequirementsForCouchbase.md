---
title: Thali Requirements for Couchbase
layout: default
---

# Introduction 

This document describes Thali's requirements for using CouchBase and then walks through how a current proposal actually implements these requirements.

# Background 

The readers of this document are not expected to know anything about Thali's architecture so I provide a quick introduction here so our requirements will make sense.

<dl>

<dt>Thali Device Hub</dt>
<dd>In Thali each device (e.g. phone, tablet, laptop, cloud, whatever) contains a Thali Device Hub. This is a wrapper around CouchBase Lite.</dd>
<dt>Thali Application</dt>
</dd>This is a stand alone application running on a device that leverages the services, such as replication, offered by the Thali Device Hub.</dd>

</dl>

Note that when referring to a remote entity talking to a Thali Device Hub we have a habit of referring to the remote entity as a Thali Application. So one can actually think of a Thali Device Hub as a type of Thali Application. On the wire there is no way for a Thali Device Hub to know if the remote entity it is talking to is another Thali Device Hub or a Thali Application.

In Thali identity is provided by public keys. So, for example, when a Thali Application talks to a Thali Device Hub or when two Thali Device Hubs talk to each other mutual SSL/TLS auth is used. Both endpoints send certificates with their public keys. Each endpoint then examines the certificate it received, pulls out the public key and identities the other endpoint based on that key. Unlike HTTPS, there is no attempt to bind public keys to DNS names.

In a typical Thali device a Thali Application will handle its storage needs via the local Thali Device Hub. This provides a number of benefits:

* By extracting the data from the Thali Application it makes it possible for other Thali Applications to use the same data. So, for example, we can have a database in the Thali Device Hub for calendaring information and multiple Thali Applications can interact with that particular database.

* By storing data in the Thali Device Hub the application leverages the hub's logic for managing replicating that data. The Thali Application can just specify who is to receive copies of the data and the hub will manage the actual replication. This is a particularly big deal with mobile where logic is needed to make sure that replication occurs in a manner that doesn't drain the device battery dead.


This means that the Thali Device Hub (and by extension CouchBase Lite) needs to act as a central hub both for requests from local Thali Application services as well as from remote devices that want to get data. Note that in our security model there is an assumption that Thali Applications may have limited rights. E.g. the calendaring application has the right to read the calendaring DB in the local Thali Device Hub but not say the email or document or whatever DB. The user authorizes apps to access data and the Thali Device Hub enforces those permissions. Obviously permissions also apply to remote callers as well. E.g. a remote user might have been given the right to read a view on the user's calendaring DB that just provides information about free/busy. The Thali Device Hub would enforce those permissions.

# Scenarios 

## Local Connection 

1. A Thali application sends a request over localhost to its local Thali Device Hub

1. The Thali Device Hub accepts the connection which must be over SSL and must involve both a server cert from the hub and a client cert from the Thali application.

1. The Hub examines the exact request the application has made, looks at its permissions and decides if the identified application is allowed to execute the specified request.

## Remote Connection 

1. A Thali application uses Tor to bind the public key of a Thali Device Hub to a channel

1. The rest of the scenario runs as described in the local connection scenario

## Replication 

1. A Thali application sends a request to the Hub to cause a Push or Pull Replication

1. The URL for the remote database is a [Httpkey URL Scheme](HttpkeyURLScheme) or similar URL.

1. The request will also contain an Auth section with additional information such as what particular client cert to use when replicating

1. The Hub is able to establish a TLS mutual auth connection to the remote database using the server cert identifier in the Httpkey and using the client cert specified in the Auth section and so execute the replication related commands.

# Permission Change 

1. A Thali application used its legitimate permissions on the Hub to set up continuous replications, create views, create databases, etc.

1. The Thali application has withdrawn from it the permissions on the Hub that allowed it to perform the previous activities.

1. The Thali Device Hub is able to identify all the continuous replications, views, databases, etc. that were created with the now withdrawn permissions and decides which ones to stop/delete. Note that it is possible that other authorized Thali Applications asked for identical continuous replications, views, databases, etc. So even though one of the Thali Applications may have lost the right to say, create a continuous replication between a database and a particular remote endpoint, another Thali Application may have asked for the same continuous replication. In that case, if the second Thali Application is still authorized, then the continuous replication should continue.

## Requirements 

Note: Requirement IDs have no meaning OR order. They are just IDs to make it easy to talk about a particular requirement.

The following are requirements on CouchBase Lite driven by the scenarios above.

<dl>
<dt>Requirement L</dt>

<dd>CouchBase Lite MUST be able to bind the listener to whatever port is currently available on the device.</dd>

<dt>Requirement D</dt>

<dd> CouchBase Lite MUST be able to receive a SSL/TLS incoming connection</dd>

<dt>Requirement E</dt>

<dd> CouchBase Lite MUST be able to allow specifying the SSL/TLS server cert to present on SSL/TLS connections</dd>

<dt> Requirement F</dt>

<dd> CouchBase Lite MUST be able to allow code to be submitted by the CouchBase Lite user to validate the client cert sent on a SSL/TLS.</dd>

<dt>Requirement G</dt>

<dd> CouchBase Lite MUST provide a plugin where a customer can examine a Listener request and determine if it should be honored based on the client cert (if any) sent in the SSL/TLS connection as well as the full contents of the request. The validation mechanism MUST have access to the CouchBase Lite databases since authentication information is quite likely recorded there.</dd>

<dt> Requirement H</dt>
<dd> CouchBase Lite MUST provide a mechanism to associate with replications, views, databases, etc. the principal or principals who caused those entities to be created. CouchBase Lite MUST also provide a mechanism where by its possible to enumerate those objects and query their principals in order to enforce changes in permissions.</dd>

<dt> Requirement I</dt>

<dd> CouchBase Lite MUST provide a mechanism to support new URL schemes to specify the remote database in a replication.</dd>

<dt> Requirement J</dt>
<dd> CouchBase Lite MUST provide a mechanism to enable plugins to process and handle Auth sections of replication requests.</dd>

<dt> Requirement K</dt>

<dd> CouchBase Lite MUST provide a mechanism to enable plugins to handle establishing a remote connection and validating it.</dd>

</dl>

# How to meet these requirements with CouchBase Lite 

This section is just for discussion. It is not normative.

This text talks about how I tried to meet these requirements using some code I wrote. You can see the code at [Thali Auth branch of Core](https://github.com/thaliproject/couchbase-lite-android-core/tree/thaliauth) and [Thali Auth branch of Listener](https://github.com/thaliproject/couchbase-lite-android-listener/tree/thaliauth).

Requirement L is there because Thali Device Hubs don't actually care what local port they are running on since in general they handle binding of external requests via Tor and expect to handle internal requests through some kind of local rendezvous mechanism. To handle this I put in code in the Listener that accepts port 0. This causes the system to pick any available port automatically. The port can then be discovered via a call I added that will examine the connection and see what port was chosen. The real key here is not to have a situation where a Thali Device Hub can't run because the port it 'expected' to be able to use isn't available. Thali is specifically targeted at non-technical users and so needs to 'just work'. So if there is any port available Thali needs to be able to use it. Otherwise we have to say things to the user like 'please figure out why the port I want isn't available'. Yeah, right.

Requirements D & E are really handled by TJWS using its existing SSL support mechanisms. So in practice we get these requirements for 'free'. I just include them for completeness.

Requirement F we get partially for free. The actual validation code is handled at the TJWS layer. But we need a way to communicate what the client cert was, how it was validated, etc. from TJWS into CouchBase Lite so that the information can be used by plugin code to make authorization decisions. The current proposal handles this by extending the URLConnection class to expose a setter/getter for the SSLSession class. This class provides a set of read only (e.g. you can't screw things up using them) interfaces to query the status (and existence) of the SSL session. This includes any client certs (cert chains), Principals, etc. The reason for having extra data on the class is that it turned out to be really useful when I was debugging some nasty SSL issues and it's also how we solved the problem of discovering what port we dynamically bound to.

Requirement G is what the RequestAuthorization interface does. It is called before the request is processed and submits both the manager object (providing access to databases) and the urlConnection object (providing all the data about the incoming request) and returns a boolean to let the system know if it should continue with the request. What's nice about this approach is that it doesn't require specifying how the user of CouchBase Lite wants to handle authorization. They can introduce whatever kind of ACLs they want, not just the ones that Java happens to have (not supported on Android) interfaces for. What's really seriously broken about the current approach is that the way CouchBase Lite sees a request and how the RequestAuthorization object see a request are not the same and this is pretty much guaranteed to lead to security holes. For example, RequestAuthorization has to decide what database a request is about. This requires parsing the incoming URI. If the parsing code used by RequestAuthorization is in any possible way different than the code used by the rest of CouchBase Lite then the authorization decision may be wrong. The same goes for parsing the body. What we *really* need is a standard object to represent a fully parsed URL and a set of objects for representing the different kinds of parsed request bodies (see what I did with replication below). This would significantly reduce the probability of RequestAuthorization and the rest of the system 'seeing' a request differently. 

Requirement H first requires a way to 'flow' a Principal through the system. First I changed URLConnection to be able to record a principal object. I then changed LiteServlet to surface the principal (if any) generated on the SSL connection. I didn't worry about the principal on the servlet as TJWS always returns null for this but I do validate that it is null and throw an exception if it's not, that's just defense in depth. I then enhanced the replication object so it could record a list of principals (I'll get to why I used a list in another requirement). I then hooked it all up so when a replication object is created any associated principal is put on it. I haven't done the same work for views and databases yet. In the case of views and databases (and possibly other objects, I haven't done a full survey yet) it probably isn't necessary since these objects have their own unique names and I can index that myself. Replications were the hard ones because they aren't truly uniquely named.

Requirement I mostly required removing the existing code that required that the remote URL be a HTTP URL. I then put a method on Authorizer that lets it return a HttpClientFactory. This can be null (and is for Facebook and Persona) in which case a default factory is used. By providing my own factory I could handle my custom URL type as well as other requirements below.

Requirement J I dealt with by introducing the AuthorizerFactory. This accepts a request as input and either returns null if it can't handle the request, throws an exception if it knows the request is bad or returns an authorizer if it can handle the request. It is during the call to the AuthorizerFactory that I can set up things like SSL certs and such which will then be in place when the Authorizer's HttpClientFactory method is called.

Requirement K I dealt with using the AuthorizerFactory since as part of creating the Authorizer and its HttpClientFactory I can also specify my own SSLSocketFactory where I can handle all the TLS mutual auth logic however I want.

I also, btw, introduced the AuthorizerFactoryManager. The idea is that one can specify multiple AuthorizerFactory objects if one wants in order to handle multiple, independent, authorization mechanisms. In theory this could have all been wrapped inside of a single AuthorizerFactory instance but it seemed 'neater' and more pluggable to use the AuthorizerFactoryManager.
