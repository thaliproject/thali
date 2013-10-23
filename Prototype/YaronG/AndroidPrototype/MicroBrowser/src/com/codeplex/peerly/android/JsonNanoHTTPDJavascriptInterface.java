package com.codeplex.peerly.android;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.codeplex.peerly.common.JsonNanonHTTPDJavascriptBridge;

import com.codeplex.peerly.org.json.JSONException;
import com.codeplex.peerly.org.json.JSONObject;

import java.io.IOException;

/**
 * Created by yarong on 9/20/13.
 */
public class JsonNanoHTTPDJavascriptInterface implements JsonNanonHTTPDJavascriptBridge {
    private WebView webView;
    private AndroidJsonNanoHTTPD server;

    public JsonNanoHTTPDJavascriptInterface(WebView webView) {
        this.webView = webView;
    }

    @Override
    @JavascriptInterface
    public boolean isHttpServerRunning() {
        if (server == null) {
            return false;
        }

        return server.isAlive();
    }

    @Override
    @JavascriptInterface
    public void startHttpServer(int port, String requestHandlerCallBack) {
        if (server != null) {
            throw new RuntimeException("The server is already running.");
        }

        server = new AndroidJsonNanoHTTPD(port, requestHandlerCallBack, webView);
        try {
            server.start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    @JavascriptInterface
    public void stopHttpServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    @JavascriptInterface
    public void setResponse(String responseJsonString) {
        try {
            server.SetResponse(new JSONObject(responseJsonString));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
