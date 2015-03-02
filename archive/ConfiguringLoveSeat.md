---
title: Configuring LoveSeat
layout: default
---

<dl>
<dt>Repository Location</dt>
<dd><a href="https://github.com/yaronyg/LoveSeat">https://github.com/yaronyg/LoveSeat</a></dd>
<dt>Branch</dt>
<dd>proposal</dd>
</dl>

We currently use LoveSeat as our .net CouchDB client for Thali. Making LoveSeat work with Thali required surgery to their code so, like everything else, we have our own fork of their code. Note that I did contact the LoveSeat folks and they even responded in 2/2014 saying they would follow up with me on the changes I needed but they never did. At some point I'll need to follow up again but right now I have too many other things going on to deal with it.

Since this is our only 'external' .net dependency I haven't set up a local nuget or anything like that. So basically what you have to do is:

1. Open LoveSeat.sln in Visual Studio and build the thing

2. Go to the project you want to use the output in (e.g. DotNetUtilities) and in that project's Visual Studio solution add LoveSeat as a dependency and point it at /LoveSeat/bin/Debug/LoveSeat.dll
