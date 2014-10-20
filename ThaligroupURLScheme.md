---
title: Thaligroup URL Scheme
layout: default
---

# Introduction 

The thaligroup URL scheme is used to identify a group defined by a particular entity. The primary scenario for thaligroup URLs is in replication requests to Thali Device Hubs (TDHs). An application would submit a CouchDB replication request to a TDH with a thaligroup URL as the source or target. The TDH would then be responsible for expanding the single replication request into multiple requests, one for each member of the identified group.

# Requirements 

The thaligroup URL MUST support unambiguously identifying a particular group as defined by a particular principal.

The thaligroup URL MUST support translating the thaligroup URL into other URL types, such as HTTPKEY, in order to translate the Thali group URL into a set of URLs, one for each member of the group.

# URL Syntax 

<pre>
 thaligroup-URI = "thaligroup:" "/" owner "/" group "/" path
 owner = type-value
 group = segment-nz-nc / type-value
 path = type path-abempty ["?" query] [ "#" fragment]
 type-value = type ":" value
 type = scheme
 value = segment
</pre>

The productions path-abempty, query and fragment are defined in section 2.7.2 of [http://tools.ietf.org/html/rfc7230 RFC 7230](http://tools.ietf.org/html/rfc7230 RFC 7230). The productions segment-nz-nc, scheme and segment are defined in [http://tools.ietf.org/html/rfc3986 RFC 3986](http://tools.ietf.org/html/rfc3986 RFC 3986)

All owner, group and path types MUST be registered with IANA following <code>[insert appropriate magic here]</code>.

# Owner 

Groups MUST be defined in the context of an owner. The owner production identifies who that owner can be. All identity-key productions defined in [Httpkey URL Scheme](HttpkeyURLScheme) are automatically registered as owner values.

[OPEN ISSUE: By only using the identity-key we identify the owner by their public key. But that isn't actually resolvable. Do we want to also stick in the ability to put in an onion address or other owner authority?]

# Group 

The group production defines the name of the group being referenced. This specification does not define any group type-value productions. Therefore for now only segment-nz-nc values can be used to identify groups. Note however that any group type-value productions that are introduced in the future MUST work with the URI comparison rules described below.

# Path and transformation 

This specification defines the path type "httpkeysimple". The semantics of this path type is that if the thaligroup URI is to be transformed and contains a "httpkeysimple" path then the transformation MUST be to a httpkey URL.

A transformer transforming a thaligroup URI with a path of type "httpkeysimple" MUST:

1. recognize the owner type
1. recognize the group type if present
1. have reasonably up to date (the exact definition of which is left undefined) knowledge of the membership of the group identified by the combination of the owner and group productions
1. enumerate the membership of the group and the group membership must, when resolution of group membership is complete, consist exclusively of a set of httpkey URLs. The produced URLs MUST not contain query or fragment productions and the path-abempty MUST NOT end in a "/".
1. return to the requester, subject to access control requirements, a (possibly empty) set of the httpkey URLs enumerated from the group membership with the (possibly empty) value part of the path production appended to the end.

A transformer MUST reject transformation requests for thaligroup URIs whose path type is not supported.

# Comparing thaligroup URIs and their component parts 

By default only simple string comparison as defined in section 6.2.1 of [RFC 3986](http://tools.ietf.org/html/rfc3986) MUST be used to compare two thaligroup URIs for equality.

In terms of comparing owners on their own the rules specific in [Httpkey URL Scheme](HttpkeyURLScheme) MUST be used.

To just compare the group parts of a thaligroup URI the comparison logic MUST first prove equality of owner as specified above and then MUST do a simple string comparison of just the group production. If both parts are equal then the identified groups are the same.

# Example 

<pre>
 thaligroup:/rsapublickey:65537.22912332915818678422150816008567595304572
 530270766238859922343032791612966824557932947659960333351153388435158284
 666309248825175974911964431170141623906931040856664130981842177060601883
 093191311741405530353180334971823580750344435314197473833181898842010566
 738993075259001808566463348027523141542809926498335324273802899607724831
 414078729370096517958658346374270205621071263361779683051242363287222987
 735418011187771204718883145520252089502815273843893528710808519526473112
 874774561851138101896806013797598277895539034330328094877276084533967507
 831910523283288815798592996543256860992426377095371666673172691091922277
 /goodfriends/httpkeysimple/photos
</pre>

Please note that the introduced white space is just for readability purposes.

This thaligroup URI specifies that it refers to the group named goodfriends as defined by the identified owner. If transformed it would return zero or more httpkey URLs containing the membership of the goodfriends group as defined by the identified owner/group name combination and would append "/photos" to each of the httpkey URLs in the group.

# Q&A 

## Is this spec done? 
See [Httpkey URL Scheme](HttpkeyURLScheme), the same applies here.
