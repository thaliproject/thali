// This is a hacked up mess of Express from Node.JS, I just built enough to do what we need for PouchDBExpress
var Express = new Object();
Express.Handlers = [];

Express._HandlerGenerator = function(matchingMethod, matchingUri, callback)
{
    var matchingUriPattern = matchingUri + ((matchingUri[matchingUri.length - 1] == "*") ? ".*" : "");
    var uriPattern = new RegExp(matchingUri);
    uriPattern.compile(uriPattern);
    return function(submittedMethod, submittedUri)
    {
        return (matchingMethod == submittedMethod && uriPattern.test(submittedUri)) ? callback : undefined;
    }
}

Express.use = function(callback)
{
    Express.Handlers.push("*","*", callback);
}

[{"friendlyName":"get", "methodName":"GET"},
 {"friendlyName":"post","methodName":"POST"},
 {"friendlyName":"put", "methodName":"PUT"},
 {"friendlyName":"del", "methodName":"DELETE"}].forEach(function(methodObj, index, array){
    Express[methodObj.friendlyName] = function(matchingUri, callback){
      Express.Handlers.push(Express._HandlerGenerator(methodObj.methodName, matchingUri, callback));
    }
})

Express.all = function(matchingUri, callback)
{
    Express.Handlers.push("*", matchingUri, callback);
}

Express._ProcessRequest = function(req, res, index, array)
{
    if (index > array.length) return;
    var callBack = array[index](req.method, req.pathname);
    if (typeof callBack == 'function')
    {
        callback(req, res, function() {
           Express._ProcessRequest(req, res, index + 1, array);
        });
    }
    else
    {
        Express._ProcessRequest(req, res, index + 1, array);
    }
}