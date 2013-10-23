// PEERLYEXPRESSREQUESTOBJECT

/**
 * Represents an Express 'req' object, a subset of both the node.js and express req object
 * @param {PeerlyHTTPServerRequestObject} peerlyHTTPServerRequestObject
 * @constructor
 */
function PeerlyExpressRequestObject(peerlyHTTPServerRequestObject) {
    this.method = peerlyHTTPServerRequestObject.method;
    this.pathname = peerlyHTTPServerRequestObject.pathname;
    this.body = peerlyHTTPServerRequestObject.body;
    this.query = {};
    this.protocol = peerlyHTTPServerRequestObject.protocol;
    this.host = peerlyHTTPServerRequestObject.host;
    this.subdomains = peerlyHTTPServerRequestObject.subdomains;
    this._requestHeaders = {};
    this.params = [];
    this.socket = {
        "setTimeout": function (timeout) {
            //NOOP for now
        }
    };

    for (var headerName in peerlyHTTPServerRequestObject._requestHeaders) {
        this._requestHeaders[headerName.toUpperCase()] = peerlyHTTPServerRequestObject._requestHeaders[headerName];
    }

    for (var queryName in peerlyHTTPServerRequestObject.query) {
        if (queryName != "NanoHttpd.QUERY_STRING") {
            this.query[queryName] = peerlyHTTPServerRequestObject.query[queryName];
        }
    }
}

/**
 * Return the value of the named header, header names are not case sensitive
 * @param {String} headerName
 * @returns {String|null}
 */
PeerlyExpressRequestObject.prototype.get = function (headerName) {
    // HTTP headers are only supposed to contain ASCII so in theory this is actually safe
    return this._requestHeaders[headerName.toUpperCase()];
};

// PEERLYEXPRESSRESPONSEOBJECT

/**
 * Represents an Express 'res' object, a subset of both the node.js and express res object
 * @param {Function} responseCallBack - The hook into the HTTP server to call when the response is ready to be sent.
 * @constructor
 */
function PeerlyExpressResponseObject(responseCallBack) {
    this._responseHeaders = {};
    this._responseCallBack = responseCallBack;
}

/**
 * Send the response to the requester
 * @param {Number} responseCode
 * @param {String | null} responseBody - The body, if any, to send in the response
 */
PeerlyExpressResponseObject.prototype.send = function (responseCode, responseBody) {
    var response = new PeerlyHTTPServerResponseObject();
    response.responseCode = responseCode;
    //TODO: We need to figure out how to set sane mime types when dealing with documents, but for now I don't care.
    response.responseMIMEType = "application/json";
    response.responseBody = typeof responseBody == "string" ? responseBody : JSON.stringify(responseBody);
    response._responseHeaders = this._responseHeaders;
    this._responseCallBack(response);
};

/**
 * Set a header on the response
 * @param {String} name
 * @param {String} value
 */
PeerlyExpressResponseObject.prototype.setHeader = function (name, value) {
    this._responseHeaders[name] = value;
};

/**
 * Set the location header
 * @param {String} locationHeaderValue
 */
PeerlyExpressResponseObject.prototype.location = function (locationHeaderValue) {
    this.setHeader["Location"] = locationHeaderValue;
};

// PEERLYEXPRESS

/**
 * Sets up an Express style wrapper listening to a HTTP server on port that is run by "new peerlyHTTPServer()".
 * @param {Number} port
 * @param {Function} peerlyHttpServer - This is the server function, not an instance, we will new up an instance inside.
 * @constructor
 */
function PeerlyExpress(port, peerlyHttpServer) {
    this._handlers = [];
    var supportedMethods = [
        {"friendlyName": "get", "methodName": "GET"},
        {"friendlyName": "post", "methodName": "POST"},
        {"friendlyName": "put", "methodName": "PUT"},
        {"friendlyName": "del", "methodName": "DELETE"}
    ];

    // Defines handlers for specific HTTP Method handlers on the express object
    for (var i = 0; i < supportedMethods.length; ++i) {
        var methodObj = supportedMethods[i];
        this[methodObj.friendlyName] = this._defaultMethodGenerator(methodObj.methodName, this._handlers, this._handlerGenerator.bind(this));
    }

    this._peerlyHttpServer = new peerlyHttpServer(port, this.PeerlyHttpServerCallbackGenerator());
}

/**
 * Generates methods to generate handlers for the named method
 * @param {String} methodName
 * @param {Function[]} handlers
 * @param {Function} handlerGenerator
 * @returns {Function}
 * @private
 */
PeerlyExpress.prototype._defaultMethodGenerator = function (methodName, handlers, handlerGenerator) {
    return function (matchingUri, callback) {
        handlers.push(handlerGenerator(methodName, matchingUri, callback));
    }
};

/**
 * Generates request handlers to process incoming request.
 * @param {String} matchingMethod - Either a string or a *
 * @param {String} matchingPath - Processed with the Express path syntax processor
 * @param {Function} callback - callback to handle processing a match
 * @returns {Function} - Tests if a request matches and if so runs the callback
 * @private
 */
PeerlyExpress.prototype._handlerGenerator = function (matchingMethod, matchingPath, callback) {
    var methodPattern = this._processWildCardStringToRegEx(matchingMethod),
        keys = [];
    // TODO: I need to study the last two flags more closely. I had to set them to false in order to match
    // foo and foo/ (e.g. to match a request regardless of the presence or absence of a / at the end) but
    // I should better understand what they are up to.
    var uriRegEx = this._pathRegexp(matchingPath, keys, false, false);
    var handlerCallBack = function (req) {
        req.params = [];
        return (methodPattern.test(req.method) && this._parsePathForMatch(uriRegEx, req.pathname, keys, req.params)) ? callback : undefined;
    };
    return handlerCallBack.bind(this);
};

