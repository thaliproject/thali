var _peerlyGlobalXMLHttpRequestHandler = {};

_peerlyGlobalXMLHttpRequestHandler._xmlHTTPObjects = {};
_peerlyGlobalXMLHttpRequestHandler._javaObject = {};
_peerlyGlobalXMLHttpRequestHandler._currentKey = 0;


_peerlyGlobalXMLHttpRequestHandler.send = function(xmlHttpRequestObject, requestJsonString)
{
    _peerlyGlobalXMLHttpRequestHandler._currentKey += 1;
    _peerlyGlobalXMLHttpRequestHandler._xmlHTTPObjects[this._currentKey] = xmlHttpRequestObject;
    _peerlyGlobalXMLHttpRequestHandler._javaObject.send(this._currentKey, requestJsonString);
    return _peerlyGlobalXMLHttpRequestHandler._currentKey;
}

_peerlyGlobalXMLHttpRequestHandler.receive = function(key, responseJsonString)
{
    var currentObject = _peerlyGlobalXMLHttpRequestHandler._xmlHTTPObjects[key];
    if (currentObject == null)
    {
        return;
    }

    currentObject._receiveResponse(JSON.parse(responseObject));
    delete _peerlyGlobalXMLHttpRequestHandler._xmlHTTPObjects[key];
}

_peerlyGlobalXMLHttpRequestHandler.abort = function(key)
{
    delete _peerlyGlobalXMLHttpRequestHandler._xmlHTTPObjects[key];
}

XMLHttpRequest = function() {
    this._requestObjectTemplate = { "method" : "", "url" : "", "headers" : {}, "requestText" : null};
    this._readyState = 0;
    this._requestObject = this._requestObjectTemplate;
    this._responseObject = { "status" : "", "headers" : {}, "responseText" : null};
    this._responseType = "";
    this._onreadystatechange = null;
    this._peerlyGlobalXMLHttpRequestHandlerKey = null;

    Object.defineProperties(this, {
        "responseType": {
            "get" : function() { return this._responseType; },
            "set" : function(newResponseType) {
                if (newResponseType != "")
                {
                    throw "Sorry, we only support string response types which are represented as an empty string";
                }
            }
        },
        "withCredentials" : {
            "get" : function() { return false; },
            "set" : function(newValue) {
                if (newValue != false)
                {
                    throw "Sorry we only support false for withCredentials since we ignore CORS and we don't support cookies."
                }
            }
        },
        "onreadystatechange" : {
            "get" : function() { return onreadystatechange; },
            "set" : function(newValue) {
                if (newValue != null && typeof newValue != 'function')
                {
                    throw "Sorry, only functions or null can be submitted."
                }

                this._onreadystatechange = newValue;
            }
        },
        "readyState" : {
            "get" : function() { return this._readyState; }
        },
        "status" : {
            "get" : function() {
                if (this.readyState < 2)
                {
                    throw "status isn't available until we get to at least readystate 3";
                }

                return this._responseObject.status;
            }
        },
        "responseText" : {
            "get" : function() {
                if (this.readyState < 4)
                {
                    return null;
                }

                return this._responseObject.responseText;
            }
        }
    })
}

XMLHttpRequest.prototype._setReadyState = function(newValue)
{
    this._readyState = newValue;
    if (this._onreadystatechange != null)
    {
        this._onreadystatechange();
    }
}

XMLHttpRequest.prototype._receiveResponse = function(responseObject)
{
    this._peerlyGlobalXMLHttpRequestHandlerKey = null;
    this._responseObject = responseObject;
    this._setReadyState(4);
}

XMLHttpRequest.prototype.open = function(method, url)
{
    if (this._readyState != 0)
    {
        this.abort();
    }

    this._requestObject.method = method;
    this._requestObject.url = url;
}

XMLHttpRequest.prototype.setRequestHeader = function(header, value)
{
    if (this.readyState != 1)
    {
        throw "Method can only be called after open and before send";
    }

    var currentHeaderValue = this._requestObject.headers[header];

    var newHeaderValue = currentHeaderValue == null ? value : currentHeaderValue + "," + value;

    this._requestObject.headers[header] = newHeaderValue;
}

XMLHttpRequest.prototype.abort = function()
{
    if (this._peerlyGlobalXMLHttpRequestHandlerKey != null)
    {
        _peerlyGlobalXMLHttpRequestHandler.abort(this._peerlyGlobalXMLHttpRequestHandlerKey);
    }

    this._requestObject = this._requestObjectTemplate;

    this._setReadyState(0);
}

XMLHttpRequest.prototype.getResponseHeader = function(header)
{
    if (this.readyState < 2)
    {
        return null;
    }

    return this._responseObject.headers[header];
}

XMLHttpRequest.prototype.send = function(data)
{
    if (this.readyState != 1)
    {
        throw "You must have only called open."
    }

    this._requestObject.requestText = data;

    this._peerlyGlobalXMLHttpRequestHandlerKey = _peerlyGlobalXMLHttpRequestHandler.send(JSON.stringify(this._requestObject));
}