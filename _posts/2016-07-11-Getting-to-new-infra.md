---
layout: post
title: Getting to new infra
---
## How we got here
In December of 2015 we pushed the last major update to our master branch in our main repro, ThaliCordova_Plugin. This was a proof of concept code base that was just intended to show that the basic technologies could work together. It wasn't anything anyone was ever supposed to ship.

After December we started to work on what would become the "New Infra" milestone which would be a complete re-write of the Node.js code and a substantial re-write of all the native code. The goal wasn't to finish everything needed to ship a real product, but rather, that anything we did ship would be product quality and that we would ship enough to do real demos on top of and gett someoe most of the way to having a shipping productt.

"New Infra" was supposed to ship at the end of March/early April 2016. It's now July 2016 and "New Infra" still hasn't shipped! 

What happened was mainly that JXcore went out of business. This heavily impacted our work in multiple ways. First, it means we no longer had any way to fix bugs or even officially integrate changes like pre-shared keys that we needed. So we (really Shawn Cicoria) had to figure out how to recreate the JXcore build environment and get out new binaries. Second, it meant our continuous integration environment (which was also being run by JXcore) went down. Thankfully Oguz Bastemur, former CTO and now Microsoft employee, of JXcore literally drove the equipment over to our partner's, Rockwell Automation's, facilities in Poland (Oguz lived a few hours away) and we got the CI set up there.

But we ran into a whole bunch of problems that kept CI non-functional for quite some time. Rockwell didn't just host the CI, they also lent us two engineers part time, Jarosław Słota and Marek Czyz, to help fix things. But even with that it took a lot of time, mostly thanks to Ville Rantala's outstanding efforts to get things sorta going again. The political ramifications of JXcore's demise also ended up eating up a huge amount of my time. The end result is that out of a group of roughly 5 people, 2 were still on the work they were supposed to be on.

So yeah, we didn't exactly make the "New Infra" March/April dates but we actually came amazingly close. 

Unfortunately at the end of March the majority of the folks at Microsoft working on Thali had to go to other assignments. This was always expected. Thali is an incubation project that is using resources from the Microsoft Partner Catalyst Team and all of our engagements are time boxed. So when March ended most of our folks had to do other things. Eventually this really just left myself, Jarek and Marek. We were also recently joined by a Rockwell intern, Marcin Lesniczek.

Since early April, Jarek and Marek focused primarily on building a test suite for our Android code which literally had no tests at all. This is a lot harder than it sounds. Not only did they have to write a ton of tests but they also had to figure out how to get those tests to run in our CI environment. They have done an awesome job and we now have vastly better test coverage.

I ended up spending most of my time since April on a bunch of overhead/political stuff (read: welcome to a large corporation, giving presentations at conferences, meeting with potential customers, etc.) as well as some actual coding. Working with Tomi Paananen, who was able to give us some time before he had to peel off, I helped to get a bunch of Android bugs cleaned up and I finished about 98% of the Node.js stack with great test coverage.

## Where's the code?

https://github.com/thaliproject/CI - This is our CI code that we use to run our CI environment. https://pbs.twimg.com/media/CVf24TIU8AA0oQs.jpg:large is a picture of the CI environment when it was at Nubisa. I know Rockwell has a picture somewhere of the modern version. But what you are seeing is a whole bunch of iOS and Android devices that are connected to raspberry PIs that are then connected to a Mac which is listening for events from GitHub. When we get a PR the Mac does a clone, a build, runs the desktop tests and if they pass then it does an iOS and Android build which it then pushes out to the Raspberry PIs who load them onto the phones. The code to manage all of this is in this repro. Right now we are tweaking it a bit to let us run ThaliCordova_Plugin_Bluetooth tests separately from ThaliCordova_Plugin tests. All the code is in the master branch.

https://github.com/thaliproject/Thali_CordovaPlugin_BtLibrary - This is the bulk of our Android code. We have continued to develop on the master branch. This code handles using the Wifi, Bluetooth and BLE radios.

https://github.com/thaliproject/Thali_CordovaPlugin - This is our main project. This contains all the iOS code, a chunk of the Android code and all of our Node.js code. All the new code lives in the vNext branch. But see the next section for a lot more details of what's going on here.

https://github.com/thaliproject/jxbuild - This is the project where all the code we use to build and host JXcore releases live.

## What do we need to do in order to release New Infra?

You can head over to https://huboard.com/thaliproject/thali#/?milestone=%5B%22New%20Infra%22%5D and check out all the items that are needed to finish up New Infra. But let's walk through them.

720 - This contains a bunch of fixes to the Android layer. But unfortunately Tomi just doesn't have time to finish it. Most of the issues are pretty minor but the issue in SocketThreadBase is a much bigger deal than it looks like. So we need to finish this PR out and get it checked in.

417/740 - 417 is the issue to add the replication layer and 740 is the PR that actually does it. The good news is that 740 is code and test complete and passes on my machine. But there are multiple layers of bad news we have to dig through. First off, there are several bugs in PouchDB and Express-PouchDB that will keep the tests from passing (they pass on my machine because I'm using custom builds of both PouchDB and Express-PouchDB). So we either have to wait for new releases of both projects before we can actually get this PR to pass or we have to install our own versions. Installing our own versions isn't actually that hard so we should probably do that. This leads to our second problem, I haven't been running these tests on either Android or iOS. Instead I have only been testing on desktop. The reason for this is that I felt it was better to get the code finished and then fix bugs on Android/iOS then get stuck waiting for the issues there to be resolved (see below). Third off, this PR actually already includes 720. So we really can't check in 740 until 720 is finished up.

507 - This is just some top level code to make it easy to start and stop Thali. It's no big deal. But it would be a good intro item for someone trying to learn Thali's code base top down to take on.

721 - This is one of the bigger open issues from 720 above. It's in the Android code and it really does need to be fixed.

718 - This is just a bug we need to see if it still repro's on Android when we get 740 passing on its Android tests.

719 - This actually needs to be fixed because the code we commented out to get this passing is, I believe, actually doing important stuff.

621 - When we started 'New Infra' we wrote the code 'in place' with the old code (bad idea, but oh well). So what we did is create a new directory called 'NextGeneration' and stuck all the new code there. This work item is to finally 'bust out' the new code from the sub-directory and move it to the top level directory while simultaneously deleting a bunch of old code.

4 - Salti is a library we wrote and use to authorize connections to Thali and the sample is currently broken. It needs to be fixed.

716 - We never actually put Salti support into Thali. We need to fix that. It's not nearly as scary as you might think. :)

361 - Without identity exchange there is no way to actually connect two Thali devices without some magical backdoor key exchange mechanism. The good news is that identity exchange really works with full testing. The bad news is that the code works on an older version of Thali (the master release). So we need to upgrade it to the new infrastructure.

The rest of the 'new infra' bugs are all related to iOS.


## What happens after New Infra?

Getting off Bluetooth and into BLE
Handling illities
