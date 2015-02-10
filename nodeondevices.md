---
title: Node on Devices
layout: default
---

# Scenarios for Node.js on Android, iOS and WinRT
I believe that node.js should run on Android, iOS and WinRT. I then want to wrap node.js on those platforms in Cordova to make it easy to program and package. See my [packaged apps](http://www.goland.org/html6packagedapps/) for details. This article gives my motivations for why I think this is interesting beyond just the Thali project.

## Internet Of Things (IoT)
So you want to write an app that runs on mobile platforms and can interact with IoT. Great, because Node.js is an awesome way to handle the asynchronous nature of IoT messaging in a completely portable way. Our thinking is to build a node.js library that abstracts the details of the IoT transport, e.g. bluetooth, bluetooth LE, wi-fi, wi-fi direct, nfc, sonic, alljoyn, mDNS, SSDP, etc. The library will handle discovery, announcements, connection handling, etc. regardless of the device's underlying capability. And yes, of course we can integrate in the mobile platform's native cloud notification capabilities as well! This is a great solution for everything from beacons (both interacting with and being one) to making the mobile device into a hub to device control scenarios like alljoyn. 

## Local Cloud
Does your app depend on the cloud? Do you want it to run even if there is no good Internet connection? Heck, do you just want your app to have better performance? How about lowering costs by letting the user pay for the electricity and bandwidth of providing the service? If any of this applies to you, have you considered moving some of your cloud logic down to the device? By hosting your logic on the device you can respond to requests faster and work when offline. It's amazing how many cloud services can be run on a user's device if you cache user relevant data on the device. Node.js makes this particularly easy because you can standardize your logic in Node.js and then run the same code both on the device and in the cloud. Throw in PouchDB and you even have an off-the-shelf standard solution for synching data to/from the device and the cloud.

## Low latency, high availability and security
Does your device need a super low latency connection to other devices in the area? For example, are you enabling multi-user games so two friends in the same room can play against each other at maximum speed? Do your devices need to work together come hell or high water? For example are you connecting manufacturing, monitoring, healthcare devices and need to make sure they can talk regardless of Internet or cellular connectivity? Do you need to enable data transfer with very little possibility of observation but still with full authentication and authorization? For example, are you building an Enterprise collaberation app? If so then node.js with the IoT library mentioned above can help solve your challenges in a platform independent way. We actually want to take this one step further and enable full mesh capabilities as well.

# Use Cases
I talk with lots of folks and some of them let me put their use cases here. I am using this as a use case dump for right now.

Company | Description | Node.js Scenario | Notes
--------|-------------|------------------|-------
[Thali](http://www.thaliproject.org) | Enable Web 3.0 by creating a peer to peer Web | Needs a listener so that updates for photos, status, calendars, etc. can be pushed out efficiently. | Although chat and a few other 'real time' features can be used on platforms without a service model, the service model is really key.
[Novisecurity](http://www.novisecurity.com/#home) | Provide a base station and sensors for home security. Use the cloud to provide notifications for alarm events to, amongst other things, user’s phones. Their current phone client is written in Cordova. | A user is home, the alarm system is not armed, but the user wishes to receive a notification when a door or window is opened (to let them know if someone is coming or going in the house). Today this scenario would require routing the notification through the cloud and back down to the user’s phone. With node.js capabilities on the phone the base station could notify the phone directly. Having two ways to notify the user enables the feature to work in homes that have limitations of cell or wi-fi capability. | The scenario is compelling but only if it works on both Android and iOS. The current limitations on iOS’s ability to run background services mean that the node.js listener couldn’t ‘always’ listen and so isn’t that interesting. The feature still has value and in a later stage of development may still be worth doing for Android only but the lack of iOS support does downgrade interest.
[Xim from Microsoft FUSE Labs](http://www.getxim.com/) | Enables screen sharing across devices, even with users who don’t have the app installed. | They would like to be able to perform local discovery via technologies like Bluetooth LE which would be easy from a node.js host. But more importantly they really want both ad-hoc and mesh networking so they can move data point to point instead of constantly having to long haul everything through the cloud. This is also useful in environments where there is no cloud access (think of sharing pictures on the ski slopes). | They aren’t happy that only Android has a good background model but they would use ad-hoc and mesh networking opportunistically. So if it works, great, they want to use it. If it won’t work (because say the iOS Xim app isn’t open) then they’ll try to fall back to the cloud.
Hypothetical airline software company | Our hypothetical airline software company provides in-plane entertainment services such as movies, games, etc. | Running all the in-plane entertainment services on a central server on the plane is taxing in terms of CPU/storage/bandwidth/electricity requirements. Why not let the user's run most of the load on their own devices? Movies can be quickly streamed down and played locally. Games can be played between passengers directly. Everything is still coordinated and monitored by the central system but most of the load is on the user's devices. | This scenario works great with the iOS and WinRT limitations since it only applies when those devices are active.
Hypothetical beacon company | Our hypothetical beacon company wants to provide hardware beacons that users can interact with using their mobile devices. They also want to provide a SDK for third parties to build functionality on top of their beacons. | Node.js is a great solution for their customers who want to use the SDK. Those customers don't want to re-write their beacon related code for every platform they will run on. By providing the SDK using node.js they offer their customers a write once/run everywhere beacon solution. | The WinRT and iOS limitations are not as big a deal for beacons if we can figure out how to activate node.js when we receive a bluetooth LE notification.
Industrial IoT | In industrial scenarios it's very common for there to be neither cell nor wi-fi infrastructure. Job sites and factories are often in areas with little or no cell connectivity. And the cost of wiring up facilities with wi-fi often runs into the millions. | IoT is always about communication but how do you communicate without any infrastructure? The answer is that you have to build ad-hoc networks, preferably meshes. Our Node.js approach provides a common platform to define the ad-hoc networking/mesh logic and then connecting that logic to the device's local radio. | We are finding the limitations around iOS and WinRT to be an issue here. We will need to find a way to allow our code to be 'woken up' when another device comes into the vicinity.