---
layout: post
title: Native module use in NPM appears to be dropping
---
It would appear native modules are becoming even less common than they were before. I had previousy published some heuristics from 12/2014 which estimated that NPM packages that either used node-gyp themselves or depended on a module that used node-gyp accounted for 16% of all packages and around 3% of all NPM downloads. I just re-ran those numbers for 4/2016 and now it looks likes 10% or so of packages either use native code or depend on a package that does somewhere in their dependency tree. And less than 2% of downloads from NPM were for such packages. The less native modules we have, the easier it will be to get Node running on multiple platforms so this is good news. See [here](http://www.goland.org/node-gyp-and-node-js-on-mobile-platforms/) for details.
