// This is a hacked up mess of Express from Node.JS, I just built enough to do what we need for PouchDBExpress
var Express = new Object();
Express._Handlers = [];

Express._HandlerGenerator = function(matchingMethod, matchingUri, callback)
{
    var methodPattern = Express._processWildCardStringToRegEx(matchingMethod);
    var keys = [];
    // TODO: I need to study the last two flags more closely. I had to set them to false in order to match
    // foo and foo/ (e.g. to match a request regardless of the presence or absence of a / at the end) but
    // I should better understand what they are up to.
    var uriRegEx = Express._pathRegexp(matchingUri, keys, false, false);
    return function(req)
    {
        req.params = [];
        return (methodPattern.test(req.method) && Express._parsePathForMatch(uriRegEx, req.pathname, keys, req.params)) ? callback : undefined;
    }
}

// The following was adapted from Route.prototype.match in https://github.com/visionmedia/express/blob/master/lib/router/route.js
Express._parsePathForMatch = function(uriRegEx, path, keys, params)
{
    var matches = uriRegEx.exec(path);

    if (!matches) return false;

    for(var i = 1, len = matches.length; i < len; ++i)
    {
        var key = keys[i - 1];

        var val = 'string' == typeof matches[i]
            ? Express._decode(matches[i])
            : matches[i];

        if (key) {
            params[key.name] = val;
        } else {
            params.push(val);
        }
    }

    return true;
}

Express._processWildCardStringToRegEx = function(wildcardString)
{
    var replaceWildCard = wildcardString.replace(/[*]/g,'.*');
    var regexPattern = new RegExp("^"+replaceWildCard+"$");
    regexPattern.compile(regexPattern);
    return regexPattern;
}

Express.use = function(callback)
{
    Express._Handlers.push(Express._HandlerGenerator("*","*",callback));
}

Express._supportedMethods = [{"friendlyName":"get", "methodName":"GET"},
 {"friendlyName":"post","methodName":"POST"},
 {"friendlyName":"put", "methodName":"PUT"},
 {"friendlyName":"del", "methodName":"DELETE"}];

Express._supportedMethods.forEach(function(methodObj, index, array){
    Express[methodObj.friendlyName] = function(matchingUri, callback){
      Express._Handlers.push(Express._HandlerGenerator(methodObj.methodName, matchingUri, callback));
    }
})

Express.all = function(matchingUri, callback)
{
    Express._Handlers.push(Express._HandlerGenerator("*", matchingUri, callback));
}

Express._ProcessRequest = function(req, res, index, handlers)
{
    if (index > handlers.length - 1)
    {
        res.send(404);
        return;
    }
    var callBack = handlers[index](req);
    if (typeof callBack == 'function')
    {
        try
        {
            callBack(req, res, function() {
               Express._ProcessRequest(req, res, index + 1, handlers);
            });
        }
        catch(err)
        {
            throw err;
        }
    }
    else
    {
        Express._ProcessRequest(req, res, index + 1, handlers);
    }
}

Express.PeerlyHttpServerCallback = function(jsonNanoHTTPDRequestObject, responseCallBack)
{
    var req = Express._createReqObject(jsonNanoHTTPDRequestObject);
    var res = Express._createResObject(responseCallBack);
    Express._ProcessRequest(req, res, 0, Express._Handlers);
}

Express._createReqObject = function(jsonNanoHTTPDRequestObject)
{
    var req = {
        "method": jsonNanoHTTPDRequestObject.method,
        "pathname": jsonNanoHTTPDRequestObject.pathname,
        "body": jsonNanoHTTPDRequestObject.body,
        "query": {},
        "protocol": jsonNanoHTTPDRequestObject.protocol,
        "host": jsonNanoHTTPDRequestObject.host,
        "subdomains": jsonNanoHTTPDRequestObject.subdomains,
        "_requestHeaders": {},
        "get": function(headerName)
        {
            // HTTP headers are only supposed to contain ASCII so in theory this is actually safe
            return req._requestHeaders[headerName.toUpperCase()];
        },
        "params": []
    }

    for(var headerName in jsonNanoHTTPDRequestObject._requestHeaders)
    {
        req._requestHeaders[headerName.toUpperCase()] = jsonNanoHTTPDRequestObject._requestHeaders[headerName];
    }

    for(var queryName in jsonNanoHTTPDRequestObject.query)
    {
        if (queryName != "NanoHttpd.QUERY_STRING")
        {
            req.query[queryName] = jsonNanoHTTPDRequestObject.query[queryName];
        }
    }

    return req;
}

Express._createResObject = function(responseCallBack)
{
    var res =
    {
        "_responseHeaders" : {},
        "send": function(responseCode, responseObject)
        {
            var response = new Object();
            response.responseCode = responseCode;
            //TODO: We need to figure out how to set sane mime types when dealing with documents, but for now I don't care.
            response.responseMIMEType = "application/json";
            response.responseBody = typeof responseObject == "string" ? responseObject : JSON.stringify(responseObject);
            response._responseHeaders = res._responseHeaders;
            responseCallBack(response);
        },
        "setHeader": function(name, value)
        {
            res._responseHeaders[name] = value;
        },
        "location": function(locationHeaderValue)
        {
            res.setHeader["Location"] = locationHeaderValue;
        }
    }
    return res;
}

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
Express._pathRegexp = function(path, keys, sensitive, strict) {
    if (toString.call(path) == '[object RegExp]') return path;
    if (Array.isArray(path)) path = '(' + path.join('|') + ')';
    path = path
        .concat(strict ? '' : '/?')
        .replace(/\/\(/g, '(?:/')
        .replace(/(\/)?(\.)?:(\w+)(?:(\(.*?\)))?(\?)?(\*)?/g, function(_, slash, format, key, capture, optional, star){
            keys.push({ name: key, optional: !! optional });
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
}

/**
 * Decodes a URI component. Returns
 * the original string if the component
 * is malformed.
 *
 * @param {String} str
 * @return {String}
 * @api private
 */

Express._decode = function(str) {
    try {
        return decodeURIComponent(str);
    } catch (e) {
        return str;
    }
}