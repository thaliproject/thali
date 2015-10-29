---
title: Thali and Tor Onion Proxy
layout: page-fullwidth
permalink: "/ThaliAndTorOnionProxy/"
categories:
    - technologyinvestigations
---

# Introduction

Thali Device Hubs are, by default, exposed as Tor hidden services. This means that Thali needs to both be able to host hidden services and to talk to hidden services. Below we will explore what options are available to both talk to and host hidden services. This paper assumes the reader has a pretty good familiarity with Tor.

# The Plan

Since we need the ability to both host a hidden service and to talk to other people's hidden services it makes sense for us to stick with the Tor executable. The reason is that near as I can tell only the Tor executable can actually host a hidden service. Libraries like Orchid can talk to hidden services, but not host them. So this means we need the Tor executable no matter what in order to provide local Tor hidden service hosting. But the Tor executable can also handle talking to hidden services. It does this via a SOCKS proxy. We would program the TDH and Thali apps to speak to the local SOCKS proxy and in that way they can reach hidden services. So the Tor executable is an 'all in one' for us.

Unfortunately using the executable isn't much fun. It's written in C and is generally taken as a binary dependency. So we'll need to pack it up in our desktop distributions. Thanks to Guardian and Briar it was relatively easy for us to create our own hosting tool for Java and Android, see [https://github.com/thaliproject/Tor_Onion_Proxy_Library](https://github.com/thaliproject/Tor_Onion_Proxy_Library)

# Dramatis Personae

The following are the key pieces of software that enable one to use Tor. I'm focusing on solutions that I think may be applicable to Thali. This means that they run on the desktop and/or Android and for which there is a way for us to play with them from Java.

## Primary Tor Executable

Tor's primary code is written in C and runs as an executable on the desktop. In theory there is no Android version but I'm told that Orbot actually uses a migrated version.

The Tor executable provides a number of services but two interest us here. First, it provides a SOCKS proxy. This proxy can be used by SOCKS enabled apps to connect via Tor to the open Internet or specifically to Hidden Services. The Tor executable also provides the ability to host hidden services. This ability is enabled either by a configuration file or I think it can also be enabled via the [control protocol](https://gitweb.torproject.org/torspec.git/blob/HEAD:/control-spec.txt).

Our purposes are simple enough that just configuring the TDH via the config file and then using the SOCKS proxy could probably meet all our needs.

## Orchid

Orchid is a Java library that implements a Tor client. It can actually stand up a SOCKS proxy like the Tor executable but it can also provide native Java abstractions like sockets and streams to enable connecting to hidden services or the open web via Tor. Note that it doesn't appear to support hosting hidden services, just talking to them. So even if we wanted to use Orchid it can only solve half our problem.

I am a bit worried about Orchid personally. Issues that have been filed on their Github as well as PR requests have gone unanswered, in some cases, for years. While I'm told Orchid is working it doesn't seem to be interested in having a robust community. That makes me concerned about its long term viability.

## Orbot

Orbot is a port of Tor to Android. It runs as a stand alone application that provides a local SOCKS proxy (amongst other things) and can also (it claims) host hidden services. The main challenge with Orbot is that it has to be installed as a stand alone APK and then started before anything will work. This is not the best user experience. But see below.

## Onionkit

This is a  Java library from the same folks who gave us Orbot that amongst other things includes something called Orbot Helper. This library will let us issue a check to see if Orbot is installed and if not prompt the user to install it. So the idea is that the TDH could use the Orbot Helper library from Onionkit to see if Orbot is around and if not, prompt the user to install it. The Orbot Helper library will also apparently let one programmatically configure a hidden service which Orbot will then set up and proxy. The helper library will also let one programmatically start Orbot if it isn't currently on.

The only problem I've seen is that when creating a hidden service there doesn't seem to be a way to find out the .onion address of the service that was created! Also we would like to use long lived .onion addresses so how does one make sure one gets the same address?

## Briar

The Briar project has their own Orbot like code that they can embed in Android. It is the basis for our own library.

## Tor Onion Proxy Library
This is from Thali and generalizes the Briar code to work on Android, Windows, Mac and Linux. See [here](https://github.com/thaliproject/Tor_Onion_Proxy_Library).
