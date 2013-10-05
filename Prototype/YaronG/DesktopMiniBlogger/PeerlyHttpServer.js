// I can't wait until all of this just goes away and we can run proper node.js inside the browser, but until then
// this awful hack will have to do.

/**
 * Creates a new HTTP server on the specified port. Any incoming requests will be forwarded to the _callback. Note
 * that right now because of how the Java works only a single server listening to a single port is supported within
 * a single page.
 * @param {Number} port - The port the HTTP server should listen on
 * @param {Function} callback - The callback to handle any incoming requests
 * @constructor
 */
function PeerlyHttpServer(port, callback) {
    this._port = port;
    this._callback = callback;

    // TODO: This depends on the applet tag (in the non-Android case) which is stupid, we need to load the applet and name it programmatically
    // so the code is more robust, see notes for how to do this pretty easily
    this._server = (typeof AndroidJsonNanoHTTPD === 'undefined') ? window.peerlyJavaApp.getJsonNanoHTTPDJavascriptInterface() : AndroidJsonNanoHTTPD;

    if (this._server.isHttpServerRunning(port)) {
        throw "There is already a HTTP server running on that port.";
    }

    // This is a function, defined off the global window object so so as to keep my silly .call method in LiveConnectJsonNanoHTTPD happy, that callbacks
    // about this port will be sent to.
    var externalCallBackName = "_PeerlySubmittedCallBack" + port;
    window[externalCallBackName] = this._incomingRequestCallBackHandlerGenerator();

    this._server.startHttpServer(port, externalCallBackName);
}

/**
 * Generates a callback to be used to handle a HTTP request.
 * @returns {Function}
 * @private
 */
PeerlyHttpServer.prototype._incomingRequestCallBackHandlerGenerator = function () {
    var callBack = function (jsonNanoHTTPDRequestString) {
        // It turns out I can't just return PeerlyHttpServer._server_.setResponse, this will trigger an error
        // called "NPMethod called on non-NPObject". The way around this is to use a lambda.
        var responseCallBack = function (response) {
            this._server.setResponse(this._port, JSON.stringify(response));
        };
        this._callback(JSON.parse(jsonNanoHTTPDRequestString), responseCallBack.bind(this));
    };
    return callBack.bind(this);
};

/**
 *
 * @returns {Boolean}
 */
PeerlyHttpServer.prototype.isHttpServerRunning = function () {
    return this._server.isHttpServerRunning(this._port);
};

PeerlyHttpServer.prototype.stopHttpServer = function () {
    this._server.stopHttpServer(this._port);
};

function PeerlyHTTPServerRequestObject() {
    this.method = "";
    this.pathname = "";
    this.body = null;
    this.query = null;
    this.protocol = "";
    this.host = "";
    this.subdomains = null;
    this._requestHeaders = {};
}

function PeerlyHTTPServerResponseObject() {
    this.responseCode = null;
    this.responseMIMEType = "";
    this.responseBody = null;
    this.responseHeaders = {};
}