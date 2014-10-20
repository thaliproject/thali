---
title: Crosswalk and Cordova
layout: default
---

Thali wants to use HTML5 + PouchDB as its premier client development environment. We are taking this approach because we believe it's the easiest way for developers interested in Thali to write apps that will 'run everywhere'. We also want it for our own use to make it easy to write the small bits of UX that Thali itself needs.

Today we handle this by creating our own Cordova style wrapper that can produce an Android KitKat APK (with full Thali support) or a JavaFX Jar. Unfortunately this approach is very limited for several reasons including:

1. We have run into numerous limitations with JavaFX 8.0's webview that make us believe it is not a good platform for us to work on

2. We can only safely use the lowest common denominator APIs across both the Android KitKat and JavaFX 8.0 webview platforms

3. We have no story for Android before KitKat

The two obvious open source contenders for wrapping our HTML5 based apps to make them portable are [Crosswalk](https://crosswalk-project.org/) and [Cordova](http://cordova.apache.org/). So below we walk through some key features of both projects and compare and contrast them.

<dl>

<dt> Project Focus </dt>
<dd> Crosswalk - Supporting Android and Tizen with bleeding edge HTML5 APIs and the absolute best graphics performance.</dd>
<dd> Cordova - Supporting all mobile platforms with whatever APIs the community wants</dd>

<dt> Technology </dt>
<dd> Crosswalk - Has its own version of Chromium that it even installs on Android.</dd>
<dd> Cordova - Uses whatever the native mobile platform supports and will add in extensions via a bridge written in whatever the native language is.</dd>

<dt> Platforms</dt>
<dd> Crosswalk - Only Android and Tizen are officially supported. They have community projects for Windows, Mac and Linux but admit that it's best effort and they don't even have a packaging solution (e.g. they can't generate exes on Windows for example).</dd>

<dd> Cordova - Everything mobile and almost nothing else.</dd>

<dt> Cross Compatibility</dt>
<dd> Crosswalk - They have a version of Cordova that can run on their Android/Tizen code</dd>
<dd> Cordova - What Crosswalk has done is pretty much the most sensible thing to do with Cordova so nothing is expected from them</dd>

<dt> Community</dt>
<dd> Crosswalk - Brand new, not clear how popular it is although they claim a bunch of gaming wins</dd>
<dd> Cordova - Old as the hills, insanely well supported</dd>

<dt>HTML5 Support</dt>
<dd>Crosswalk & Cordova - Both appear to take a polyfill approach. So if you want to use a HTML5 native API then you just use it and both platforms will either get out of the way if the underlying web container supports that HTML5 API or polyfill it if it doesn't. So this means that one can just write a HTML5 app, drop it into either Crosswalk or Cordova and it should 'just work'.</dd>

</dl>

I'm not sure I see a real benefit to Crosswalk over Cordova for Thali. We desperately need desktop support and neither project has it. But in theory if one writes a 'pure' HTML5 app it should 'just work' in a browser on the desktop. Using Crosswalk + Cordova might be interesting if there are specific HTML5 features we want that Cordova doesn't polyfill. But it seems like a big dependency to take just for that. Also such an approach means forever writing off iOS (which Crosswalk that is Chromium based isn't going to support in this lifetime) and probably Windows Mobile.
