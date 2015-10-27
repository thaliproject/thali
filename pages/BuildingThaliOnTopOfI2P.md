---
title: Building Thali on Top of I2P
layout: page-fullwidth
permalink: "/BuildingThaliOnTopOfI2P/"
---

Thali needs to work even in completely locked down network environments. We are seeing more and more situations where firewalls don't allow UDP at all. That basically the only communication is outgoing (not ingoing), only TCP and only on port 80 or 443. In fact more and more networks won't even let people on the same network access point talk to each other! So the firewalling is both internal and external.

These requirements cause a few issues for I2P currently:

1. I2P's introduction mechanism only runs in UDP
1. I2P doesn't run on 'privileged' ports (e.g. those below 1024)

Both of these issues are resolvable. They just haven't been yet.

Thanks to STR4D for the great conversations on I2P and Thali.
