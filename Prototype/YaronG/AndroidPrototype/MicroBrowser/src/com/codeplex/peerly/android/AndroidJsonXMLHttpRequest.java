package com.codeplex.peerly.android;

import android.webkit.WebView;
import com.codeplex.peerly.common.JsonXmlHTTPRequest;
import com.codeplex.peerly.org.json.JSONObject;

public class AndroidJsonXMLHttpRequest extends JsonXmlHTTPRequest {
    private WebView webView;

    public AndroidJsonXMLHttpRequest(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void sendResponse(String peerlyXMLHttpRequestManagerObjectName, int key, JSONObject responseObject) {
        final String javascriptUri = "javascript:" + peerlyXMLHttpRequestManagerObjectName + ".receive(" + key + "," + JSONObject.quote(responseObject.toString()) + ");";
        webView.post(new Runnable() {
            public void run() {
                webView.loadUrl(javascriptUri);
            }
        });
    }
}
