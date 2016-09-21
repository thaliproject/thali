---
layout: post
title: Getting to new infra
---
## Executive Summary
This post is mostly for some new folks coming onboard the Thali effort. Even they won't make it all the way through. But it explains where our code is and where it needs to go in excruciating detail. The point of which is to help figure out priorities in terms of what we are going to fix.

## How we got here
In December of 2015 we pushed the last major update to our master branch in our main repro, ThaliCordova_Plugin. This was a proof of concept code base that was just intended to show that the basic technologies could work together. It wasn't anything anyone was ever supposed to ship.

After December we started to work on what would become the "New Infra" milestone which would be a complete re-write of the Node.js code and a substantial re-write of all the native code. The goal wasn't to finish everything needed to ship a real product, but rather, that anything we did ship would be product quality and that we would ship enough to do real demos on top of and get someone most of the way to having a shipping product.

"New Infra" was supposed to ship at the end of March/early April 2016. It's now July 2016 and "New Infra" still hasn't shipped!

What happened was mainly that Nubisa went out of business. This heavily impacted our work in multiple ways. First, it means we no longer had any way to fix bugs or even officially integrate changes like pre-shared keys that we needed in JXcore. So we (really Shawn Cicoria) had to figure out how to recreate the JXcore build environment and get out new binaries. Second, it meant our continuous integration environment (which was also being run by JXcore) went down. Thankfully Oguz Bastemur, former CTO of Nubisa and now Microsoft employee, literally drove the equipment over to our partner's, Rockwell Automation's, facilities and we got the CI set up there.

But we ran into a whole bunch of problems that kept CI non-functional for quite some time. Rockwell didn't just host the CI, they also lent us two engineers part time, Jarosław Słota and Marek Czyz, to help fix things. But even with that it took a lot of time, mostly thanks to Ville Rantala's outstanding efforts to get things sorta going again. The political ramifications of JXcore's demise also ended up eating up a huge amount of my time. The end result is that out of a group of roughly 5 people, 2 were still on the work they were supposed to be on.

So yeah, we didn't exactly make the "New Infra" March/April dates but we actually came amazingly close.

Unfortunately at the end of March the majority of the folks at Microsoft working on Thali had to go to other assignments. This was always expected. Thali is an incubation project that is using resources from the Microsoft Partner Catalyst Team and all of our engagements are time boxed. So when March ended most of our folks had to do other things. Eventually this really just left  Jarek, Marek and myself. We were also recently joined by a Rockwell intern, Marcin Lesniczek.

Since early April, Jarek and Marek focused primarily on building a test suite for our Android code which literally had no tests at all. This is a lot harder than it sounds. Not only did they have to write a ton of tests but they also had to figure out how to get those tests to run in our CI environment. They have done an awesome job and we now have vastly better test coverage.

I ended up spending most of my time since April on a bunch of overhead/political stuff (read: welcome to a large corporation, giving presentations at conferences, meeting with potential customers, etc.) as well as some actual coding. Working with Tomi Paananen, who was able to give us some time before he had to peel off, I helped to get a bunch of Android bugs cleaned up and I finished about 98% of the Node.js based code with great test coverage.

## Where's the code?

https://github.com/thaliproject/CI - This is our CI code that we use to run our CI environment. https://pbs.twimg.com/media/CVf24TIU8AA0oQs.jpg:large is a picture of the CI environment when it was at Nubisa. I know Rockwell has a picture somewhere of the modern version. But what you are seeing is a whole bunch of iOS and Android devices that are connected to raspberry PIs that are then connected to a Mac which is listening for events from GitHub. When we get a PR the Mac does a clone, a build, runs the desktop tests and if they pass then it does an iOS and Android build which it then pushes out to the Raspberry PIs who load them onto the phones. The code to manage all of this is in this repro. Right now we are tweaking it a bit to let us run ThaliCordova_Plugin_Bluetooth tests separately from ThaliCordova_Plugin tests. All the code is in the master branch.

https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary - This is the bulk of our Android code. We have continued to develop on the master branch. This code handles using the Wifi, Bluetooth and BLE radios.

https://github.com/thaliproject/Thali_CordovaPlugin - This is our main project. This contains all the iOS code, a chunk of the Android code and all of our Node.js code. All the new code lives in the vNext branch. But see the next section for a lot more details of what's going on here.

