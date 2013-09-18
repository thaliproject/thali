// I can't wait until all of this just goes away and we can run proper node.js inside the browser, but until then
// this awful hack will have to do.
var PeerlyHttpServer = new Object();

// BugBug: This depends on the applet tag which is stupid, we need to load the applet and name it programmatically
// so the code is more robust, see notes for how to do this pretty easily
PeerlyHttpServer._server = {};

PeerlyHttpServer.isHttpServerRunning = function()
{
    return PeerlyHttpServer._server.isHttpServerRunning();
}

var _PeerlySubmittedCallBack = {};

PeerlyHttpServer.startHttpServer = function(port, callback)
{
    PeerlyHttpServer._server = simpleJavascriptHttpServerApp;

    _PeerlySubmittedCallBack = function(method, requestUriPath, jsonQueryParams, jsonHeaders, requestBody)
    {
        // It turns out I can't just return PeerlyHttpServer._server_.setResponse, this will trigger an error
        // called "NPMethod called on non-NPObject". The way around this is to use a lambda.
        var responseCallBack = function(response) { PeerlyHttpServer._server.setResponse(response) };
        callback(method, requestUriPath, jsonQueryParams, jsonHeaders, requestBody, responseCallBack);
    }

    PeerlyHttpServer._server.startHttpServer(port, "_PeerlySubmittedCallBack");
}