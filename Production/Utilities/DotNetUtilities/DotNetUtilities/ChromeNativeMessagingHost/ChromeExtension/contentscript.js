/*
 Copyright (c) Microsoft Open Technologies, Inc.
 All Rights Reserved
 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
 MERCHANTABLITY OR NON-INFRINGEMENT.

 See the Apache 2 License for the specific language governing permissions and limitations under the License.
 */

"use strict";

// This code exists solely to act as a bridge between the user's web page and background.js. See
// xmlhttprequesttothali.js for why we choose not to use cross-extension messaging.

// This code communicates to background.js via the extension postMessage mechanism. Originally
// we used chrome.extension.sendMessage which was nice because it has a callback and so managing
// state was easy. But unfortunately there is either a bug or something about sendMessage I don't
// understand that causes the messaging to fail after the first round. In other words when we
// send the second sendMessage with a new callback, the callback won't work when called.
// Things would only start working again when we reloaded the extension. Honestly this
// smells like  I reloaded the extension! I saw some bugs such as https://code.google.com/p/chromium/issues/detail?id=168263 that
// but I can't be sure. The work around is that we switched to using postMessage. Note that ideally
// we would create a single port to talk to background and re-use it but I'm not super trusting of the
// state management in extensions at the moment so instead I create a new port for each request.

var ports = {};

window.addEventListener("message", function(event) {
    if (event.source != window) {
        return;
    }

    if (event.data.type && (event.data.type == "REQUEST_XMLHTTP")) {
//        chrome.extension.sendMessage({
//            type: 'page',
//            requestBody: event.data.requestBody
//        }, function(response) {
//            window.postMessage({ type: "RESPONSE_XMLHTTP", responseBody: response.responseBody }, "*");
//        });

        var portId = event.data.transactionId;
        ports[portId] = chrome.runtime.connect({name: portId});
        ports[portId].onMessage.addListener(function(response) {
            window.postMessage(response, "*");
            delete ports[portId];
        });

        ports[portId].postMessage({
            type: 'page',
            requestBody: event.data.requestBody
        });
    }
});

