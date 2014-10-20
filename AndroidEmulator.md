---
title: Android Emulator
layout: default
---

NOTE: For Windows 8.1 users. Virtual Box and Hyper-V do not seem to get along well with each other. So if you want to use Virtual Box please disable Hyper-V by going to Control Panel->Programs and Features->Turn Windows feature on or off->Uncheck Hyper-V if it's checked and do the reboot dance.
Alternatively you can just run the emulator in Hyper-V! See [http://blog.dot42.com/2013/08/running-android-43-in-hyper-v.html](here).

SECOND NOTE: IF YOU ARE GOING TO USE THIS READ THE SECTION BELOW ON NETWORKING SINCE THIS DOESN'T WORK THE WAY THE ANDROID SDK CLAIMS!

The Android Emulator is so slow that many tests fail on time outs. Yes, I tried the tricks with using CPU acceleration and using Intel based (rather than ARM based) VMs and using the Intel accelerator. None of it made much of a difference. Genymotion offers a faster emulator but it won't work through Remote Desktop because in Remote Desktop only an ancient version of OpenGL (1.1) is supported and Genymotion requires 2.0. Since all my development is done through Remote Desktop that is a show stopper. BlueStacks also doesn't seem to work with Remote Desktop.

1. Following instructions [http://www.android-x86.org/documents/virtualboxhowto](here). I am currently using [http://android-x86.googlecode.com/files/android-x86-4.3-20130725.iso 4.3].
1. I make sure (per the instructions) to put in Type: Linux, Version: Linux 2.6 
1. I set memory to 1024 MB. 
1. I do not add a virtual hard disk. 

The instructions above tell you how to save to disk but I like running a live image since I can easily clean things up. I normally just leave the image running for debugging purposes.

1. The settings are now created but go hit settings again and go to system->motherboard->boot order and disable everything but CD/DVD-ROM. 
1. Contrary to the instructions on the above page I left Video Memory at the default 16 MB.
1. Go to Storage->IDE and click Empty then go over to the CD icon to the right and click it and point it at your ISO. Then click Live CD/DVD.
1. Go to Audio->Audio Controller-> and set it to SoundBlaster 16 (wow, does that bring back memories)
1. Go to Network->Adapter 1->Attached to->Bridged Adapter, Adapter Type->PCnet-Fast III
1. Now hit o.k. and press start
1. Choose VESA mode and wait a second and you will see the ANDROID bootup screen and then you get to answer all the config questions.
1. Go to Machine->Disable Mouse Integration or you won't be able to see your mouse. A big problem I had was getting my mouse back! Yes ctrl+alt 'appeared' to free it but it couldn't do anything. I finally had to use ctrl-alt-end (I'm running in remote desktop) to free it.
1. Look [http://www.android-x86.org/documents/debug-howto](here) in section 1 for details on how to set up debug. But in short, alt-f1, type in netcfg, copy the IP address and alt-f7 to go back. Then issue "adb connect [ip]" at your closest command line. Then go to Intellij Run->Edit Configurations, pick your configuration and choose Target Device->Show chooser dialog. Then when you start a run the dialog will show the emulator you connected via adb connect.
1. If the screen locks then you have to go to machine->ACPI Shutdown, that will 'wake it up' and then you can use the mouse to unlock the lock icon.

Jon Udell pointed [http://www.bobbychanblog.com/2011/07/faster-android-emulator-alternative-using-virtualbox/](here) for a much prettier version of the above instructions. But I have to warn you that the link is out of date, doesn't point to the same release of Android and has some other subtle differences. But I know that being able to see pictures of the dialogs can help some folks. So use, but remember that the instructions above are the ones you must use if you want to replicate our environment.

'''WARNING:''' When shutting down Intellij it will ask you if you want to disconnect Android. If you say yes, it will do what you say and your emulator won't be hooked into ADB. If you say yes then remember to issue the 'adb connect [ip]' command from above to reconnect.

Note: Android 4.0 has some kind of problem that prevents it from properly supporting client certs so don't go there.

#### Networking - PLEASE READ ME!!! 

If you read the Android SDK emulator docs it will talk about how the Android emulator is isolated from the world and you have to do all sorts of weird things to talk to it. None of that applies here. If you set up the VM as described above then your Android device is bridged to the host's network and from both the device and host's perspective it's just another machine with an IP address.

This has numerous implications that you need to be aware of.

1. in theory, it means anyone who has the android device's IP address can navigate to it.

1. it means you don't need to use 'adb forward' to talk to the android device from the host. Instead just use the android device's IP address (the one you got from netcfg in the instructions above) and off you go!

1. it means that if you want to talk from the android device to the host you just use the host's IP address, you can forget all about that 10.0.2.2 stuff you will read about in the Android emulator SDK docs. Now finding your host's IP address can be a tiny bit tricky depending on your setup. On Windows I open a command window on the host and type 'ipconfig'. I typically use ethernet as my primary connection so that is usually the adapter I'm bridged over. For WiFi only folks you will use that address. You will see an adapter called "VirtualBox Host-Only Network", ignore that.
