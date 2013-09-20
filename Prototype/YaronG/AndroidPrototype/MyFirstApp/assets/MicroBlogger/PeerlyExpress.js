// This is a hacked up mess of Express from Node.JS, I just built enough to do what we need for PouchDBExpress
var Express = new Object();
Express._Handlers = [];

Express._HandlerGenerator = function(matchingMethod, matchingUri, callback)
{
    var methodPattern = Express._processWildCardStringToRegEx(matchingMethod);
    var uriPattern = Express._processWildCardStringToRegEx(matchingUri);
    return function(submittedMethod, submittedUri)
    {
        return (methodPattern.test(submittedMethod) && uriPattern.test(submittedUri)) ? callback : undefined;
    }
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
    var callBack = handlers[index](req.method, req.pathname);
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

Express.PeerlyHttpServerCallback = function(method, requestUriPath, jsonQueryParams, jsonHeaders, requestBody, responseCallBack)
{
    var req = Express._createReqObject(method, requestUriPath, jsonQueryParams, requestBody);
    var res = Express._createResObject(responseCallBack);
    Express._ProcessRequest(req, res, 0, Express._Handlers);
}

Express._createReqObject = function(method, requestUriPath, jsonQueryParams, jsonHeaders, requestBody)
{
    var queryParams = JSON.parse(jsonQueryParams);
    var rawHeaderParams = JSON.parse(jsonHeaders);
    var headerParams = {};
    for(var headerName in rawHeaderParams)
    {
        headerParams[headerName.toUpperCase()] = rawHeaderParams[headerName];
    }
    var req = {
        "method": method,
        "pathname": requestUriPath,
        "body": requestBody,
        "query": {},
        "protocol": "http",
        "host": "localhost",
        "subdomains": [],
        "get": function(headerName)
        {
            // HTTP headers are only supposed to contain ASCII so in theory this is actually safe
            return headerParams[headerName.toUpperCase()];
        }
    }

    for(var queryParamName in queryParams)
    {
        if (queryParamName != "NanoHttpd.QUERY_STRING")
        {
            req.query[queryParamName] = queryParams[queryParamName];
        }
    }

    return req;
}

Express._createResObject = function(responseCallBack)
{
    var res =
    {
        "send": function(responseCode, responseObject)
        {
            var response = new Object();
            response.responseCode = responseCode;
            //TODO: We need to figure out how to set sane mime types when dealing with documents, but for now I don't care.
            response.responseMIMEType = "application/json";
            response.responseBody = typeof responseObject == "string" ? responseObject : JSON.stringify(responseObject);
            if (res.hasOwnProperty("_Location")) {
                response.LocationHeader = res._Location;
            }
            responseCallBack(response);
        },
        "location": function(locationHeaderValue)
        {
            res._Location = locationHeaderValue;
        }
    }
    return res;
}