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

// This code assumes that the Java Bridge Manager object has already been registered
if (ThaliBridgeManager0 === null) {
    throw "ThaliBridgeManager0 is not defined!";
}

/**
 * Generates what should be a globally unique ID. This was supposed to generate a GUID but the
 * format in its normal string form isn't compatible with Javascript function names which I need to use it for.
 * This is taken from http://byronsalau.com/blog/how-to-create-a-guid-uuid-in-javascript/
 * @returns {string}
 */
window.ThaliCreateUniqueId = function()
{
    return 'xxxxxxxxxxxxxxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
};

window.thali_callback_manager = {};

/**
 * Callback to get result from a ThaliBridgeCall
 * @callback ThaliBridgeCallBack
 * @param {Object} jsonObject Asynchronous response object
 */

/**
 * Calls handlers in Java. All calls are asynchronous. So a call is made to Java and the response will come
 * back via the handlers. The expectation is that the call will return one and only one response (e.g.
 * request/response pattern).
 * @param {String} handlerName Name of handler in Java
 * @param {Object} jsonObject An object we will stringify and pass to handler
 * @param {ThaliBridgeCallBack} successCallBack
 * @param {ThaliBridgeCallBack} errorCallBack
 */
window.ThaliBridgeCallOnce = function (handlerName, jsonObject, successCallBack, errorCallBack) {
    var cleanUpAndRun = function(successName, errorName, callback) {
        return function (jsonString) {
            delete window.thali_callback_manager[successName];
            delete window.thali_callback_manager[errorName];
            callback(jsonString);
        }
    };

    // The name is only included to make debugging slightly easier, maybe.
    var createUniqueCallbackName = function(name) {
        return name + window.ThaliCreateUniqueId();
    };

    var jsonString = JSON.stringify(jsonObject);

    var successName = createUniqueCallbackName("success");
    var errorName = createUniqueCallbackName("error");

    window.thali_callback_manager[successName] = cleanUpAndRun(successName, errorName, successCallBack);
    window.thali_callback_manager[errorName] = cleanUpAndRun(successName, errorName, errorCallBack);

    ThaliBridgeManager0.invokeHandler(handlerName, jsonString, successName, errorName);
};