https://github.com/thaliproject/jxbuild - This is the project where all the code we use to build and host JXcore releases live.

## What work is left to be done before we release New Infra?

You can head over to https://huboard.com/thaliproject/thali#/?milestone=%5B%22New%20Infra%22%5D and check out all the items that are needed to finish up New Infra. But let's walk through them.

### Node.js layer

[417](https://github.com/thaliproject/Thali_CordovaPlugin/issues/417)/[740](https://github.com/thaliproject/Thali_CordovaPlugin/issues/740) - 417 is the issue to add the replication layer and 740 is the PR that actually does it. The good news is that 740 is code and test complete and passes on my machine. But there are multiple layers of bad news we have to dig through. First off, there are several bugs in PouchDB and Express-PouchDB that will keep the tests from passing (they pass on my machine because I'm using custom builds of both PouchDB and Express-PouchDB). So we either have to wait for new releases of both projects before we can actually get this PR to pass in CI or we have to install our own versions. Installing our own versions isn't actually that hard so we should probably do that. This leads to our second problem, I haven't been running these tests on either Android or iOS. Instead I have only been testing on desktop. The reason for this is that I felt it was better to get the code finished and then fix bugs on Android/iOS then get stuck waiting for the issues there to be resolved (see below). Third off, this PR actually already includes 720. So we really can't check in 740 until 720 is finished up. 720 is discussed in the Android section below.

[507](https://github.com/thaliproject/Thali_CordovaPlugin/issues/507) - This is just some top level code to make it easy to start and stop Thali. It's no big deal. But it would be a good intro item for someone trying to learn Thali's code base top down to take on.

[621](https://github.com/thaliproject/Thali_CordovaPlugin/issues/621) - When we started 'New Infra' we wrote the code 'in place' with the old code (bad idea, but oh well). So what we did is create a new directory called 'NextGeneration' and stuck all the new code there. This work item is to finally 'bust out' the new code from the sub-directory and move it to the top level directory while simultaneously deleting a bunch of old code.

[4](project/salti#4) - Salti is a library we wrote and use to authorize connections to Thali and the sample is currently broken. It needs to be fixed.

[716](https://github.com/thaliproject/Thali_CordovaPlugin/issues/716) - We never actually put Salti support into Thali. We need to fix that. It's not nearly as scary as you might think. :)

[361](https://github.com/thaliproject/Thali_CordovaPlugin/issues/361) - Without identity exchange there is no way to actually connect two Thali devices without some magical backdoor key exchange mechanism. The good news is that identity exchange really works with full testing. The bad news is that the code works on an older version of Thali (the master release). So we need to upgrade it to the new infrastructure.

[383](https://github.com/thaliproject/Thali_CordovaPlugin/issues/383), [133](https://github.com/thaliproject/Thali_CordovaPlugin/issues/133), [136](https://github.com/thaliproject/Thali_CordovaPlugin/issues/136), [137](https://github.com/thaliproject/Thali_CordovaPlugin/issues/137), [219](https://github.com/thaliproject/Thali_CordovaPlugin/issues/219), [158](https://github.com/thaliproject/Thali_CordovaPlugin/issues/158) -Known bugs in identity exchange.

### Android

[720](https://github.com/thaliproject/Thali_CordovaPlugin/issues/720) - This contains a bunch of fixes to the Android layer. But unfortunately Tomi just doesn't have time to finish it. Most of the issues are pretty minor but the issue in SocketThreadBase is a much bigger deal than it looks like but it's covered in 721 below. So we need to finish this PR out and get it checked in.

[721](https://github.com/thaliproject/Thali_CordovaPlugin/issues/721) - This is one of the bigger open issues from 720 above. It's in the Android code and it really does need to be fixed.

[718](https://github.com/thaliproject/Thali_CordovaPlugin/issues/718) - This is just a bug we need to see if it still repro's on Android when we get 740 passing on its Android tests.

[719](https://github.com/thaliproject/Thali_CordovaPlugin/issues/719) - This actually needs to be fixed because the code we commented out to get this passing is, I believe, actually doing important stuff.

### iOS

[646](https://github.com/thaliproject/Thali_CordovaPlugin/issues/646) - This is a PR that was supposed to get iOS working with our Node.js infrastructure. It also submitted a new version of our Mux layer that was supposed to handle some of the peculiarities of iOS (such as only handling one connection at a time). Unfortunately the code didn't work. We have already checked in a fairly massive re-write of the mux layer separately. But that re-write only focused on getting Android working, not iOS. iOS's mux layer is more complicated than Android's. So this PR is going to have to have a lot of surgery before it's ready. To get a sense for the details see the bugs below.

[751](https://github.com/thaliproject/Thali_CordovaPlugin/issues/751) - This is really the heart of the iOS problems. Read the bug for all the details but this is probably the most important question we can answer to get iOS back on its feet.

[652](https://github.com/thaliproject/Thali_CordovaPlugin/issues/652) - If the answer to 751 is 'no, there really is a bug in the multi-peer connectivity framework' then for our sanity the way to fix the Node.js mux layer issues is to first teach the WiFi based Mobile emulator environment to act like the iOS environment.

[358](https://github.com/thaliproject/Thali_CordovaPlugin/issues/358) - With 652 we can fix up the remaining mux layer issues using the WiFi emulator and then test on the actual device. This will radically increase productivity given how long it takes to test/debug on the hardware or even the emulator. Among other things, with 652 we can debug Node.js easily.

[552](https://github.com/thaliproject/Thali_CordovaPlugin/issues/552) - This is really to write the tests needed to prove that 358 is working.

[665](https://github.com/thaliproject/Thali_CordovaPlugin/issues/665) - The named branch was Ville's attempt to address some of the problems with 646 and it has some nasty hangs, we need to resolve them.

[536](https://github.com/thaliproject/Thali_CordovaPlugin/issues/536) - This is a non-compliance in the iOS code with the required node.js behavior. We need to put in a test (if we don't already have one) and fix the bug as described.

[699](https://github.com/thaliproject/Thali_CordovaPlugin/issues/699), [663](https://github.com/thaliproject/Thali_CordovaPlugin/issues/663) - Some pretty straight forward iOS bugs

[637](https://github.com/thaliproject/Thali_CordovaPlugin/issues/637) - We have seen a ton of connection failures in iOS. It's unclear if the problem is the multi-peer connectivity framework or bugs in iOS. We need to investigate.

## What happens after New Infra?

Oh boy, there is a lot of stuff to do once we get New Infra out the door. Everything after New Infra is all about getting to V1. In this case V1 doesn't have any real new features. It's about cleaning up a bunch of known problems and investigating the known unknowns like our battery and network performance. But we can break the work into several sub areas.

### Missing functionality/security/bugs

New Infra actually has the bulk of required functionality. What's left over are mostly security related features. Things that aren't visible to users but necessary to have a secure system.

#### Features

[108](https://github.com/thaliproject/Thali_CordovaPlugin/issues/108) - Need to manage size of PouchDB

[419](https://github.com/thaliproject/Thali_CordovaPlugin/issues/419) - We need the quota manager

#### New Platforms

[754](https://github.com/thaliproject/Thali_CordovaPlugin/issues/754) - Test JXcore on Android Nougat

[755](https://github.com/thaliproject/Thali_CordovaPlugin/issues/755) - Add Android Nougat devices to CI

[756](https://github.com/thaliproject/Thali_CordovaPlugin/issues/756) - Test JXcore on iOS 10

[757](https://github.com/thaliproject/Thali_CordovaPlugin/issues/757) - Add iOS 10 devices to CI

#### Perf Bugs

[151](https://github.com/thaliproject/Thali_CordovaPlugin/issues/151) - Last time we checked if a record was updated, even if its attachment isn't changed, the attachment would be resent! MUST FIX (if still there)!

[42](https://github.com/thaliproject/Thali_CordovaPlugin/issues/42) - Right now we use Bluetooth to do token discovery on Android. This is a battery suck of the first order. What we really need is to move tokens over BLE. But we have run into a lot of issues with BLE on Android. We need to figure those issues out.

[735](https://github.com/thaliproject/Thali_CordovaPlugin/issues/735) - We don't detect when two phones are already in the same state so we'll think they aren't and keep resynching forever, great way to kill battery.

[734](https://github.com/thaliproject/Thali_CordovaPlugin/issues/734) - We don't notify when a peer disappears so we'll keep looking for them (and eating battery) forever.

[730](https://github.com/thaliproject/Thali_CordovaPlugin/issues/730) - There is a bug in PouchDB that keeps it from honoring custom http agents which means we can't share agents for the same remote clients across different operations. This result is that we will put too much load on those agents. Fixing the PouchDB bug isn't a big deal but we will also need to update our test code to recognize that the agent feature now works.

[62](https://github.com/thaliproject/Thali_CordovaPlugin/issues/62) - We need to confirm that we setting Java socket options correctly.

[700](https://github.com/thaliproject/Thali_CordovaPlugin/issues/700) - The way we handle discovery is not efficient, we need to make it easier to figure out when a peer ID comes from a peer we have seen before. The bug has the details.

[697](https://github.com/thaliproject/Thali_CordovaPlugin/issues/697) - There is a setInterval that will kill phone batteries dead. The fix is thankfully fairly straight forward.

[651](https://github.com/thaliproject/Thali_CordovaPlugin/issues/651) - When new peers show up everyone will try to connect to everyone causing lots of wasted radio time. We need a random delay.

[420](https://github.com/thaliproject/Thali_CordovaPlugin/issues/420) - The thaliPeerPoolInterface doesn't currently have support for event emitters from tcpServersManager which means it can't make good decisions about how to prioritize things.

[32](https://github.com/thaliproject/Thali_CordovaPlugin/issues/32) - We might be able to use Android's new Marshmallow scan APIs to save some battery.

[333](https://github.com/thaliproject/Thali_CordovaPlugin/issues/333) - Using NetworkRequest.Builder we could be much smarter about detecting when Bluetooth and Wifi are available.

[379](https://github.com/thaliproject/Thali_CordovaPlugin/issues/379) - We have to make sure we don't eat up too many resources while we are in the background.

[714](https://github.com/thaliproject/Thali_CordovaPlugin/issues/714) - We don't properly timeout connections in Android.

[733](https://github.com/thaliproject/Thali_CordovaPlugin/issues/733) - We don't notify folks upstream when peers disappear which means we will look for them (and waste battery) forever.

#### Stability Bugs

[701](https://github.com/thaliproject/Thali_CordovaPlugin/issues/701) - This is a bug in Leveldown mobile that has caused others to crash. To be fair we haven't seen it ourselves.

[430](https://github.com/thaliproject/Thali_CordovaPlugin/issues/430), [409](https://github.com/thaliproject/Thali_CordovaPlugin/issues/409) - We need to properly get notified when there are life cycle events in Node.js or we will crash iOS and hurt Android.

[448](https://github.com/thaliproject/Thali_CordovaPlugin/issues/448) - It would be a good idea to just completely shut down JXcore every once in a while and restart it, running a node.js server for way too long can have funny effects.

[7](https://github.com/thaliproject/Thali_CordovaPlugin/issues/7)/[42](https://github.com/thaliproject/Thali_CordovaPlugin/issues/42) - We need to figure out if JXcore is properly giving us persistent locations to store user content in.

[229](https://github.com/thaliproject/Thali_CordovaPlugin/issues/229) - Need to review the cipher suite we are using.

[471](https://github.com/thaliproject/Thali_CordovaPlugin/issues/471), [476](https://github.com/thaliproject/Thali_CordovaPlugin/issues/476), [477](https://github.com/thaliproject/Thali_CordovaPlugin/issues/477), [479](https://github.com/thaliproject/Thali_CordovaPlugin/issues/479), [489](https://github.com/thaliproject/Thali_CordovaPlugin/issues/489), [684](https://github.com/thaliproject/Thali_CordovaPlugin/issues/684) - All minor node-ssdp related issues

[407](https://github.com/thaliproject/Thali_CordovaPlugin/issues/407) - Need to make sure Android behaves itself in the background.

[48](https://github.com/thaliproject/Thali_CordovaPlugin/issues/48) - Need to confirm our logging framework for Android.

#### Security

[741](https://github.com/thaliproject/Thali_CordovaPlugin/issues/441) - We are using an old version of OpenSSL in JXcore, we need to upgrade or Google play will throw us out.

[707](https://github.com/thaliproject/Thali_CordovaPlugin/issues/707) - This is a JXcore issue. The version of OpenSSL it uses is so old it doesn't support any of the ECDHE_PSK_* suites so we don't have perfect forward secrecy. We need to update the OpenSSL version to one that supports those suites.

[723](https://github.com/thaliproject/Thali_CordovaPlugin/issues/723) - We don't test that we properly reject bad PSK connections.

[664](https://github.com/thaliproject/Thali_CordovaPlugin/issues/664) - This is related to 723, it involves us testing bad PSK connections specifically in thaliWiFiInfrastructure and thaliMobileNativeWrapper.

[705](https://github.com/thaliproject/Thali_CordovaPlugin/issues/705) - Our HTTP handling has identical security related code in different places (bad, since fixes might not be applied everywhere), doesn't check properly to limit download size and doesn't properly time out connections over WiFi.

[264](https://github.com/thaliproject/Thali_CordovaPlugin/issues/264) - We need professionals to confirm the correctness of the discovery protocol.

[490](https://github.com/thaliproject/Thali_CordovaPlugin/issues/490) - There is an attack possible using SSDP headers, easy to prevent but we need to do the work.

[261](https://github.com/thaliproject/Thali_CordovaPlugin/issues/261) - There is an attack that could be used to expose our admin password, it's pretty easy to protect against.

[499](https://github.com/thaliproject/Thali_CordovaPlugin/issues/499) - We have no protections currently in place for badly behaving but otherwise authorized attackers. This includes everything from making too many network requests to storing too much data.

[444](https://github.com/thaliproject/Thali_CordovaPlugin/issues/444) - A nasty attack where an authorized third party can freeze someone out. Fairly easy to stop though.

[434](https://github.com/thaliproject/Thali_CordovaPlugin/issues/434) - We don't have DOS protection against bad peers

[137](https://github.com/thaliproject/Thali_CordovaPlugin/issues/137) - Another DOS issue.

[53](https://github.com/thaliproject/Thali_CordovaPlugin/issues/53) - We need to delete old logs so we don't overwhelm the device. An attacked could take advantage of this to disable a device.

[52](https://github.com/thaliproject/Thali_CordovaPlugin/issues/52) - We have to make sure our logs don't have sensitive data.

[320](https://github.com/thaliproject/Thali_CordovaPlugin/issues/320) - We need a spec to secure the webview.

[446](https://github.com/thaliproject/Thali_CordovaPlugin/issues/446), [447](https://github.com/thaliproject/Thali_CordovaPlugin/issues/447) - This is related to 320 and defines how we move secrets to the Webview.

[125](https://github.com/thaliproject/Thali_CordovaPlugin/issues/125) - We need to make sure we have only enabled the SSL cipher suites we need.

[140](https://github.com/thaliproject/Thali_CordovaPlugin/issues/140) - If you don't have a threat model then you don't know if you are secure.

[138](https://github.com/thaliproject/Thali_CordovaPlugin/issues/138) - We need our network APIs fuzzed.

#### Android

[265](https://github.com/thaliproject/Thali_CordovaPlugin/issues/265) - We have a problem right now where on some devices we can't set a service type. We can only set manufacturer type. We have to figure out if this is a bug in our code or a problem on those devices and what we want to do about it.

[645](https://github.com/thaliproject/Thali_CordovaPlugin/issues/645) - This is related to 265. We're worried that the work around may suck battery (e.g. searching for both service and manufacturer type).

[66](https://github.com/thaliproject/Thali_CordovaPlugin/issues/66) - The code to check for BLE feature support is scary.

#### Nice to haves

[747](https://github.com/thaliproject/Thali_CordovaPlugin/issues/747) - We really should get PSK into mainline node. The work on this actually underway. We just need to support it.

[269](https://github.com/thaliproject/Thali_CordovaPlugin/issues/269) - Using something like rollup we could turn our huge mess of Node.js files into one massive file. This would help things like iOS perf. But it's not clear how big a win this really is and how much it would hurt to implement it.

[668](https://github.com/thaliproject/Thali_CordovaPlugin/issues/668) - There is a bug in our PSK code that if the callback to check the identity has a Javascript error it will crash everything.

[449](https://github.com/thaliproject/Thali_CordovaPlugin/issues/449) - Right now we don't run in the background at all in iOS. But this could give us a few extra minutes of sync time which is a big deal.

[282](https://github.com/thaliproject/Thali_CordovaPlugin/issues/282) - If androidAftterPrepare in our install script fails the dev doesn't get any notification.

[123](https://github.com/thaliproject/Thali_CordovaPlugin/issues/123) - Best practice is that all crypto operations, like creating keys, should be in a separately spawned process that doesn't have any connections to the network.

[124](https://github.com/thaliproject/Thali_CordovaPlugin/issues/124) - We constantly use our root key for all crypto operations. That is a bad idea. We should be using a chain.

[582](https://github.com/thaliproject/Thali_CordovaPlugin/issues/582) - We don't take into account failed connections when marking a peer as unavailable.

### Illities

Since 'new infra' was meant as product quality proof of concept there is a ton of basic blocking and tackling we didn't handle.

#### Testing

[556](https://github.com/thaliproject/Thali_CordovaPlugin/issues/556) - This is by far the biggest sin we have and probably something that should be moved to new infra. We have zero CI testing or automated test suites for iOS. There is a tiny bit of code coverage but nothing comprehensive. This has got to be fixed at the highest priority. Let me repeat - our iOS native code has effectively ZERO test coverage.

[78](https://github.com/thaliproject/Thali_CordovaPlugin/issues/78) - We have done a ton of work giving code coverage for Android but only for the code in ThaliCordova_Plugin_Bluetooth. We only indirectly test the Java code in ThaliCordova_Plugin via exercising the Java interfaces from Node.js. But that is just asking for problems. We need dedicated Java level tests of the Android code in ThaliCordova_Plugin.

[750](https://github.com/thaliproject/Thali_CordovaPlugin/issues/750) - We have no idea how good our code coverage is in Node, Android or iOS. We really need to find out.

[39](https://github.com/thaliproject/Thali_CordovaPlugin/issues/39) - This is a bug in CI where we don't get any logs. Bad.

[709](https://github.com/thaliproject/Thali_CordovaPlugin/issues/709) - We set related environment variables in random different places, we need to clean that up.

[19](https://github.com/thaliproject/Thali_CordovaPlugin/issues/19) - We need to let the CI run for longer times. Some of our tests take a while.

[554](https://github.com/thaliproject/Thali_CordovaPlugin/issues/554) - We don't even have a way to run iOS unit tests in CI!

[112](https://github.com/thaliproject/Thali_CordovaPlugin/issues/112) - We need a framework to randomly kill connections when we are testing to see how well we do.

[337](https://github.com/thaliproject/Thali_CordovaPlugin/issues/337) - How many connections can we handle on iOS and Android before things become unstable?

[636](https://github.com/thaliproject/Thali_CordovaPlugin/issues/636) - We print the wrong results summary in Unit tests for local devices.

[691](https://github.com/thaliproject/Thali_CordovaPlugin/issues/691) - If there is a failure while we are in teardown the CI system won't see it. That is bad because it has hidden bugs in the past

#### Code Quality

[731](https://github.com/thaliproject/Thali_CordovaPlugin/issues/731) - We have frozen all of our dependencies in config.json because of issues we ran into where minor updates broke things. But this means we don't automatically get bug fixes. So we really must introduce something like greenkeeper to help us manage our dependencies.

[715](https://github.com/thaliproject/Thali_CordovaPlugin/issues/715) - We have some repeated code that needs to be abstracted out.

[712](https://github.com/thaliproject/Thali_CordovaPlugin/issues/712) - PouchDB no longer blocks on Node 0.10 failures during CI. Since JXcore is (more or less) Node 0.10 this is a big deal for us. We have to set up a way to get notified when PouchDB's Travis tests fail on Node 0.10 and investigate.

[667](https://github.com/thaliproject/Thali_CordovaPlugin/issues/667) - At one point our PSK code required that certs be supplied even thought they weren't being used. The bug is fixed but we need to clean up our tests to make sure they don't send the certs anymore.

[609](https://github.com/thaliproject/Thali_CordovaPlugin/issues/609) - Our docs for JXcore's PSK support aren't any good, we need to fix.

[602](https://github.com/thaliproject/Thali_CordovaPlugin/issues/602) - thaliTcpServersManager is a huge mess of repeated code.

[457](https://github.com/thaliproject/Thali_CordovaPlugin/issues/457) - Automate tests of our code to detect when we are running on a device that doesn't have the right BLE hardware.

[253](https://github.com/thaliproject/Thali_CordovaPlugin/issues/253) - Our code docs have a lot of broken @link, we really need to fix.

[265](https://github.com/thaliproject/Thali_CordovaPlugin/issues/265) - Our install.js install file is a beast. We really need to clean up.

[488](https://github.com/thaliproject/Thali_CordovaPlugin/issues/488) - The HKDF code we checked into JXcore turns out to be broken. We either need to fix or remove.

[541](https://github.com/thaliproject/Thali_CordovaPlugin/issues/541) - Our dev instructions are not complete

[547](https://github.com/thaliproject/Thali_CordovaPlugin/issues/547) - Put in a test for Marshmallow related behavior, this is really about adding Marshmallow devices to CI

[647](https://github.com/thaliproject/Thali_CordovaPlugin/issues/647) - We need to re-enable the DOC check linting in CI

#### Stability

[450](https://github.com/thaliproject/Thali_CordovaPlugin/issues/450) - What happens to our data and over all functionality if Android or iOS just straight out kills us?

[408](https://github.com/thaliproject/Thali_CordovaPlugin/issues/408) - We have to confirm that we properly kill things when we go into and out of the background on iOS.

[413](https://github.com/thaliproject/Thali_CordovaPlugin/issues/413) - Need to automate tests for doze and standby in Android on Marshmallow and higher.

#### Measuring performance

[404](https://github.com/thaliproject/Thali_CordovaPlugin/issues/404) - We used to have perf tests but they have been fallow for a long time now. We really need to comprehensively measure perf.

[41](https://github.com/thaliproject/Thali_CordovaPlugin/issues/41) - We need to measure how much slower WiFi is when Bluetooth is running, if it's bad then we will probably need to avoid synching with Bluetooth on Android when there is a foreground app using WiFi or people will hate us for making their phones slow.

[40](https://github.com/thaliproject/Thali_CordovaPlugin/issues/40) - If we turned off WiFi when we aren't connected would that increase Bluetooth bandwidth? (Remember, WiFi and Bluetooth are time division multiplexed across the same antenna)

[37](https://github.com/thaliproject/Thali_CordovaPlugin/issues/37) - We don't know what we don't know about Bluetooth perf, connection limits, etc. Help!

[258](https://github.com/thaliproject/Thali_CordovaPlugin/issues/258) - We've had some issues with connections constantly being dropped in iOS. We need to confirm it actually exists and then fix if necessary.

[367](https://github.com/thaliproject/Thali_CordovaPlugin/issues/367) - Just how much data can our multiplex layer handle in practice? How many connections? How much does it slow things down?

[336](https://github.com/thaliproject/Thali_CordovaPlugin/issues/336) - How do we affect the phone (iOS or Android) when we are going full blast on synch's?

[23](https://github.com/thaliproject/Thali_CordovaPlugin/issues/23) - We need to measure how our code to handle multiple BT handshakes actually works in practice.

[160](https://github.com/thaliproject/Thali_CordovaPlugin/issues/160) - What is our battery performance on iOS?

[752](https://github.com/thaliproject/Thali_CordovaPlugin/issues/752) - What is our battery performance on Android?

[330](https://github.com/thaliproject/Thali_CordovaPlugin/issues/330) - Need to measure our perf over WiFi.

#### Nice to haves

[44](https://github.com/thaliproject/Thali_CordovaPlugin/issues/44) - We really need to get a local NPM cache into our CI environment. We already lost a full day of work when NPM went down. A cache wuld protect us.

[729](https://github.com/thaliproject/Thali_CordovaPlugin/issues/729) - There are various ways that PouchDB can become corrupt. How do we recover?

[15](https://github.com/thaliproject/Thali_CordovaPlugin/issues/15) - We need CI to update itself automatically when we update the CI project. Right now it's manual and we forget.

[349](https://github.com/thaliproject/Thali_CordovaPlugin/issues/349) - We need to clean up install scripts a bit to make it easier for 3rd parties to install us.

[569](https://github.com/thaliproject/Thali_CordovaPlugin/issues/569) - PouchDB now supports SQL Lite. Is it even theoretically a good replacement for LevelDB?

[116](https://github.com/thaliproject/Thali_CordovaPlugin/issues/116) - Life would be better if we could live reload Node.js content on devices for testing purposes.
