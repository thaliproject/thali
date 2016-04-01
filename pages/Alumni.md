---
title: Thali Alumni
layout: page-fullwidth
permalink: "/Alumni/"
header:
   image_fullwidth: "header.png"
---
The main group at Microsoft that works on Thali uses an agile model where we can pull in resources as we need them. This means we don't have a standing team for Thali. Therefore the purpose of this page is to honor the folks who have contributed to Thali.

[Jon Udell](http://blog.jonudell.net/)  - Thali's first PM, first advocate and first user!

We had what I jokingly called a "summer of code" like event back in 2014 and the following folks spent a few weeks helping Thali out. They worked on our now depricated Java codebase which is why you don't really see their contributions. But it is because of their contributions that we were able to convince our senior management that this Thali thing could work which then led to them wanting Thali on more than just Java which led to our work on Node.js. So in a sense they wrote their own code out of a job.

[Stefan Gordon](http://www.stefangordon.com/) - He single handedly got our Crosswalk based HTML front end working on Android and then put together the NanoHTTPD infrastructure that let us host our content on the desktop in the browser.

[Ivan Judson](http://irjudson.org/) - He wrote our first Javascript/PouchDB based address book, was the first guinea pig for Stefan's work and managed to survive the awful hell of trying to scan QR Codes in Javascript.

[James Spring](http://www.innerdot.com/) - He hacked into both the Javascript and the Java replication code so that we could manage replication between devices in a way that didn't instantly crash. This was a huge deal for our demo.

[Jason Poon](http://www.jasonpoon.ca/) - Jason got Jenkins running in Azure so we could start doing cross platform testing and also worked a bit on getting Jenkins running locally.

[Tim Park](https://twitter.com/timpark) - He wrote the demo that convinced our upper management that someone other than me could actually use Thali! His deliver was a big part of getting us funded!

For just over a month we had the awesome help of [Wenhui Lu](https://twitter.com/ui_lu) who had a really hard job to do that we can't really talk about yet, but he did it well and without complaint. We are greatful!

[Brian Lambert](http://www.softwarenerd.org/) - Brian created the foundation of our iOS code including figuring out how the heck to make BLE centrals work in the background, how to get an iPhone to be both a central and a peripheral at the same time and how to integrate the multipeer connectivity framework. He also was the original designer of the native API we use to communicate from JXCore/Node.js to native code and debugged through a lot of tricky issues in the early days of JXCore's native plugin api on iOS. He still helps out from time to time but now has moved on to lead his own project.

[Sreejumon Purayil](http://sreesharp.com) - Sreejumon helped to create the initial version of the Postcard app that demonstrates what Thali can do and worked on the install and logging infrastructure. Install in particular was a nightmare because we had to get Android, iOS, Cordova and Node all playing well together.

[Srikanth Challa](https://www.linkedin.com/pub/srikanth-challa/10/b47/905) - Srikanth did the work to put in support for HKDF and PKCS12 libraries directly into JXCore thus enabling new capabilities in JXCore's crypto library. He also worked on integrating these capabilities into Thali and updating the Postcard app to be able to use them. He also helped with some early work on implement a prototype of our notification system's crypto using the extended APIs.

[Jukka Silvennoinen](http://www.drjukka.com/) - When Thali was rebooted as a Cordova/Node.js project Jukka was our very first dev. His contributions included but weren't limited to:

* Got Wifi Direct and Bluetooth working well enough to enable us to demo Thali on Android
* Wrote the first version of our JXcore/native layer
* Helped us to eventually figure out that Wifi direct wasn't going to work right on Android
* Did the initial work on figuring out if we could use BLE instead of Wifi direct
* Helped us get the first iteration of our test infrastructure up and running

[Ville Rantala](https://github.com/vjrantal) - Ville joined Thali in 10/2015 and finished his assignment on 4/1/2016 so he spent 6 months working on the project. In that time he was one of the super heros of the Thali project. His super power is that he can do anything. Have an iOS issue? He'll debug and help fix the code. Have an Android issue? No problem, more debugging and fixing code. When we lost the vendor who was hosting our CI environment he almost single handedly brought the environment up again and fixed a mountain of issues that kept devices from working. He reworked our test coordinator code (which manages running our tests in sync across multiple devices) to make it vastly more robust. He wrote the Wifi infrastructure code, the desktop mock of our native code interfaces as well as the Thali Mobile layer which brings WiFi and native together to present a unified interface to the rest of the Thali stack. But beyond the code he wrote he really drove quality across Thali. As the person who made our CI work he was the person who would first find problems and make sure they got filed and fixed. Because of his mastery in so many areas he could go beyond just saying 'there is a problem' to specifying what the exact problem was and in many cases how to fix it or at least how to reliably find it. Dev, test, PM, Ville does it all.

[Juhana Ilari Sakari Koski](http://juhana.cloudapp.net/) - Juhana joined Thali at the end of January 2016 and finished his assignment on the first of April so he was only here for two months but he packed a lot into those two months. He actually started on our Android code fixing some issues with Tomi but then we transitioned him over to Node where he learned Javascript and Node and then preceeded to more or less singlehandedly write our entire Node.js based notification layer.
