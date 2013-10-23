package com.codeplex.peerly.browsertest;

import com.codeplex.peerly.browser.JsonNanoHTTPDJavascriptInterface;
import com.codeplex.peerly.browser.LiveConnectJsonXmlHTTPRequest;
import com.codeplex.peerly.org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 10/7/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        JsonNanoHTTPDJavascriptInterface httpServerManager = null;
        int port = 8090;

        String proxyAddress = "127.0.0.1";
        int proxyPort = 8888;
        try {
            ServerJSObject testJSObject = new ServerJSObject();
            httpServerManager = new JsonNanoHTTPDJavascriptInterface(testJSObject);
            String callback = "foo";

            testJSObject.SetServer(httpServerManager, port, callback);
            httpServerManager.startHttpServer(8090, callback);

            String xmlHTTPCallBack = "bar";
            JSONObject request = new JSONObject();
            request.put("method", "GET");
            request.put("url", "http://127.0.0.1:" + port + "/foo/bar/blah");
            request.put("Content-Type", "Application/JSON");
            request.put("headers", new JSONObject());
            request.put("requestText", "");
            String requestAsString = request.toString();
            ClientJSObject clientJSObject = new ClientJSObject(requestAsString);
            LiveConnectJsonXmlHTTPRequest liveConnectJsonXmlHTTPRequest = new LiveConnectJsonXmlHTTPRequest(xmlHTTPCallBack, clientJSObject);
            liveConnectJsonXmlHTTPRequest.send(xmlHTTPCallBack, 23, request.toString(), proxyAddress, proxyPort);


            request = new JSONObject();
            request.put("method", "PUT");
            request.put("url", "http://127.0.0.1:" + port + "/foo");
            request.put("Content-Type", "Application/JSON");
            request.put("headers", new JSONObject());
            request.put("requestText", requestAsString);
            liveConnectJsonXmlHTTPRequest = new LiveConnectJsonXmlHTTPRequest(xmlHTTPCallBack, clientJSObject);
            liveConnectJsonXmlHTTPRequest.send(xmlHTTPCallBack, 24, request.toString(), proxyAddress, proxyPort);


            Thread.sleep(100000000);
        } finally {
            if (httpServerManager != null) {
                httpServerManager.stopHttpServer(port);
            }
        }
    }
}
