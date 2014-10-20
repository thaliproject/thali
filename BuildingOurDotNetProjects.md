---
title: Building our .NET Projects
layout: default
---

<dl>
<dt> Repository Location</dt>
<dd> https://thali.codeplex.com/ </dd>
<dt> Branch</dt>
<dd> master</dd>
<dt> Project Location</dt>
<dd> Production/Utilities/DotNetUtilities/</dd>
</dl>

The main problem with getting DotNetUtilities to build is that it has a dependency on our custom release of LoveSeat (please see [[Configuring LoveSeat]]) and we can't be sure where you put that depot. So you will need to go to each project that has LoveSeat references and set their path correctly. At some point I really should just put the two dlls we need into a directory in the project and call it a day but if I do that I know that I'll make a mess the next time I fix a bug or add a feature in LoveSeat. The right answer here is to set up a local NuGet server and use that the way we use mavenLocal() but it's just not high enough on my priority list right now.