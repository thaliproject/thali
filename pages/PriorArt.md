---
title: Prior Art
layout: page-fullwidth
permalink: "/PriorArt/"
---

## Groove

### Microsoft Office Groove Security Architecture

The [Groove (2007) Security White paper](http://www.nsa.gov/ia/_files/support/Office_Groove_Security_White_Paper.pdf) describes Groove's security architecture at a high level. Excerpts:

#### From the overview

* Uses standard Web protocols to cross firewalls. Until now, allowing users to securely collaborate with others outside of their organization meant enlisting IT resources to set up a virtual private network (VPN) or alternative solution, such as a secured, shared Web site. With Groove 2007, users are empowered to collaborate through firewalls securely, with no extra effort required by IT. Groove 2007 firewall transparency uses standard Web protocols to avoid requiring network administrators to open special ports in the firewall.

* Encrypts all content on disk and over the network. Groove 2007 automatically encrypts all user accounts, workspaces, and their contents locally using 192-bit encryption. Furthermore, all content and activity within a workspace that is sent across the network is also encrypted and can only be decrypted by other members of the workspace.

* Provides user-driven workspace access control. Role-based access control is a security feature that most organizations want, but, in practice, find unwieldy to implement. Traditionally, IT resources are required to move users into separate access control lists. After initial setup, these access control lists remain static and inflexible. Groove 2007 pushes the power to determine user permissions to the manager of each workspace. With Groove 2007, setting the role of a workspace member and configuring access rights take just seconds

#### Identity verification

In the manual scenario, users examine the digital fingerprints of other users' public keys. Digital fingerprints are hexadecimal strings generated dynamically using a secure hash algorithm (SHA­1, approved by Federal Information Processing Standards) to hash a user's public keys. Fingerprints are easier for people to read than public keys, yet just as secure. Public keys, which Groove 2007 uses to verify signatures on messages, travel in a user's contact information. The contact information is a user's electronic identity; it can be stored in the Groove Public Directory on local directory servers where it’s readily available to other users. Fingerprint based authentication involves contacting the person using an out­of­band means such as a phone call to confirm their fingerprint value is correct.

### Peer to Peer Chapter 18: Security

We have asked permission to reprint [the chapter](http://my.safaribooksonline.com/book/technology-management/059600110x/iiidot-technical-topics/ch18-1-fm2xml) on Groove's cryptographic mechanisms.
