package com.codeplex.peerly.common;

import com.codeplex.peerly.org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This whole design is silly because it takes a synchronous function httpURLConnection and makes it asynchronous.
 * It also doesn't support transport encoding (read: inline compression), chunking, or properly handle XMLHTTPRequest
 * behaviors such as streaming out the request body, notifying when headers are available and being able to stream
 * back the response body.
 * So why do something so badly designed?
 * The main reason is speed. Probably the best client library to really get things going right would be the Apache
 * HTTP client library but the Android gods frown on it (http://android-developers.blogspot.in/2011/09/androids-http-clients.html).
 * O.k. that's not a great reason but I would have to include the Apache HTTP client library in the Applet version of the
 * code which required either always including it or having split code basis and neither was any fun.
 * The silly design below is my way of handling the limitations of WebView. Unlike LiveConnect (Read: applets) which
 * support creating new Java objects and passing them into Javascript, even managing their lifetimes, such is not the
 * case with addJavascriptInterface which doesn't support exposing new Java objects without refreshing the entire
 * browser session and doesn't have any way to pass the whole Java object in. This is a real problem because we
 * want to give people an experience of using a XMLHttpRequest object that should be GC'd like any Javascript
 * object but since Javascript doesn't have finalizers or destructors there is no way for us to know when an object
 * has gone out of scope and therefore we would never know when to garbage collect the associated Java object!
 * I 'solve' (as in see that dirt on that window? Let me use my hammer to clean that) the problem by creating a
 * static class which asynchronously makes the request, gets the response and then calls back into Javascript.
 * This sucks for all sorts of perf reasons and someday I might have to come up with something better but for right
 * now it seems (famous last words) to work. Just don't try it with anything non-trivially sized or you'll blow RAM.
 * But really, before we can deal with non-trivially sized data we are going to have to make changes everywhere
 * since WebView doesn't support sending values from Java to Javascript that aren't simple types or strings so
 * no ArrayBuffers or blobs or anything. Fixing that will drive lots of other changes.
 */
public abstract class JsonXmlHTTPRequest {

    abstract public void sendResponse(String peerlyXMLHttpRequestManagerObjectName, int key, JSONObject responseObject);

    public void send(String peerlyXMLHttpRequestManagerObjectName, int key, String requestJsonString)
    {
        final String finalPeerlyXMLHttpRequestManagerObjectName = peerlyXMLHttpRequestManagerObjectName;
        final int finalKey = key;
        final String finalRequestJsonString = requestJsonString;

        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Logger.getLogger(JsonXmlHTTPRequest.class.getName()).log(Level.SEVERE, null, throwable);
                // TODO: There is an error event on xmlhttprequest that one can listen to and we should eventually
                // hook this up to that but that event isn't used by PouchDB so for now I'll skip it.
            }
        };

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject(finalRequestJsonString);
                String urlString = jsonObject.getString("url");
                try {
                    HttpURLConnection httpURLConnection = sendRequest(jsonObject, urlString);

                    JSONObject responseObject = getResponse(httpURLConnection);

                    sendResponse(finalPeerlyXMLHttpRequestManagerObjectName, finalKey, responseObject);
                } catch (MalformedURLException e) {
                    // TODO: Interesting enough there is an error handler for xmlhttprequest but pouchdb doesn't use it
                    // so we haven't hooked in that functionality yet.
                    Logger.getLogger(JsonXmlHTTPRequest.class.getName()).log(Level.SEVERE, null, e);
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    Logger.getLogger(JsonXmlHTTPRequest.class.getName()).log(Level.SEVERE, null, e);
                    throw new RuntimeException(e);
                }
            }
        });

        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        thread.start();
    }

    private static JSONObject getResponse(HttpURLConnection httpURLConnection) throws IOException {
        JSONObject responseObject = new JSONObject();
        responseObject.put("status", httpURLConnection.getResponseCode());
        JSONObject responseHeaderObject = new JSONObject();
        for(String headerName : httpURLConnection.getHeaderFields().keySet())
        {
            // The Null key is apparently used to record the status response line in httpURLConnection
            if (headerName != null)
            {
                responseHeaderObject.put(headerName, httpURLConnection.getHeaderField(headerName));
            }
        }
        responseObject.put("headers", responseHeaderObject);

        // TODO: This is wrong on multiple levels. First, it assumes the contents are a string. They could be binary.
        // second it assumes that the string's encoding is UTF-8 but in theory other encodings are possible which
        // typically should be encoded as an argument in the content-type header.
        // TODO: I actually have been too lazy to see if 3xx responses are treated as exceptions, seriously would it
        // kill the folks in Java land to have docs? Or are there docs that explain when exceptions are thrown for
        // httpurlconnection and I just haven't found them?
        InputStream theInputStream = httpURLConnection.getResponseCode() > 399 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        String responseText = Utilities.InputStreamOfCharsToString(theInputStream, "UTF-8");
        responseObject.put("responseText", responseText);
        return responseObject;
    }

    private static HttpURLConnection sendRequest(JSONObject jsonRequestObject, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        String method = jsonRequestObject.getString("method");
        httpURLConnection.setRequestMethod(method);
        JSONObject headers = jsonRequestObject.getJSONObject("headers");
        for(Object headerNameObject : headers.keySet())
        {
            String headerName = (String) headerNameObject;
            String headerValue = headers.getString(headerName);
            httpURLConnection.setRequestProperty(headerName, headerValue);
        }

        String requestText = jsonRequestObject.getString("requestText");
        if (requestText != null && requestText.length() > 0)
        {
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(requestText.getBytes("UTF-8"));
        }
        return httpURLConnection;
    }
}
