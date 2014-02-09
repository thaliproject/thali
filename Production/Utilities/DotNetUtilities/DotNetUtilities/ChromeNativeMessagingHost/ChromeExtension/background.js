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

//var callBackObjects = {};

// TODO: THIS IS HORRIFICALLY INSECURE AND WORSE, JUST BROKEN, IF TWO AUTHORIZED PAGES RUN AT ONCE THEIR IDS WILL COLLIDE!
// I suspect that https://developer.mozilla.org/en-US/docs/Web/API/window.crypto.getRandomValues, which apparently
// is supported by Chrome can get us out of this by creating a cryptographically secure port ID. Note that the
// attack isn't as clear as one might think. For example, let's say that page B knows that page A will use a particular
// port ID. How useful is that? Can it open a port with the same ID? You would hope that is illegal. If it jumps
// ahead of line of page A then page A's request to use that ID should fail. We need to test all of this. If we
// are very lucky all we might need to be secure is not crypto but just some retry logic in case there is a port ID
// collision. More research is needed.

var contentPorts = {};

var nativePort = chrome.runtime.connectNative('com.msopentech.thali.chromebridge');

nativePort.onMessage.addListener(function(msg) {
//    callBackObjects[msg.transactionId](msg);
//    delete callBackObjects[msg.transactionId];
    contentPorts[msg.transactionId].postMessage(msg);
    delete contentPorts[msg.transactionId];
});

//chrome.runtime.onMessage.addListener(
//    function(request, sender, sendResponse) {
//        var transactionId = guid();
//        callBackObjects[transactionId] = sendResponse;
//        port.postMessage({ transactionId: transactionId, requestBody: request.requestBody });
//    }
//);

chrome.runtime.onConnect.addListener(function(contentPort) {
    contentPorts[contentPort.name] = contentPort;
    contentPort.onMessage.addListener(function(request) {
        nativePort.postMessage(request);
    });
});
