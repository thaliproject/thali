package com.codeplex.peerly.android;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.codeplex.peerly.common.JsonXmlHTTPRequestJavascriptBridge;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 10/2/13
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class JsonXmlHTTPRequestJavascriptInterface implements JsonXmlHTTPRequestJavascriptBridge {
    public WebView webview;

    public JsonXmlHTTPRequestJavascriptInterface(WebView webView) {
        this.webview = webView;
    }

    @Override
    @JavascriptInterface
    public void sendJsonXmlHTTPRequest(String javascriptCallBackMethodName, int key, String requestJsonString) {
        AndroidJsonXMLHttpRequest androidJsonXMLHttpRequest = new AndroidJsonXMLHttpRequest(webview);
        androidJsonXMLHttpRequest.send(javascriptCallBackMethodName, key, requestJsonString);
    }
}
