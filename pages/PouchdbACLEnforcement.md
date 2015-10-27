---
title: Defining an Express-PouchDB middleware for enforcing ACLs
layout: page-fullwidth
permalink: "/PouchdbACLEnforcement/"
---

# Defining an Express-PouchDB middleware for enforcing ACLs

This document defines a set of behaviors for an Express middleware designed to enforce ACLs in PouchDB. This middleware tries to make relatively few assumptions about how the underlying ACL layer it is asked to enforce actually works. Instead the middleware is intended to take callbacks from the programmer using it and it is in those callbacks that actual ACL decisions will be made. The middleware's job then is to make it as easy as possible to enforce those decisions in a secure manner for Express-PouchDB.

We do however make some very specific assumptions about how the ACL layer built on top of this middleware works. We assume that:
* The ACL engine will need to make ACL decisions at the granularity of a document (and its related attachments)
* The ACL engine will explicitly ban all admin REST endpoints (defined in detail later) as "all or nothing", that is, either one is an admin or one is not

# Basic Questions
1. Can we build middleware that lets us see requests before PouchDB and see responses after PouchDB?
2. Can we set things up with Express-PouchDB so that we can parse the JSON ourselves and pass in our own (modified) request bodies and query lines without having to serialize them back out and requiring Express-PouchDB to do double parsing.
3. Can we set things up with Express-PouchDB so that we can get the response body to an Express-PouchDB response as pure javascript object (e.g. before being serialized) and edit it before it gets serialized to a string?

## Fail safe

Although it is guaranteed to cause incredible annoyance the only safe way to deal with requests and responses from PouchDB is via a fail safe mechanism whereby if any request or response element is not recognized then the request/response MUST fail. This means that if Express-PouchDB adds some new route that the ACL middleware doesn't know about or if Express-PouchDB adds new fields or arguments in existing routes, we will have to fail requests that leverage that new capability. Same thing for new fields in responses. :(

## Express-PouchDB Middleware
The following is all the middleware I found Express-PouchDB installing. I explore each one below to see which we should keep, which we should remove and for those we are keeping, what implications they have for Thali.
### config-infrastructure
This is actually a set of useful functions that set up the infrastructure that Express-PouchDB uses to handle parsing requests.
### logging-infrastructure
This sets up PouchDB's logging infrastructure and isn't directly used to handle requests.
### compression
This loads the npm compression package which adds deflate and gzip support. Its functionality should be invisible to us. It shouldn't even effect our eventual quota management since the compression is a transport artifact, it only reduces the size of the data on the wire, not in storage.
### disk-size
This puts in support for the npm package pouchdb-size which figures out the size of the underlying DB across various back ends like leveldown. In theory we shouldn't leak the size of the DB to unauthorized callers but such a leak wouldn't actually happen in this plugin. It would be a side effect of calling one of the administrative interfaces. So we should leave this plugin alone but we need to be on the lookout for where it actually gets used.
### replicator - REMOVE
I believe this primarily installs the npm pouchdb-replicator. The files for this plugin live [here]('http://bazaar.launchpad.net/~marten-de-vries/python-pouchdb/0.x/files/head:/js/pouchdb-replicator/').  This is used to power the \_replicate endpoint. In general we shouldn't support that endpoint since we manage replication ourselves at the Thali level. So I think we should remove this route all together.
### routes/http-log
This is middleware that logs the method, path and ip on requests and ip, method, path and status code on responses. Logging this data in a privacy sensitive app like Thali worries me in general.
### routes/authentication - REMOVE
This is middleware intended to provide cookie and basic based authentication sessions based on CouchDB's authentication mechanisms. See [here](http://bazaar.launchpad.net/~marten-de-vries/python-pouchdb/0.x/files/head:/js/pouchdb-auth/) for details. We use a TLS based authentication mechanism that is a full layer below this one. So most likely we should just yank this middleware all together.
### routes/special-test-auth - REMOVE
This is used during testing to slip in pre-configured name/passwords. It's not appropriate for us and should be removed.
### routes/authorization
This is middleware that protects certain sensitive REST endpoints by validating that the caller is in the "\_admin" role. I hope we can leverage this to put callers into the \_admin role and then let this middleware handle the details.
### routes/vhosts - REMOVE
This installs pouchdb-vhost which is a NPM that implements the CouchDB Virtual Hosts config functionality. We will not be using virtual hosts in Thali so we should remove this.
### routes/rewrite - REMOVE
This installs pouchdb-rewrite which is intended to emulate the CouchDB rewrite functionality in PouchDB. This is part of the \_design doc functionality. My own feeling is that this is way too easy to cause serious pain and suffering in Thali land. So my strong inclination is to remove it.
### routes/root
### routes/log
### routes/session
### routes/session-stub
### routes/fauxton
### routes/config
### routes/uuids
### routes/all-dbs
### routes/replicate
### routes/active-tasks
### routes/db-updates
### routes/stats
### routes/db
### routes/bulk-docs
### routes/all-docs
### routes/changes
### routes/compact
### routes/revs-diff
### routes/security
### routes/view-cleanup
### routes/temp-views
### routes/find
### routes/views
### routes/ddoc-info
### routes/show
### routes/list
### routes/update
### routes/attachments
### routes/documents
### validation
### routes/404

## couchConfig
There is now a couchConfig file where much of the functionality of PouchDB can be configured. We need to review it to see if there are things in there that could be a security issue.

## Pull Replication
Our immediate thinking is that we will block all APIs from non-admin users that are not immediately needed by PouchDB's pull replication functionality. So we need to walk through the replicator and understand what REST endpoints it is using.
