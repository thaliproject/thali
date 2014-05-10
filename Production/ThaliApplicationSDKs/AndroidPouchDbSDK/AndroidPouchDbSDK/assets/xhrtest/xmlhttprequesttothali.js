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

    // To get Chrome to work just add this somewhere useful. Notice that it shouldn't need to be inside this
    // function since it only deals with objects hanging off window.
//    window.addEventListener("message", function (event) {
//        // It's possible for other windows to post to our window in some circumstances, this check
//        // prevents that attack.
//        if (event.source != window) {
//            return;
//        }
//
//        if (event.data.type && (event.data.type == new ThaliXMLHttpResponseObject().type)) {
//            window[globalCallBackName].receive(event.data);
//        }
//    });
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
    var self = this;
    this._xmlHTTPObjects[transactionId] = xmlHttpRequestObject;
    thaliXMLHttpRequestArguments.transactionId = transactionId;
//    // o get Chrome to work just register your own ThaliBridgeCallOnce function and then have it relay the
      // request body to window.postMessage per below. Don't worry about the success and failure functions, you
      // can safely ignore them.
//    window.postMessage(thaliXMLHttpRequestArguments, "*");
    window.ThaliBridgeCallOnce(
        "ThaliXHR",
        thaliXMLHttpRequestArguments,
        function(response) { self.receive(self.FixMissingErrors(JSON.parse(response))) },
        function(response) { throw "This should never have been called: " + response });
    return transactionId;
};

/**
 * CouchBase Lite doesn't return the same error information that CouchDB Erlang does which is a problem
 * because PouchDB depends on this error data. I have raised this issue with the CouchDB Lite folks
 * but until it's fixed we need to add in some hacks. This code is the start of that hacking. It tries
 * to add in the fields that PouchDB is expecting that CouchBase Lite isn't sending.
 * @param {ThaliXMLHttpResponseObject} xmlHttpResponse
 * @constructor
 */
ThaliXMLHttpRequestManager.prototype.FixMissingErrors = function (xmlHttpResponse) {
    var contentType = xmlHttpResponse.headers["content-type"];
    if (contentType && contentType.toLowerCase() == "application/json") {
        if (xmlHttpResponse.status == 404) { this.SetErrorValue("not_found", xmlHttpResponse); }
        if (xmlHttpResponse.status == 412) { this.SetErrorValue("missing_id", xmlHttpResponse); }
    }
    return xmlHttpResponse;
};

/**
 *
 * @param {string} errorValue
 * @param {ThaliXMLHttpResponseObject} xmlHttpResponse
 * @constructor
 */
ThaliXMLHttpRequestManager.prototype.SetErrorValue = function(errorValue, xmlHttpResponse) {
    var responseBody = JSON.parse(xmlHttpResponse.responseText);
    responseBody.error = errorValue;
    xmlHttpResponse.responseText = JSON.stringify(responseBody);
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
    if (currentObject === undefined || currentObject === null) {
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
 * Retrieve all response headers
 * @returns {String|null}
 */
ThaliXMLHttpRequest.prototype.getAllResponseHeaders = function () {
    if (this.readyState < 2) {
        return null;
    }

    var output = "";
    for(var headerName in this._responseObject.headers) {
        output += headerName + ": " + this._responseObject.headers[headerName] + "\r\n";
    }

    return output;
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

ThaliXMLHttpRequest.httpKey = "httpkey";

/**
 * A hack for creating httpkey URIs with no public key, this function goes away when the real security infrastructure
 * shows up.
 * @param {string} host
 * @param {int} port
 * @returns {string}
 */
var _CreateBogusHttpKey = function(host, port) {
    return "httpkey://" + host + ":" + port + "/rsapublickey:0.0/";
};

var _SetUpXhrForProvisioning = function(callback) {
    var thaliRequestManager = new window.ThaliXMLHttpRequestManager(guid());
    var xhr = new window.ThaliXMLHttpRequest(thaliRequestManager);
    xhr.onreadystatechange = function () {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                callback(null, xhr.responseText);
            } else {
                callback(xhr, null);
            }
        }
    };
    return xhr;
};

/**
 * Callback used for provision methods
 * @callback thaliProvisionCallback
 * @param {ThaliXMLHttpRequest} Error
 * @param {string} - Success The fully qualified httpkey URI
 */

/**
 * Looks up the server key associated with the submitted host and port and then provisions the local client's
 * public key into that server's principal database
 * @param {String} host
 * @param {int} port
 * @param {thaliProvisionCallback} callback - If successful will get passed the fully qualified httpkey URL for the identified hub
 * @returns {void}
 */
ThaliXMLHttpRequest.ProvisionClientToHub = function(host, port, callback) {
    var xhr = _SetUpXhrForProvisioning(callback);
    var localHubUrl = _CreateBogusHttpKey(host, port);
    xhr.open("ThaliProvisionLocalClientToHub", localHubUrl);
    xhr.send();
};

/**
 * Provisions the hub at the specified remote host and port with the key used by hub identified by the localHttpUrlKey
 * @param {string} localHttpUrlKey - This is a fully qualified httpkey URI. Typically returned from a call to
 *                                  ProvisionClientToHub
 * @param {string} remoteHubHost
 * @param {int} remoteHubPort
 * @param {thaliProvisionCallback} callback - If successful will get passed the fully qualified httpkey URL for the remote hub
 * @returns {void}
 */
ThaliXMLHttpRequest.ProvisionHubToHub = function(localHttpUrlKey, remoteHubHost, remoteHubPort, callback) {
    var xhr = _SetUpXhrForProvisioning(callback);
    var remoteHubUrl = _CreateBogusHttpKey(remoteHubHost, remoteHubPort);
    xhr.open("ThaliProvisionRemote", remoteHubUrl);
    xhr.send(localHttpUrlKey);
};

// s4 and guid taken from http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
}

