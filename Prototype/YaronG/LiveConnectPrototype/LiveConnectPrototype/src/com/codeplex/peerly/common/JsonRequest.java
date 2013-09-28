package com.codeplex.peerly.common;

import com.codeplex.peerly.org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

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
public abstract class JsonRequest {

    abstract public void sendResponse(String javascriptCallBackMethodName, int key, String responseJsonString);

    public void send(String javascriptCallBackMethodName, int key, String requestJsonString)
    {
        final String finalJavascriptCallBackMethodName = javascriptCallBackMethodName;
        final int finalKey = key;
        final String finalRequestJsonString = requestJsonString;
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject(finalRequestJsonString);
                String urlString = jsonObject.getString("url");
                try {
                    HttpURLConnection httpURLConnection = sendRequest(jsonObject, urlString);

                    JSONObject responseObject = getResponse(httpURLConnection);

                    sendResponse(finalJavascriptCallBackMethodName, finalKey, responseObject.toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    // TODO: Interesting enough there is an error handler for xmlhttprequest but pouchdb doesn't use it
                    // so we haven't hooked in that functionality yet.
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private JSONObject getResponse(HttpURLConnection httpURLConnection) throws IOException {
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

        String contentLengthString = httpURLConnection.getHeaderField("content-length");
        int contentLength = contentLengthString == null ? 0 : Integer.parseInt(contentLengthString);
        String contentType = httpURLConnection.getHeaderField("content-type");

        if (contentLength > 0 && contentType.equalsIgnoreCase("Application/JSON") == false)
        {
            // TODO: We really need logging support
            throw new RuntimeException("We only support JSON (and nobody can see this exception since it is on its own thread)");
        }

        String responseText = Utilities.StringifyInputStream(contentLength, httpURLConnection.getInputStream());
        responseObject.put("responseText", responseText);
        return responseObject;
    }

    private static HttpURLConnection sendRequest(JSONObject jsonObject, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        String method = jsonObject.getString("method");
        httpURLConnection.setRequestMethod(method);
        JSONObject headers = jsonObject.getJSONObject("headers");
        for(Object headerNameObject : headers.keySet())
        {
            String headerName = (String) headerNameObject;
            String headerValue = jsonObject.getString(headerName);
            httpURLConnection.setRequestProperty(headerName, headerValue);
        }
        // TODO: We don't support transfer encodings, this will stop a server from sending one
        httpURLConnection.setRequestProperty("accept-encoding", "identity");

        String requestText = jsonObject.getString("requestText");
        if (requestText != null)
        {
            httpURLConnection.setDoOutput(true);
            OutputStream out = httpURLConnection.getOutputStream();
            out.write(requestText.getBytes("UTF-8"));
        }
        return httpURLConnection;
    }
}
