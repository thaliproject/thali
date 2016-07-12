---
layout: post
title: Dividing up the work
---
[Previously](http://thaliproject.org/Getting-to-new-infra/) I had talked about the work items needed to get us to 'New Infra' and then to V1. Keep in mind that this leaves off a metric ton of functionality that we want, you can see a big chunk of that list [here](http://thaliproject.org/Stories/) and even that doesn't include key work items like the [Thali Device Hub](http://thaliproject.org/TDHReplicationManager/) and bringing back Tor support.

But for now let's just focus on 'New Infra' and V1.

## New Infra
In terms of 'New Infra' there are a couple of obvious work areas and open issues:

### Android
We need to finish up the remaining testing, fix one remaining ugly bug (see [here](https://github.com/thaliproject/Thali_CordovaPlugin/issues/721)), get everything passing in CI and then decide how we want to deal with devices that aren't working. My guess is that we should go with an approved list (e.g., list out devices we have tested that we know work).

### iOS
There are two pretty fundamental questions we have to answer about iOS.

The first question is - do we focus on finishing the one way stream architecture we have now or do we stop and try to figure out if we need this architecture at all? Read [here](https://github.com/thaliproject/Thali_CordovaPlugin/issues/751) to see the details. But the bottom line is - we can either finish our existing infrastructure which is insanely complex at the node layer or we can hold off on finishing iOS and instead run tests to determine if the problems discussed in the bug were our fault. Because if they were the whole world gets much simpler.

The second question is - When do we deliver tests? The whole point of 'new infra' was to have product quality code. To me that means having a reasonable test suite and right know we have no CI for native iOS tests. We absolutely have to build a CI environment that can run native iOS tests and then we need to write those tests. This is a bunch of work. Do we do this as part of 'new infra' or do we wait for V1?

### Node

In terms of new functionality we really just need the Thali manager (e.g. [Bug 507](https://github.com/thaliproject/Thali_CordovaPlugin/issues/507)) and then update and fix the identity exchange code. To be fair I'm probably the only person on the project right now who cares about identity exchange so I assume that work will have to fall on me. We should also stick in Salti support although we could wait until V1 for that.

## V1
The only new feature in V1 is the quota manager which is a tiny work item. Everything else is bug fixes of various kinds and hardening the code. Well, o.k., with the possible exception of some BLE work in Android. See below for more on that.

### Android
We need to validate that we can run on Nougat and add Nougat devices to CI. We also need to come up with formal criteria for deciding which devices we are going to declare we run on. We should also strengthen our testing of how well we run in the background.

One area that we have to decide if we want to investigate is expanding our use of BLE. Right now we only use BLE to just find other Thali devices and then we move beacons over Bluetooth. This is really expensive in terms of battery and available connections since in many cases we expect to find out that there aren't any beacons for us on the discovered device. It would be much better if we could use BLE to exchange the beacons. They are small and this would reduce the cost of discovery. But by how much? Without the perf work I'll discuss later, we really don't know. 

The reason, btw, that we went away from BLE except for discovery is that we found that the BLE GATT stack on Android was really unreliable. Talking to other people in the Android community they have discovered the same. But we also felt that with a bit of loving we could get the stack to work well enough for our needs. But due to time pressures we decided to simplify things and use Bluetooth for beacons. But what we never investigate was how badly the decision to use Bluetooth for discovery effects battery life, bandwidth, etc.

### iOS
Other than testing on iOS 10 the only remaining work item here would be writing all the test coverage we need for iOS's native code if we don't already do it as part of 'new infra'. I don't think the work to enable Thali apps to continue to run in the background on iOS is worth doing because of the limitations both in terms of time and APIs for iOS's background mode.

### Node
From a feature perspective there is just quota management. Everything else are either security bugs (like securing the connection between the webview and node) or known perf issues.

### Perf testing
We haven't done any real perf testing in a long time and the results of the old tests didn't exactly inspire us. This needs to be one of the main focus areas for V1. We need to cover a bunch of different types of testing as outlined below.

On the good news side a lot of the infrastructure needed to make this happen already exists in CI. The biggest issue I currently foresee is that we don't have CI configured to run more than one test at a time. So unless we fix that or set up two separate CIs (probably easiest, my guess is we will want a dedicated perf rig) we will block everyone else.

I also don't actually expect us to get to all of these tests or even all aspects of these tests anytime soon. Think of this more as a menu we need to choose from.

#### Battery consumption testing 
We have no idea how much battery we eat. In the case of iOS this isn't a big deal because we only run when the app is in the foreground. But with Android this is huge since we run in the background. We also have to test Android on the three different levels of power available for BLE. We have to figure out how much energy discovery eats. We have to figure out how much energy data transfer eats. We need to start with 1 device to 1 device testing but then test other configurations to see what happens. Is battery usage linear as a function of the number of other devices? Or does it plateau because of limitations in the radio stack? What is the plateau? Anyone building on Thali MUST have these numbers or they can't create a peer pool policy that makes any sense.
#### Time to discovery testing
We have no idea how long it takes two phones in range of each other to discover each other. Anecdotally the time seems super short. But we need to quantify it and make sure it is consistent. Nothing will anger users more than standing in front of another user, both having the phone app and nothing happening!
#### Distance to discovery testing
We have never run any meaningful tests to see how far away two phones can be, even if just line of sight, and still discover each other. This is really important on Android because we use BLE for discovery and we can set the strength of the BLE signal. This lets us trade off battery vs distance but right now we have no idea what the trade off is!
#### Bandwidth testing
We did some testing of how much data we could move peer to peer between devices over both iOS and Android and the results were pretty sad. But those tests are out of date. We need to do a variety of tests here. We need to test raw bandwidth (e.g. write native tests that just try to move bulk data across the radios, this should include WiFi!) to give us a baseline. Then we should try to run our replication logic and see just how bad the overhead is. This will require distinguishing between replicating attachments vs JSON records since they have very different behavior and some known (awful) perf issues.
#### Load testing 
One of the best ways to find awful bugs is to just open up a big can of data and let it rip. This would involve taking a device and having it do non-stop discovery and data transfer for at least 12-24 hours and see what breaks.
#### DOS testing
This is fundamentally different than load testing in that we are explicitly testing that the DOS protections work. There are several variants of this testing including testing attacks on discovery, attacks on brute force Bluetooth connections (that will fail PSK), attacks on the network layer of Bluetooth by authorized users and attacks on PouchDB storage by authorized users.
#### Chaos Monkey Testing
In the real world connections are constantly going to go up and down. We need to test how we handle this. This would be a variant of load testing but would intentionally have the test rigs randomly drop connections at various points in the life cycle. E.g. someone being discovered would go away, someone exchanging data would cut the connection, etc.
#### Uptime testing
Apps can run essentially forever. But most software isn't really trustworthy that long. We need to run tests where we try to run the code for extended periods of time and just see if it keeps working right or not. We need to test both constant activity (like the load test) but also more realistic patterns where activity is transient with long dead periods.