function guid() {
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

/**
 * Creates a new adapter for PouchDB by wrapping the existing http.js adapter and having it talk to our
 * custom XMLHTTPRequest implementation.
 * @param opts
 * @param callback
 * @returns {*}
 * @constructor
 */
function HttpKeyPouch(opts, callback) {
    var optsAndCallback = HttpKeyPouch._SetThaliOpts(opts, callback);
    var self = this;
    // It turns out that PouchDB depends on a custom 'this' object to access the context with the api object
    // so when creating a new instance of the http adapter we have to remember to use 'call' and provide the
    // self object that was passed to us by the adapter factory.
    return window.PouchDB.adapters.http.call(self, optsAndCallback.opts, optsAndCallback.callback);
}

/**
 * Required to be exposed by any adapter
 * @returns {*}
 */
HttpKeyPouch.valid = function() {
    return window.PouchDB.adapters.http.valid();
};

/**
 * Required to be exposed by any adapter
 * @param name
 * @param opts
 * @param callback
 * @returns {*}
 */
HttpKeyPouch.destroy = function (name, opts, callback) {
    var optsAndCallback = HttpKeyPouch._SetThaliOpts(opts, callback);
    var hiddenPouch = new PouchDB(name, optsAndCallback.opts, optsAndCallback.callback);
    return hiddenPouch.destroy();
};

/**
 * Overrides the default getHost to make sure that we parse httpkey URLs correctly
 * @param name
 * @param opts
 * @returns {{}}
 */
HttpKeyPouch.getHost = function(name, opts) {
    var urlSeparator = "://";

    if (name.slice(0, ThaliXMLHttpRequest.httpKey.length + urlSeparator.length) != ThaliXMLHttpRequest.httpKey + urlSeparator) {
        throw "Huh?!?! How did this happen? Expected httpkey URL but got " + name;
    }

    var parts = name.split('/');

    // Figuring out what properties to set was mostly trial and error
    var uri = {};
    uri.remote = true;
    uri.protocol = ThaliXMLHttpRequest.httpKey;
    // Used by replicateOnServer in HTTP adapter to see if two URLs of the same protocol are for the same Couch Server
    // To make such a comparison for httpkey we need both the authority and key
    uri.authority = parts[2] + "/" + parts[3];
    uri.source = name;
    var hostAndPort = parts[2].split(':');
    uri.host = hostAndPort[0];
    uri.port = hostAndPort[1];
    uri.db = parts[4];
    uri.path = parts[3];
    return uri;
};

HttpKeyPouch._SetThaliOpts = function(opts, callback) {
    opts = opts || {};
    if (typeof opts === 'function') {
        callback = opts;
        opts = {};
    }

    opts.getHost = HttpKeyPouch.getHost;

    opts.ajax = opts.ajax ? opts.ajax : {};

    var thaliRequestManager = new window.ThaliXMLHttpRequestManager(guid());

    // Takes over xhr so we can handle httpkey requests ourselves
    opts.ajax.xhr = function() { return new window.ThaliXMLHttpRequest(thaliRequestManager) };
    // TODO: Fix per https://thali.codeplex.com/workitem/19
    opts.ajax.timeout = 0;

    return { opts: opts, callback: callback };
};

// Register httpkey as a handler, this only works because the HttpPouch handler just cares if the URL starts
// with http.
window.PouchDB.adapter(ThaliXMLHttpRequest.httpKey, HttpKeyPouch);