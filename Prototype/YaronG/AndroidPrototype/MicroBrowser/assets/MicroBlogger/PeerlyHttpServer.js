// I can't wait until all of this just goes away and we can run proper node.js inside the browser, but until then
// this awful hack will have to do.
var PeerlyHttpServer = new Object();

PeerlyHttpServer._server = {};

PeerlyHttpServer.isHttpServerRunning = function()
{
    return PeerlyHttpServer._server.isHttpServerRunning();
}

var _PeerlySubmittedCallBack = {};

// port - TCP port to list on
// callback - Will be given a Javascript object representing the request and a callback function expecting to get
// a Javascript object with the required format as defined in the Java code. See JsonNanoHTTPD.java.
PeerlyHttpServer.startHttpServer = function(port, callback)
{
    // BugBug: This depends on the applet tag (in the non-Android case) which is stupid, we need to load the applet and name it programmatically
    // so the code is more robust, see notes for how to do this pretty easily
    PeerlyHttpServer._server = (typeof SimpleJavascriptHttpServerAndroid == 'undefined') ? simpleJavascriptHttpServerApp : SimpleJavascriptHttpServerAndroid;

    _PeerlySubmittedCallBack = function(jsonNanoHTTPDRequestString)
    {
        // It turns out I can't just return PeerlyHttpServer._server_.setResponse, this will trigger an error
        // called "NPMethod called on non-NPObject". The way around this is to use a lambda.
        var responseCallBack = function(response) { PeerlyHttpServer._server.setResponse(JSON.stringify(response)) };
        callback(JSON.parse(jsonNanoHTTPDRequestString), responseCallBack);
    }

    PeerlyHttpServer._server.startHttpServer(port, "_PeerlySubmittedCallBack");
}