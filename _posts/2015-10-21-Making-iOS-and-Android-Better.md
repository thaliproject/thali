---
layout: post
title: Making iOS and Android Better
---
We just did a new [release](https://github.com/thaliproject/Thali_CordovaPlugin/releases/tag/npmv2.0.1) that fixes issues with both Android and iOS. With Android we now support connecting to more than one device at a time. We also put in a temporary hack where once we discover a device over Wi-Fi Direct we will never forget it. This is great for demos, not so great in real life. This hack will go away when we switch to BLE. For iOS we ran into some odd issues with the streams in multi-peer connectivity framework deciding to just forget to notify us if too much data was sent. We found a work around so now iOS should work with larger data.
