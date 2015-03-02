---
title: Building the HTML 5 Environment that Thali Needs
layout: default
---

#### Introduction

Thali's ideal vision would be that we had our own browser that we could take to all the major versions of all the major mobile platforms and all the major versions of all the major desktops. In the case of mobile it doesn't seem like this vision is possible. Crosswalk gives us a consistent platform on Android but only for Android 4.x and higher. iOS and Windows Mobile don't seem to have the equivalent of Crosswalk so one really has to use whatever webview is there. We might potentially have better luck with our own stand alone wrapper browser on the desktop, but we have to see how well this works in practice.

If we are going to run on a random assortment of mobile webviews and various versions of desktop browsers then we will need to use polyfills to even things out. Our main challenge is that the polyfill with the widest support on mobile is Cordova. Unfortunately Cordova has no story on the desktop and so we have to use different polyfills there. That is a huge challenge for us as it essentially means we have to write and maintain two different configuration systems. One to handle Cordova and one to handle the desktop. We really want one solution that runs everywhere.

#### The plan

We are going to run in the browser on the desktop and we will use Crosswalk on Android. This means we can only support Android versions 4.x and up. We really want to get to Cordova and eventually we will, but not today.

We use PouchDB as our primary HTML 5 interface to Thali Device Hubs (both local and remote). The PouchDB team has been kind enough to give us the hooks we need to drop in an adapter for the httpkey URL type that will then be able to send requests to our application proxy. So the developer experience is 'submit httpkey URL to pouch and go'.

Unfortunately there isn't any really easy way to get the browser environment to 'do the right thing' in terms of Thali's use of both TLS with mutual auth and self signed certs plus Tor. So we use an external proxy that is run with each application (e.g. each app packages its own unique proxy instance) that actually handles the Thali security part. The design for this proxy is explained [here](https://thali.codeplex.com/discussions/544073#PostContent_1244918).

So the developer experience is write your app in HTML5. Then we will provide a tool (like Cordova) that will package it up for the desktop and for Android. 

On the desktop the packaging step will created an executable that will do several things when run. First, we will deploy our own localhost http server to host the files (since browsers act really oddly around file urls), this will most likely just be NanoHttpd. Second, we will deploy the proxy as described in the previous link including setting up app keys, connecting to the local TDH, etc. Third, we will call the system to load the index.html page in whatever the local browser is and we're off to the races.

In Android the packaging step will actually end up calling Crosswalk which will create the APK for Android. We will include a plugin for Crosswalk that will activate the proxy and the local nanohttpd server. The plugin will also grab the life cycle model for the application so when the app goes through life cycle events we can capture them and do the right thing.

#### Open Issues

##### How does the proxy and NanoHttpd servers get de-activated on the desktop?

How do we know when the user is actually done with the app and so we can safely shut down the NanoHttpd server and the proxy? Do we just leave them running forever? Do we have an 'off' button? That seems dorky. If a user goes to the localhost link and then navigates away (it is a browser after all) it would be nice if when they navigate back the app is still there. But it's not cool to liter their system with a bunch of proxies and NanoHttpd servers either. So we have a life cycle issue. One way I've seen people handle this is that in addition to opening the browser they will also open an app window (e.g. some simple Java window) that represents the app being 'alive' and if they want to kill the app they kill that window which then shuts everything down.

#### Q&A

##### What about Chrome Apps/Packages Apps?

In theory we could just use [Chrome Apps](https://developer.chrome.com/apps/about_apps). They seem to cover all the platforms we care about (more or less) and provide a single development environment. The problem is that we really want a true open source solution run by a real community that doesn't put in all sorts of random restrictions like limiting eval().

##### What about Cordova?

In our dreams we would build the whole product in Cordova and there would be a 'build to HTML 5' option that would just run in the browser. In our really insane dreams we would have 'build to executable' option that would produce a self contained exe/dmg/etc. that would just run on the right flavor of desktop.
