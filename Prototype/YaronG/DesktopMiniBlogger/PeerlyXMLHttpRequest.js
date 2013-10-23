/**
 * The set of values that the Java HTTP request handler will look for
 * @constructor
 */
function PeerlyXMLHttpRequestObject() {
    this.method = "";
    this.url = "";
    this.headers = {};
    this.requestText = "";
}

/**
 * This is the structure returned by the Java HTTP request handler
 * @constructor
 */
function PeerlyXMLHttpResponseObject() {
    this.status = "";
    this.headers = {};
    this.responseText = "";
}

/**
 * Because of PeerlyXMLHttp's asynchronous nature we need a manager in Javascript that can make sure responses to
 * requests get routed the right way. That's the job of this class.
 * @param {String} globalCallBackName - A variable (that must be undefined) on the window object that response will be routed to.
 * @param {String} [proxyIPorDNS] - Specifies the IP or DNS for a HTTP proxy
 * @param {String} [proxyPort] - Specifies the port for the HTTP proxy
 * @constructor
 * @public
 */
function PeerlyXMLHttpRequestManager(globalCallBackName, proxyIPorDNS, proxyPort) {
    this._globalCallBackName = globalCallBackName;
    this._xmlHTTPObjects = {};
    this._javaObject = typeof AndroidJsonXMLHttpRequest === 'undefined' ? peerlyJavaApp : AndroidJsonXMLHttpRequest;
    this._currentKey = 0;
    this._proxyIPorDNS = !proxyIPorDNS ? null : proxyIPorDNS;
    this._proxyPort = !proxyPort ? 0 : proxyPort;

    if (typeof window[globalCallBackName] !== 'undefined') {
        throw "The globalCallBackName has already been defined so we have to assume someone else is using it.";
    }
    window[globalCallBackName] = this;
}

/**
 * Calls across to Java to make the request
 * @param {PeerlyXMLHttpRequest} xmlHttpRequestObject - The object to be called back when the response shows up
 * @param {PeerlyXMLHttpRequestObject} peerlyXMLHttpRequestArguments
 * @returns {number} - This is the key used to index the request, it will be used to find the right object when the
 * response shows up.
 * @public
 */
PeerlyXMLHttpRequestManager.prototype.send = function (xmlHttpRequestObject, peerlyXMLHttpRequestArguments) {
    this._currentKey += 1;
    this._xmlHTTPObjects[this._currentKey] = xmlHttpRequestObject;
    this._javaObject.sendJsonXmlHTTPRequest(this._globalCallBackName, this._currentKey, JSON.stringify(peerlyXMLHttpRequestArguments), this._proxyIPorDNS, this._proxyPort);
    return this._currentKey;
};

/**
 * Called by the Java object to return the response. This function will look up the right handler object by key
 * and call its _receiveResponse method.
 * @param {Number} key
 * @param {String} responseJsonString - A serialized version of PeerlyXMLHttpResponseObject
 * @public
 */
PeerlyXMLHttpRequestManager.prototype.receive = function (key, responseJsonString) {
    var currentObject = this._xmlHTTPObjects[key];
    if (!currentObject) {
        return;
    }
    currentObject._receiveResponse(JSON.parse(responseJsonString));
    delete this._xmlHTTPObjects[key];
};

/**
 * Abort the call associated with the key
 * @param {Number} key
 * @public
 */
PeerlyXMLHttpRequestManager.prototype.abort = function (key) {
    delete this._xmlHTTPObjects[key];
};

/**
 * A 'mostly' drop in replacement for XMLHTTPRequest. The goal is that the only different behavior is that the
 * constructor need a handler object (mostly for flexibility, in truth we could redesign so there is just one
 * global handler) and that we accept extra options in order to support SSL Mutual Auth.
 * @param {PeerlyXMLHttpRequestManager} peerlyXmlHttpRequestManager
 * @constructor
 */
function PeerlyXMLHttpRequest(peerlyXmlHttpRequestManager) {
    this._readyState = 0;
    this._requestObject = new PeerlyXMLHttpRequestObject();
    this._responseObject = new PeerlyXMLHttpResponseObject();
    this._responseType = "";
    this._onreadystatechange = null;
    this._peerlyXmlHttpRequestManager = peerlyXmlHttpRequestManager;
    this._peerlyGlobalXMLHttpRequestHandlerKey = null;
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
                // TODO: For now we just swallow this since PouchDB sets this to true but we really should throw hen this is true since
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
PeerlyXMLHttpRequest.prototype._setReadyState = function (newValue) {
    this._readyState = newValue;
    if (this._onreadystatechange !== null) {
        this._onreadystatechange();
    }
};

/**
 * Callback used by the PeerlyXMLHttpRequestManager to return responses.
 * @param {PeerlyXMLHttpResponseObject} responseObject
 * @private
 */
PeerlyXMLHttpRequest.prototype._receiveResponse = function (responseObject) {
    this._peerlyGlobalXMLHttpRequestHandlerKey = null;
    this._responseObject = responseObject;
    this._setReadyState(4);
};

/**
 * Establish a connection
 * @param {String} method - HTTP request method, make sure to use proper cassing as HTTP methods are case sensitive
 * @param {String} url - URL request will be sent to
 */
PeerlyXMLHttpRequest.prototype.open = function (method, url) {
    if (this._readyState !== 0) {
        this.abort();
    }

    this._requestObject.method = method;
    this._requestObject.url = url;
    this._setReadyState(1);
};

/**
 * Add a HTTP request header. Note that if a header with the specified name has already been set then the value
 * will be appended after a comma.
 * @param {String} header
 * @param {String} value
 */
PeerlyXMLHttpRequest.prototype.setRequestHeader = function (header, value) {
    if (this.readyState !== 1) {
        throw "Method can only be called after open and before send";
    }

    var currentHeaderValue = this._requestObject.headers[header];

    this._requestObject.headers[header] = currentHeaderValue === undefined ? value : currentHeaderValue + "," + value;
};

/**
 * Abort a request.
 */
PeerlyXMLHttpRequest.prototype.abort = function () {
    if (this._peerlyGlobalXMLHttpRequestHandlerKey !== null) {
        this._peerlyXmlHttpRequestManager.abort(this._peerlyGlobalXMLHttpRequestHandlerKey);
    }

    this._requestObject = new PeerlyXMLHttpRequestObject();

    this._setReadyState(0);
};

/**
 * Retrieve the value of a HTTP response header
 * @param {String} header
 * @returns {String|null}
 */
PeerlyXMLHttpRequest.prototype.getResponseHeader = function (header) {
    if (this.readyState < 2) {
        return null;
    }

    return this._responseObject.headers[header];
};

/**
 * Send a request, make sure to have called open first. The request body is whatever value, if any, that is put in data.
 * @param {String|null} data
 */
PeerlyXMLHttpRequest.prototype.send = function (data) {
    if (this.readyState !== 1) {
        throw "You must have called open and not previously called send.";
    }

    this._requestObject.requestText = (typeof data === 'undefined' || data === null) ? "" : data;

    this._peerlyGlobalXMLHttpRequestHandlerKey = this._peerlyXmlHttpRequestManager.send(this, this._requestObject);
};