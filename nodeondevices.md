---
title: Node on Devices
layout: default
---

# Getting funding to get Node.js on Android, iOS and WinRT
I have a number of ideas ranging from [packaged apps](http://www.goland.org/html6packagedapps/) to running Thali on Node.js that require Node.js to be available on Android, iOS and WinRT. My goal is to convince my employer that they want to fund this effort. To do that I need to come in with real customers who would want to use Node.js on these OS's if it was available.

To help identify potential customers for this technology I put together below a list of the characteristics that I believe a customer would have.

## Device Hubs
Any IoT device that runs on Android, iOS or WinRT is usually a candidate. Typically any device that has enough power to run one of those OS's will act as a hub and receive commands, so it needs a listener. Node.js would be an awesome choice. Also devices that coordinate other IoT devices would also be a great example of this scenario.

## Transportable Logic
As more companies use Node.js in the cloud they want to be able to re-use that logic in different places like devices. This can be done with browserfy but then any listener related logic won't work. So if any of the logic is related to receive requests on an Android/iOS/WinRT device then they would be a good candidate.

## High security
In high security communications an iron clad rule is to have as few parties involved in the communication as possible, so typically one wants peer to peer if possible. But for peer to peer to work one needs a listener on each device.

## High latency sensitivity
When multiple local devices need to communicate extremely latency sensitive information. This is typically gaming, security related or precision manufacturing.

## Cloudless Environments
Devices that need to operate in environments that either have no or inconsistent cloud connectivity. Typically the devices are mobile but industrial applications in remote areas or even old factories apply. Typically ad-hoc or mesh networking (the first being a degenerate case of the second) is used.

## Cloud Fail Over
Lots of devices push all of their data to a cloud where it is processed and commands are returned. But robust devices want to keep operating even if cloud connectivity goes away. In that case one could use a local device (such as a phone, laptop, PC, etc.) to act as a local cloud in case the 'real' cloud isn't available.

For example, imagine a refrigerator or coffee maker that normally sends data to and receives commands from the manufacturer's cloud. Users would normally use a local app to connect to the cloud in order to send commands to the device. But if the local Internet is down do we really want the refrigerator or coffee maker to be non-responsive? In that case the phone app could receive data directly from the device and send local commands. When the cloud was accessible again the collected state can be forwarded and normal operations resume.

Note that if the refrigerator or coffee maker is itself a hub then this scenario doesn't apply since the phone would presumably just connect to the device. So this only applies when the device depends on logic external to itself that is normally hosted in the cloud. Usually this applies to small and very limited devices.

## Reducing Cloud Costs
Solutions that require all data, all the time, to transit the cloud can potentially be quite expensive in terms of bandwidth and CPU costs. So enabling local devices to communicate directly can save costs by only forcing the cloud system to deal with high value data.
