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

// This javascript is to be loaded by a web page that wants to us the Thali extension/native host. But this
// code is intended to run in the web page's thread.

// This code talks to contentscript.js via window.postMessage. This works because although both this code
// and contentscript.js run on different threads and in different contexts they share certain objects,
// specifically window. The downside to this approach is that postMessage sends a general message out
// to *any* listener on the window. So we need to be able to tell who send a particular message. The
// way we do this is via the type value in the posted message.

// N.B. It would have been nice to use cross-extension messaging to connect this code directly to
// background.js and completely skip contentscript.js since it does literally nothing useful for
// our scenario. But for testing and other off line fun I need to be able to load pages from
// file:// and it's not clear to me what the permission model is for cross-extension messaging
// with files. There is a long history of files being treated oddly and the rules tend to change.
// So I decided not to use the cross-extension mechanism and instead relay through contentscript.js.

/**
 * The set of values that the native host will look for in a request
 * @constructor
 */
function ThaliXMLHttpRequestObject() {
    this.type = "REQUEST_XMLHTTP";
    this.transactionId = "";
    this.method = "";
    this.url = "";
    this.headers = {};
    this.requestText = "";
}

/**
 * This set of values the native host will return
 * @constructor
 */
function ThaliXMLHttpResponseObject() {
    this.type = "RESPONSE_XMLHTTP";
    this.transactionId = "";
    this.status = "";
    this.headers = {};
    this.responseText = "";
}

/**
 * Because of ThaliXMLHttp's asynchronous nature we need a manager in Javascript that can make sure responses to
 * requests get routed the right way. That's the job of this class.
 * @param {String} globalCallBackName - A variable (that must be undefined) on the window object that response will be routed to.
 * @constructor
 * @public
 */
function ThaliXMLHttpRequestManager(globalCallBackName) {
    this._globalCallBackName = globalCallBackName;
    this._xmlHTTPObjects = {};
    this._transactionIdSuffix = 0;

    if (typeof window[globalCallBackName] !== 'undefined') {
        throw "The globalCallBackName has already been defined so we have to assume someone else is using it.";
    }
    window[globalCallBackName] = this;

    window.addEventListener("message", function (event) {
        // It's possible for other windows to post to our window in some circumstances, this check
        // prevents that attack.
        if (event.source != window) {
            return;
        }

        if (event.data.type && (event.data.type == new ThaliXMLHttpResponseObject().type)) {
            window[globalCallBackName].receive(event.data);
        }
    });
}

/**
 * Calls across to Java to make the request
 * @param {ThaliXMLHttpRequest} xmlHttpRequestObject - The object to be called back when the response shows up
 * @param {ThaliXMLHttpRequestObject} thaliXMLHttpRequestArguments
 * @returns {number} - This is the transaction ID used to index the request, it will be used to find the right object when the
 * response shows up.
 * @public
 */
ThaliXMLHttpRequestManager.prototype.send = function (xmlHttpRequestObject, thaliXMLHttpRequestArguments) {
    this._transactionIdSuffix += 1;
    var transactionId = this._globalCallBackName + this._transactionIdSuffix;
    this._xmlHTTPObjects[transactionId] = xmlHttpRequestObject;
    thaliXMLHttpRequestArguments.transactionId = transactionId;
    // TODO: We MUST figure out how to tighten up the "*" below
    window.postMessage(thaliXMLHttpRequestArguments, "*");
    return transactionId;
};

/**
 * Called by the local event listener to transmit a message from contentscript.js to return a response.
 * This function will look up the right handler object by transactionId
 * and call its _receiveResponse method.
 * @param {ThaliXMLHttpResponseObject} responseObject
 * @public
 */
ThaliXMLHttpRequestManager.prototype.receive = function (responseObject) {
    var currentObject = this._xmlHTTPObjects[responseObject.transactionId];
    if (currentObject === null) {
        return;
    }
    currentObject._receiveResponse(responseObject);
    delete this._xmlHTTPObjects[responseObject.transactionId];
};

/**
 * Abort the call associated with the transactionId
 * @param {Number} transactionId
 * @public
 */
ThaliXMLHttpRequestManager.prototype.abort = function (transactionId) {
    delete this._xmlHTTPObjects[transactionId];
};

/**
 * A 'mostly' drop in replacement for XMLHTTPRequest. The goal is that the only different behavior is that the
 * constructor needs a handler object (mostly for flexibility, in truth we could redesign so there is just one
 * global handler) and that we accept extra options in order to support SSL Mutual Auth.
 * @param {ThaliXMLHttpRequestManager} thaliXmlHttpRequestManager
 * @constructor
 */
