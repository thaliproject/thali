---
title: CouchDB Clients to Thali-enable
layout: default
---

# Introduction

Because we need a custom HTTP stack to enable our public key based identity infrastructure over SSL/TLS and TOR support we need to take an existing CouchDB client and enhance it. Below we walk through the choices we looked at and how we made our pick. Note that the other choices could, presumably be enabled to support Thali. But we had to start somewhere.

# Java/Android

In the end we picked Ektorp but it has problems (easily fixed at the cost of perf) with CouchBase lite Android, see [here](https://groups.google.com/forum/#!topic/mobile-couchbase/ht0kAw2QFpc) for the easy work around.

Apache has a whole list of potential client libraries available [here](http://wiki.apache.org/couchdb/Getting_started_with_Java).

In evaluating each library I need to look for:

* Does it have an Apache 2 compatible license?
* Does it run both on desktop and Android?
* Does it support easy access to a reasonable HTTP client that we can use to get our TLS mutual auth with self signed cert functionality working?
* Does it deal with JSON in a manner that won't make us won't to rip our eyeballs out?
* Does it help with admin style web commands such as setting up replications or configuring options on the server?
* Does the project have an active community?
* Does the project have documentation worth a damn?


Library | License | Desktop & Android | HTTP Client Access | JSON | Admin | Community | Docs 
--- | --- | --- | --- | --- | --- | --- | --- 
Ektorp | Apache 2.0 | Dedicated support for both | Yup, including, it seems, the TLS hooks we need | Jackson | Help with setting views | Their mailing list looks pretty dead but their commit rate is low but reasonably consistent | Yes! 
JCouchDB | Apache 2.0 | It's unclear if it works on Android | Not immediately clear but given the client libraries it's using it should be possible to hack it in | JSONObject | Views and such | The forum has light traffic and they seem to do a major release once a year | Yes! 

* JRelax looks dead so I didn't put it in the table. They haven't had a commit in 2 years.
* JCouchDB hasn't had a check in in more than a year and a half.
* DroidCouch hasn't had a check in 3 years.
* CouchDB4J hasn't had an update in a year.

Given that CouchDB Lite Android uses Ektorp and that Ektorp seems to be the most active I'm going to start there.

# C#/.net 

I suppose it's a good sign that there are tons of CouchDB clients for .net as it exposes how easy the CouchDB protocol is to support. But it is a pain when trying to actually pick one to use. [http://wiki.apache.org/couchdb/Related_Projects] has a list of projects.

In evaluating each library I need to look for:

* Does it have an Apache 2 compatible license?
* Does it run on desktop? store? phone?
* Is it on nuget?
* Does it support easy access to a reasonable HTTP client that we can use to get our TLS mutual auth with self signed cert functionality working?
* Does it deal with JSON in a manner that won't make us won't to rip our eyeballs out?
* Does it help with admin style web commands such as setting up replications or configuring options on the server?
* Does the project have an active community?
* Does the project have documentation worth a damn?

Library | License | Desktop, store & phone | nuget? | HTTP Client Access | JSON | Admin | Community | Docs | Notes 
--- | --- | --- | --- | --- | --- | --- | --- | --- | --- 
MyCouch | MIT | Desktop & Store, not sure about phone | yes | Yes, via IConnection  Raw JSON & POCO | I couldn't find support for creating a replication command | I can't find any sign of a real community but there are regular updates. | They exist but are rather brief. | Their claim to fame seems to be that they are completely asynchronous but use .NET's asynch support to make that not suck.
LoveSeat | MIT | I can't tell | Yes, but I think it's an old version | No | They have a POCO model.| Yes | Not really, last check in was 3 months ago, last release was 2 years ago! | Yes | This is synchronous and simpler than say MyCouch.
Divan | MIT | I can't tell | No | No | Yes | Lots of the usual but no replication | Mailing list link doesn't work and no check ins for 6 months | None | Looks dead although I was under the (mistaken?) impression that a lot of people like it and it does have sample code
chesterfield | MIT | I can't tell | yes | No | Yes, POCO | Yes | No | No | This is a fork of both LoveSeat and DreamSeat. No check ins/releases for 9 months. They also have dependencies on a ton of DLLs.

<dl>
<dt>ottoman</dt>
<dd>According to GitHub there hasn't been a check in three years.<dd>

<dt>Relax</dt>
<dd>According to GitHub there hasn't been a check in 3 years.</dd>

<dt>relax-net</dt>
<ddt>According to Google code there hasn't been in a check in in 2 years</dd>

<dt>skitsanos</dt>
<dd>No docs. No listed license. Just code. Although it looks active.<dd>

<dt>couchbrowse</dt>
<dd>A GUI and a couch library but no updates since 2008.</dd>

<dt>DreamSeat</dt>
<dd>A fork of LoveSeat, it hasn't had a check in 11 months or a release in 2 years.</dd>

<dt>CouchDB.Net</dt>
<dd>No check ins since 2012</dd>

</dl>

For no particularly good reason beyond the fact that it supports Win RT/8.1 I will go with MyCouch.
