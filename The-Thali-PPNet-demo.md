For our first public demo of Thali we chose [PPNet](https://github.com/pixelpark/ppnet), a project that introduces itself on github like so:

<i>"This project is partly funded by the European Union through the [FI-CONTENT2](http://mediafi.org/) project which is part of [FI-PPP](https://www.fi-ppp.eu/)  (It is called 'Social Network Enabler' [there](http://mediafi.org/open-platforms/).)"</i>

The goal of PPNet is described [here](http://mediafi.org/?portfolio=social-network): 

<i>"The Social Network SE is middleware that can be used to create a social network, either temporarily or permanently for a group of users. It is able to provide social interactions in the digital world separate from the major, proprietary social networks (e.g. Facebook) and as such is best suited for intranet- based or extranet-based networking. Users of the Social Network SE keep full control over their data domain."</i>

This aligns well with Thali's goals. We want to enable people to communicate directly and securely, with full control of their data.

#### Setup for the standard PPNet demo

To run the standard PPNet demo follow the instructions on [PPNet's github page](https://github.com/pixelpark/ppnet). PPNet uses [PouchDB](http://pouchdb.com/) to store messages, photos, and "likes" locally, and uses a public CouchDB server to synchronize that data among users. You can also easily adjust it to use your own instance of [CouchDB](http://couchdb.apache.org/).

#### Setup for the Thali PPNet demo

For this first demo we have chosen the simplest possible architecture, and the one that most closely matches the standard PPNet demo. The role that CouchDB plays in the standard demo is here played by the Thali Device Hub (TDH) running on Android. The PPNet client on the Android device talks to the TDH on localhost (127.0.0.1). The PPNet client on the PC talks to the TDH on its local network address (e.g. 192.168.1.130).

![](https://github.com/judell/thali-binaries/blob/master/ppnet-demo-1.jpg)

#### An alternative setup for the Thali PPNet demo

Since there is also a Java implementation of the Thali Device Hub, we could as easily have located the TDH on the Windows PC like so:

[[File:May-22-fig-2.jpg]]

#### What does Thali add in these scenarios?

Security. All communication with the TDH is by way of [http://thali.cloudapp.net/mediawiki/index.php?title=Httpkey_URL_Scheme the HttpKey URL Scheme] and mutual SSL authentication. 

#### What else could Thali add in these scenarios?

##### Multi-master sync=

Although we are using a singleton database to conform to PPNet's current pattern, each device could run its own TDH, and those TDHs could all sync to one another. That decentralized and mesh-like pattern is a more natural way to use Thali, and we plan to show it in a future iteration of the demo.

##### Peer identity

In Thali, every database sync occurs between a pair of mutually authenticated identities known by public keys. In the current demo we programmatically provision those keys for two client/hub pairs. One pair is the Android PPNet client and its local TDH. The other is the Windows PPNet client and its  remote TDH on Android.

PPNet supports both local login, which we are using in the demo, and various OAuth logins. The Thali address book will enable a full peer identity model. The user on the Android device and the user on the Windows PC will exchange public keys, and permit (subsets of) their TDHs to sync. These mechanisms are in development, we plan to illustrate them in a future iteration of the demo.

##### Wide-area peer networking

Ad-hoc networking is one of the core uses we envision for Thali, and the current demo illustrates that pattern. The two devices identify one another as IP addresses on a local network.

But what if the two devices are separated by one or more NATs or firewalls? In that case there are two  problems that need to be solved. First they need to find one another. Second they need to connect through NATs or firewalls.

Thali's solution to both problems is [http://thali.cloudapp.net/mediawiki/index.php?title=Thali_and_Couch#Firewalls_.26_NATs Tor]. A public-key-based Thali identity works nicely with Tor's model of hidden services, which maps between (hashes of) public keys and network sockets. That will enable Thali users (and their devices) to find one other directly. And because a Tor hidden service maintains a persistent connection to the network, Thali peers can listen for and respond to sync requests from other Thali peers.

When Thali's Tor mechanisms are in place we'll illustrate them in a future iteration of the PPNet demo.

#### Running the demo on Windows

If you don't already have Git installed you can get it [here](http://www.git-scm.com/download/win). You can run these commands from Git Bash.

Thali currently uses a custom Chrome extension to enable the browser to communicate with the TDH. If you're starting from scratch with Thali, pick a folder (we'll call it BUILDROOT), go there, and clone Thali:

git clone https://git01.codeplex.com/thali

Once that's done, follow the instructions [http://thali.cloudapp.net/mediawiki/index.php?title=Using_our_Chrome_Extension here] to build and configure the extension. 

Then, go here: BUILDROOT/Production/Utilities/DotNetUtilities/DotNetUtilities/ChromeExtension.

Create a folder called apps.

Clone our fork of PPNet into apps:

git clone https://github.com/judell/ppnet.git

##### Using the Java TDH

If you don't have an Android device or emulator and just want to try the demo standalone on Windows, go back to the [https://thali.codeplex.com/releases/view/122125 releases page], grab the Java TDH, unzip it, navigate to the bin directory, and invoke java.bat. You should see this:

[[File:May-22-fig-3.jpg]]

Now close all Chrome windows, and edit the target of your Chrome shortcut to read like so:

"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --disable-web-security

This makes Chrome let you run the app from a file:// URL. When you restart you should see a pair of notifications:

[[File:May-22-fig-4.jpg]]

In the dialog, click Cancel. You don't want to Disable the extension.

The yellow banner verifies that the --disable-web-security setting is in effect, as it should be.

Finally, add this bookmark:

file:///BUILDROOT/Production/Utilities/DotNetUtilities/DotNetUtilities/ChromeExtension/apps/ppnet/www/boot.html

Note that, as per the instructions for building/configuring the extension, case matters!

Clicking your bookmark should yield:

[[File:May-22-fig-5.jpg]]

In this case, since the Java TDH is on localhost (aka 127.0.0.1) you can just click submit to launch the app.

[[File:May-22-fig-6.jpg]]

##### Using the Android TDH

See instructions below for running the Android TDH and discovering its IP address. Point PPNet at that address.

#### Running the demo on Android

You'll need two APKs. One is the Android TDH, the other is the PPNet app. 

Instructions for building the Android TDH are [http://thali.cloudapp.net/mediawiki/index.php?title=Developer_Handbook#Building_our_Java.2FAndroid_Projects here] and [http://thali.cloudapp.net/mediawiki/index.php?title=BuildingThaliDeviceHub here]. Or you can just download the latest Android TDH from [https://thali.codeplex.com/releases/view/122125 here].

Instructions for building the app, which happens in BUILDROOT/Production/ThaliApplicationSDKs/AndroidPouchDbSDK, are TBD, but you can find the binary [https://github.com/judell/ppnet/blob/master/AndroidPouchDbSDK-debug-unaligned.apk here].

Once you've build or acquired the APKs, install them to your [http://thali.cloudapp.net/mediawiki/index.php?title=Android_Emulator emulator] or device. To find the emulator's IP address, use Alt-F1 and issue the netcfg command. On a phone or tablet, use Settings -> WiFi -> Advanced to reveal the IP address.

Assuming it is 192.168.1.130, issue these adb commands:

adb connect 192.168.1.130
adb install android-debug-unaligned.apk            # the TDH
adb install AndroidPouchDbSDK-debug-unaligned.apk  # the app

On the emulator or device, launch the TDH. It presents a window like that of the Java TDH above. You can dismiss it, the TDH will keep running as background service.

Finally, launch the app. You should see a menu of options:

[[File:May-22-fig-7.jpg]]

For now PPNet is the only active option, so choose it. You should again see:

[[File:May-22-fig-5.jpg]]

Because the app and the TDH are co-located on the Android device, 127.0.0.1 is again the correct choice. Click submit to launch the app. Use the Simple Login, pick any username and address.

In order to have a conversation, you'll to repeat this exercise with another Android device and/or the Windows/Chrome setup shown above. In either case, point the app at the IP address of the Android TDH.

Here's a screenshot of the app showing a conversation between user Jon, on Windows, and Yaron, on Android.

[[File:May-22-fig-8.jpg]]