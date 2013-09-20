/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codeplex.peerly.common;

import com.codeplex.peerly.browser.LiveConnectJsonNanoHTTPD;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author yarong
 * Wraps the NanoHTTPD server in a JSON translation layer
 */
public abstract class JsonNanoHTTPD extends NanoHTTPD {
    private JSONObject responseObject;

    // How the request object is delivered differs in different environments, for
    // example on the PC we use LiveConnect which has different calls/marshalling
    // then say Android's WebView
    protected abstract void deliverRequestJsonToJavascript(JSONObject jsonRequestObject);

    public JsonNanoHTTPD(int port)
    {
        super(port);
    }
    
    @Override
    public Response serve(HTTPSession session) {
        responseObject = null;
        JSONObject jsonRequestObject = createJsonRequestObject(session);
        deliverRequestJsonToJavascript(jsonRequestObject);
        
        while(responseObject == null)
        {
            // TODO: Put some reasonable time out here so we don't get stuck in this loop for infinity
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(LiveConnectJsonNanoHTTPD.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }            
        }
        
        return createResponse(responseObject);
    }
    
    public void SetResponse(JSONObject responseObject)
    {
        this.responseObject = responseObject;
    }

    private JSONObject createJsonRequestObject(HTTPSession session)
    {
        String method = MethodEnumToMethodString(session.getMethod());
        String requestUriPath = session.getUri();
        InputStream inputStream = session.getInputStream();
        Map<String, String> headers = session.getHeaders();
        Map<String, String> queryParams = session.getParms();
        String requestBody = StringifyRequestBody(headers, inputStream);
        
        JSONObject jsonRequestObject = new JSONObject();
        jsonRequestObject.put("method", method);
        jsonRequestObject.put("pathname", requestUriPath);
        jsonRequestObject.put("body", requestBody);
        jsonRequestObject.put("query", queryParams);
        jsonRequestObject.put("protocol","http");
        jsonRequestObject.put("host","localhost");
        jsonRequestObject.put("subdomains", new String[0]);
        jsonRequestObject.put("_requestHeaders", headers);
        return jsonRequestObject;
    }
    
    private String StringifyRequestBody(Map<String, String> headers, InputStream inputStream) throws RuntimeException {
        String requestBody = null;
        int size;
        // This is wrong on so many levels. First, we should validate that
        // we want to deal with the content we have given. Second, this
        // doesn't deal with chunked content. Third, we don't check that
        // the content is actually UTF-8. Fourth, we don't check the MIME
        // type. Fifth, we actually store everything in memory rather than
        // processing it as a stream so we can use RAM better. Etc.
        if (headers.containsKey("content-length"))
        {
            size = Integer.parseInt(headers.get("content-length"));
            byte[] requestBodyByteArray = new byte[size];
            int bytesRead;
            try {
                bytesRead = inputStream.read(requestBodyByteArray);
                if (bytesRead != size)
                {
                    throw new RuntimeException();
                }
            } catch (IOException ex) {
                Logger.getLogger(JsonNanoHTTPD.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }

            try {
                requestBody = new String(requestBodyByteArray,"UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(JsonNanoHTTPD.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException();
            }
        }
        return requestBody;
    }
    
    private String MethodEnumToMethodString(Method methodEnum)
    {
        switch(methodEnum)
        {
            case GET:
                return "GET";
            case PUT:
                return "PUT";
            case POST:
                return "POST";
            case DELETE:
                return "DELETE";
            case HEAD:
                return "HEAD";
            default:
                throw new RuntimeException();
        }
    }
    
    private Response createResponse(JSONObject jsonResponseObject)
    {
        int responseCode = jsonResponseObject.getInt("responseCode");
        String mimeType = jsonResponseObject.getString("responseMIMEType");
        String responseBody = jsonResponseObject.optString("responseBody");
        
        Response simpleResponse = new Response(responseCodeToResponseStatus(responseCode), mimeType, responseBody);

        JSONObject headers = jsonResponseObject.optJSONObject("_responseHeaders");
        if (headers != null)
        {
            Iterator<String> keys = headers.keys();
            while(keys.hasNext())
            {
                String responseHeaderName = keys.next();
                String responseHeaderValue = headers.getString(responseHeaderName);
                simpleResponse.addHeader(responseHeaderName, responseHeaderValue);
            }
        }
        return simpleResponse;
    }
    
    private static Response.Status responseCodeToResponseStatus(int responseCode)
    {
        switch(responseCode)
        {
            case 200:
                return NanoHTTPD.Response.Status.OK;
            case 201:
                return NanoHTTPD.Response.Status.CREATED;
            case 202:
                return NanoHTTPD.Response.Status.ACCEPTED;
            case 204:
                return NanoHTTPD.Response.Status.NO_CONTENT;
            case 206:
                return NanoHTTPD.Response.Status.PARTIAL_CONTENT;
            case 301:
                return NanoHTTPD.Response.Status.REDIRECT;
            case 304:
                return NanoHTTPD.Response.Status.NOT_MODIFIED;
            case 400:
                return NanoHTTPD.Response.Status.BAD_REQUEST;
            case 401:
                return NanoHTTPD.Response.Status.UNAUTHORIZED;
            case 403:
                return NanoHTTPD.Response.Status.FORBIDDEN;
            case 404:
                return NanoHTTPD.Response.Status.NOT_FOUND;
            case 416:
                return NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE;
            case 500:
                return NanoHTTPD.Response.Status.INTERNAL_ERROR;
            default:
                throw new RuntimeException();
        }
    }
}
