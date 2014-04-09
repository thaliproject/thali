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

// The test starts off with the call to Test with the argument Ping. That should return a successful response
// with the value Pong which triggers a call to Test with the argument Ping1. This should return a failed response
// with the value Pong1. This then triggers the final call to Test with the argument Ping2 which shouldn't
// return anything.

var failCallBack = function(responseString) {
    throw "Was not supposed to be called, got: " + responseString;
};

var successPing1 = function(responseString) {
    var responseObject = JSON.parse(responseString);
    if (responseObject != "Pong1") {
        throw "Expecting Pong1! But got " + responseObject;
    }
    // Eventually we'll introduce one way update calls but for now, not.
    window.ThaliBridgeCallOnce("Test","Ping2", failCallBack, failCallBack);
};

var successPing = function(responseString) {
    var responseObject = JSON.parse(responseString);
    if (responseObject != "Pong") {
        throw "Expecting Pong! But got " + responseObject;
    }
    window.ThaliBridgeCallOnce("Test","Ping1", failCallBack, successPing1);
};

window.ThaliBridgeCallOnce("Test","Ping", successPing, failCallBack);