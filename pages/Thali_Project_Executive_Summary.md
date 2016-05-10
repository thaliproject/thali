---
title: "Thali Project Executive Summary"
layout: page-fullwidth
permalink: "/ThaliProjectExecutiveSummary/"
header:
   image_fullwidth: "header.png"
---
5/9/2016

From: Yaron Y. Goland, Principal Developer, Microsoft Corporation

To: Decision Makers

Subject: PROJECT THALI EXECUTIVE SUMMARY

The purpose of this white paper is to provide the reader with sufficient understanding of the Thali Project to determine if their organization should be interested in both using Thali for their needs as well as investing developer resources into the Thali Project's open source community.

The Thali Project is an open source effort that was created and incubated by the Microsoft Corporation in cooperation with Rockwell Automation. Rockwell Automation will be using the Thali Project as the foundation of their next generation software platform called Project Stanton.

The Thali Project provides a software platform that applications can be developed on top of that can run on edge devices, IoT, mobile, tablets, laptops, PCs, etc. This software platform enables those applications to leverage the devices as stand alone application servers able to remain fully productive across a variety of network circumstances ranging from no connectivity, to connectivity only via local on device radios with immediately adjacent devices to LAN, WAN and Internet connectivity.

The Thali Project is now leaving incubation state and is being prepared to move to a V1 release on the Android and iOS platforms sometime before the fall of this year.

The Thali Project is being developed by an internationally distributed group of developers. The initial Microsoft development team had developers in the U.S., U.K. and Finland. Rockwell has had developers in the U.S. and Poland. Moving forward we expect to have developers added from other locations in Eastern Europe and possibly the Caribbean. All development is checked in under the MIT license at our GitHub repositories at https://github.com/thaliproject/.

The Thali Project was developed to enable robust computing across unreliable networks. It is intended to enable edge devices to remain fully functional regardless of the surrounding networking. Typical Thali scenarios involve situations where edge devices collect data either from sensors or from humans and then share that data by whatever means are available. Scenarios that have been explored with Thali include, but at not limited to:

Military Forward Deployment – Enabling soldiers to exchange data directly using tactical radios when deployed in the field or using their vehicle's or base's higher bandwidth facilities when in range. Thali would enable the communication of data like blood pressure, heart rate, geographic location, software updates, military orders, local chat, etc. across all connectivity scenarios. When no connectivity is available the device can still collect information and synchronize it when any form of connectivity becomes available.

Disaster Recovery – Enable first responders to collect situation information such as population surveys, infrastructure surveys, etc. directly onto their Thali enabled mobile devices and then have that information exchanged with any other responders or home base as they become available. This enables full data exchange even without any formal network infrastructure.

Factory – In factories and other high metal/high concrete or otherwise electromagnetic unfriendly environments Thali enabled mobile devices are able to directly communicate with each other information such as incident reports and chat. This is the primary scenario that Rockwell Automation has adopted Thali for.

The Thali Project accomplishes these goals by taking a traditional open source server technology, Node.js and running it directly on the edge devices. On top of the Node.js platform we run an open source based multi-master synchronization stack called PouchDB that enables us to synchronize data between any peer (including centralized infrastructure like the cloud) and determine what has changed since we last synch'd. We have also built a public key based identity system using Elliptic Curve Cryptography that allows us to securely authenticate and authorize peers. All data transfers occur over TLS connections secured using Pre-Shared Keys (PSKs) negotiated over the discovery channel using Elliptic Curve Diffie-Hellman key exchange plus HKDF to ensure key uniqueness. In addition to supporting cell and Wi-Fi we also use local device radios such as BLE and Bluetooth to enable devices to communicate directly with each other in the absence of cell/Wi-Fi. Our network communication infrastructure is standardized on TCP, even when moving over non-TCP transports like Bluetooth or Apple's Multi-Peer Connectivity Framework, so that code written on top of us can leverage standard TCP technologies like HTTP(S). All of this is then wrapped as a Cordova Plugin to enable for easy HTML/Javascript/CSS UX development and packaging on both Android and iOS. Therefore, someone developing an application on top of Thali has both the full set of tools available for HTML/Javascript/CSS UX development as well as the roughly 275,000 packages in NPM (with a few exceptions when dealing with native modules) available for their use.

So if your project could use Thali and if your project is in a position to make contributions back to the open source Thali Project then we would be interested in talking.

For more information, please contact:

Yaron Y. Goland

Principal Developer, Microsoft

Yarong@microsoft.com

http://www.thaliproject.org