// The following was adapted from Route.prototype.match in https://github.com/visionmedia/express/blob/master/lib/router/route.js
/**
 * This code has two jobs. First, it has to determien if the submitted path matches the submitted regex. Second, if it
 * does match then it needs to bind any named parts of the match (listed in keys) as properties on params or if
 * there are just regex groups then bind those groups as entries where params is treated as an array.
 * @param {RegExp} uriRegEx
 * @param {String} path
 * @param {String[]} keys - named values to look for in the path
 * @param {Object|String[]} params - Records either found named values or found groups
 * @returns {boolean} - Did the path match the regex.
 * @private
 */
PeerlyExpress.prototype._parsePathForMatch = function (uriRegEx, path, keys, params) {
    var matches = uriRegEx.exec(path);

    if (!matches) return false;

    for (var i = 1, len = matches.length; i < len; ++i) {
        var key = keys[i - 1];

        var val = 'string' == typeof matches[i]
            ? this._decode(matches[i])
            : matches[i];

        if (key) {
            params[key.name] = val;
        } else {
            params.push(val);
        }
    }

    return true;
};

/**
 * Used for method names to support the use of *.
 * @param {String} wildcardString
 * @returns {RegExp}
 * @private
 */
PeerlyExpress.prototype._processWildCardStringToRegEx = function (wildcardString) {
    var replaceWildCard = wildcardString.replace(/[*]/g, '.*');
    return new RegExp("^" + replaceWildCard + "$");
};

/**
 * Handler that matches all methods and paths.
 * @param {Function} callback - handler to call on a match
 */
PeerlyExpress.prototype.use = function (callback) {
    this._handlers.push(this._handlerGenerator("*", "*", callback));
};

/**
 * Handler that matches all methods
 * @param {String} matchPath - path to match using Express magical matching syntax
 * @param {Function} callback - handler to call if a match is found
 */
PeerlyExpress.prototype.all = function (matchPath, callback) {
    this._handlers.push(this._handlerGenerator("*", matchPath, callback));
};

/**
 * Recursively (yes, I know, it's not efficient on Javascript) walks through the handlers to find the first one
 * that matches and calls its callBack.
 * @param {PeerlyExpressRequestObject} req
 * @param {PeerlyExpressResponseObject} res
 * @param {Number} index - Index for the entry in the handler array we are testing for a match
 * @param {Function[]} handlers
 * @private
 */
PeerlyExpress.prototype._ProcessRequest = function (req, res, index, handlers) {
    if (index > handlers.length - 1) {
        res.send(404, null);
        return;
    }
    var callBack = handlers[index](req);
    if (typeof callBack == 'function') {
        try {
            var nextCallback = function () {
                this._ProcessRequest(req, res, index + 1, handlers);
            };

            callBack(req, res, nextCallback.bind(this));
        }
        catch (err) {
            throw err;
        }
    }
    else {
        this._ProcessRequest(req, res, index + 1, handlers);
    }
};

/**
 * Generates the function that will be called back by the HTTP server.
 * @returns {function(this:PeerlyExpress)}
 * @constructor
 */
PeerlyExpress.prototype.PeerlyHttpServerCallbackGenerator = function () {
    var callBack = function (jsonNanoHTTPDRequestObject, responseCallBack) {
        var req = new PeerlyExpressRequestObject(jsonNanoHTTPDRequestObject);
        var res = new PeerlyExpressResponseObject(responseCallBack);
        this._ProcessRequest(req, res, 0, this._handlers);
    };
    return callBack.bind(this);
};

// The following code was taken from https://github.com/visionmedia/express/blob/master/lib/utils.js
// The following license was included with the codebase:
//(The MIT License)
//
//Copyright (c) 2009-2013 TJ Holowaychuk <tj@vision-media.ca>
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//'Software'), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//    distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//    The above copyright notice and this permission notice shall be
//included in all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
//    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//    IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/**
 * Normalize the given path string,
 * returning a regular expression.
 *
 * An empty array should be passed,
 * which will contain the placeholder
 * key names. For example "/user/:id" will
 * then contain ["id"].
 *
 * @param {String|RegExp|Array} path
 * @param {Array} keys
 * @param {Boolean} sensitive
 * @param {Boolean} strict
 * @return {RegExp}
 * @api private
 */
PeerlyExpress.prototype._pathRegexp = function (path, keys, sensitive, strict) {
    if (toString.call(path) == '[object RegExp]') return path;
    if (Array.isArray(path)) path = '(' + path.join('|') + ')';
    path = path
        .concat(strict ? '' : '/?')
        .replace(/\/\(/g, '(?:/')
        .replace(/(\/)?(\.)?:(\w+)(?:(\(.*?\)))?(\?)?(\*)?/g, function (_, slash, format, key, capture, optional, star) {
            keys.push({ name: key, optional: !!optional });
            slash = slash || '';
            return ''
                + (optional ? '' : slash)
                + '(?:'
                + (optional ? slash : '')
                + (format || '') + (capture || (format && '([^/.]+?)' || '([^/]+?)')) + ')'
                + (optional || '')
                + (star ? '(/*)?' : '');
        })
        .replace(/([\/.])/g, '\\$1')
        .replace(/\*/g, '(.*)');
    return new RegExp('^' + path + '$', sensitive ? '' : 'i');
};

/**
 * Decodes a URI component. Returns
 * the original string if the component
 * is malformed.
 *
 * @param {String} str
 * @return {String}
 * @api private
 */

PeerlyExpress.prototype._decode = function (str) {
    try {
        return decodeURIComponent(str);
    } catch (e) {
        return str;
    }
};