function ThaliXMLHttpRequest(thaliXmlHttpRequestManager) {
    this._readyState = 0;
    this._requestObject = new window.ThaliXMLHttpRequestObject();
    this._responseObject = new window.ThaliXMLHttpResponseObject();
    this._responseType = "";
    this._onreadystatechange = null;
    this._thaliXmlHttpRequestManager = thaliXmlHttpRequestManager;
    this._thaliGlobalXMLHttpRequestHandlerTransactionId = null;
    this._withCredentials = false;

    Object.defineProperties(this, {
        "responseType": {
            "get": function () {
                return this._responseType;
            },
            "set": function (newResponseType) {
                if (newResponseType !== "") {
                    throw "Sorry, we only support string response types which are represented as an empty string";
                }
            }
        },
        "withCredentials": {
            "get": function () {
                return this._withCredentials;
            },
            "set": function (newValue) {
                // TODO: For now we just swallow this since PouchDB sets this to true but we really should throw when this is true since
                // we have no intention of supporting cookies as they are a huge security hole.
                return this._withCredentials;
            }
        },
        "onreadystatechange": {
            "get": function () {
                return this._onreadystatechange;
            },
            "set": function (newValue) {
                if (newValue !== null && typeof newValue !== 'function') {
                    throw "Sorry, only functions or null can be submitted.";
                }

                this._onreadystatechange = newValue;
            }
        },
        "readyState": {
            "get": function () {
                return this._readyState;
            }
        },
        "status": {
            "get": function () {
                if (this.readyState < 2) {
                    throw "status isn't available until we get to at least readystate 2";
                }

                return this._responseObject.status;
            }
        },
        "responseText": {
            "get": function () {
                if (this.readyState < 4) {
                    return null;
                }

                return this._responseObject.responseText;
            }
        }
    });
}

/**
 * Sets the ready state variable and calls the onreadystatechange handler if any.
 * @param {Number} newValue
 * @private
 */
ThaliXMLHttpRequest.prototype._setReadyState = function (newValue) {
    this._readyState = newValue;
    if (this._onreadystatechange !== null) {
        this._onreadystatechange();
    }
};

/**
 * Callback used by the ThaliXMLHttpRequestManager to return responses.
 * @param {ThaliXMLHttpResponseObject} responseObject
 * @private
 */
ThaliXMLHttpRequest.prototype._receiveResponse = function (responseObject) {
    this._thaliGlobalXMLHttpRequestHandlerTransactionId = null;
    this._responseObject = responseObject;
    this._setReadyState(4);
};

/**
 * Establish a connection
 * @param {String} method - HTTP request method, make sure to use proper casing as HTTP methods are case sensitive
 * @param {String} url - URL request will be sent to
 */
ThaliXMLHttpRequest.prototype.open = function (method, url) {
    if (this._readyState !== 0) {
        this.abort();
    }

    this._requestObject.method = method;
    this._requestObject.url = url;
    this._setReadyState(1);
};

// pouchdb-1.1.10 sets onprogress, but none of these are actually implemented.
ThaliXMLHttpRequest.prototype.upload = {
    onloadstart: null,
    onprogress: null,
    onabort: null,
    onerror: null,
    onload: null,
    ontimeout: null,
    onloadend: null
};

/**
 * Add a HTTP request header. Note that if a header with the specified name has already been set then the value
 * will be appended after a comma.
 * @param {String} header
 * @param {String} value
 */
ThaliXMLHttpRequest.prototype.setRequestHeader = function (header, value) {
    if (this.readyState !== 1) {
        throw "Method can only be called after open and before send";
    }

    var currentHeaderValue = this._requestObject.headers[header];

    this._requestObject.headers[header] = currentHeaderValue === undefined ? value : currentHeaderValue + "," + value;
};

/**
 * Abort a request.
 */
ThaliXMLHttpRequest.prototype.abort = function () {
    if (this._thaliGlobalXMLHttpRequestHandlerTransactionId !== null) {
        this._thaliXmlHttpRequestManager.abort(this._thaliGlobalXMLHttpRequestHandlerTransactionId);
    }

    this._requestObject = new ThaliXMLHttpRequestObject();

    this._setReadyState(0);
};

/**
 * Retrieve the value of a HTTP response header
 * @param {String} header
 * @returns {String|null}
 */
ThaliXMLHttpRequest.prototype.getResponseHeader = function (header) {
    if (this.readyState < 2) {
        return null;
    }

    return this._responseObject.headers[header];
};

/**
 * Send a request, make sure to have called open first. The request body is whatever value, if any, that is put in data.
 * @param {String|null} data
 */
ThaliXMLHttpRequest.prototype.send = function (data) {
    if (this.readyState !== 1) {
        throw "You must have called open and not previously called send.";
    }

    this._requestObject.requestText = (typeof data === 'undefined' || data === null) ? "" : data;

    this._thaliGlobalXMLHttpRequestHandlerTransactionId = this._thaliXmlHttpRequestManager.send(this, this._requestObject);
};

// s4 and guid taken from http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
};

function guid() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}


window.ThaliHolderForOriginalXMLHttpRequestObject = window.XMLHttpRequest;
window.ThaliXMLHTTPRequestManager = new window.ThaliXMLHttpRequestManager(guid());

window.XMLHttpRequest = function () {
    return new window.ThaliXMLHttpRequest(window.ThaliXMLHTTPRequestManager);
};

