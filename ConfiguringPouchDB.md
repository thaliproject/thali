---
title: Configuring PouchDB
layout: default
---

<dl> 

<dt>Repository Location</dt>
<dd><a href="https://github.com/yaronyg/pouchdb">https://github.com/yaronyg/pouchdb</a></dd>
<dt> Branch</dt>
<dd> master</dd>
</dl>

Because the PouchDB folks are so ridiculously awesome in taking PRs from us and getting them released quickly we rarely have to use this repo for very long. It's just here so that when we need to submit a PR we have a place to put it together and submit it.

The main problem is that PouchDB uses a bunch of sh scripts and those just plain won't work on Windows. I tried to fix this via Cygwin but npm (which is needed to build PouchDB) doesn't work with Cygwin (scroll to the part about cygwin in [[https://www.npmjs.org/doc/README.html here]]) so basically that is that. No Windows for us.

I work around this by running Elementary OS in VirtualBox and do everything from there. And yes this is a colossal pain in the tuchus. You have to install Git, NPM, CouchDB and then clone the above repository in Linux. Just go to [PouchDB's contributor section](https://github.com/pouchdb/pouchdb/blob/master/CONTRIBUTING.md) for details.

Note that everything from npm to CouchDB that came out of apt-get by default on my build of Elementary was ancient and caused failures. So I had to install NPM/Node.JS as explained [here](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager) and CouchDB as defined [here](https://launchpad.net/~couchdb/+archive/stable ).

I also wanted to run WebStorm. I could download the x64 Linux package directly from JetBrains but it needs JDK. Oracle has its own apt-get solution for installing the latest and greatest JDK explained [here](http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html).

Then I have to set up a shared drive (which in VirtualBox is buggy as hell, when I delete files it doesn't realize they are gone) and move the files manually from the build environment to Linux to my mainline development environment in Windows. Sigh....

BTW, to even set up shared folders you have to use Virtualbox/Devices/Shared Folder Settings/+ and create a machine folder that auto-mounts. Note however that the 'auto' will only kick in on the next reboot. The shared folder should be in /media called sf_[the name you gave]. To access it you have to be a member of the vboxsf group which you can usually accomplish via 'sudo usermod -a -G vboxsf joe' where joe is your user name. And then yes, I needed to reboot again.

I am trying to work with the PouchDB folks to get PouchDB building in Windows. They want this to work so I'm hoping we can get it to work. Maintaining one dev environment is more than enough work.
