package com.codeplex.peerly.android;

import android.webkit.WebView;

import com.codeplex.peerly.common.JsonNanoHTTPD;
import com.codeplex.peerly.org.json.JSONObject;

/**
 * Created by yarong on 9/20/13.
 */
public class AndroidJsonNanoHTTPD extends JsonNanoHTTPD {
    private String requestCallBackName;
    private WebView webView;

    public AndroidJsonNanoHTTPD(int port, String requestCallBackName, WebView webview) {
        super(port);
        this.requestCallBackName = requestCallBackName;
        this.webView = webview;
    }

    @Override
    protected void deliverRequestJsonToJavascript(JSONObject jsonRequestObject) {
        final String javascriptUri = "javascript:" + requestCallBackName + "(" + JSONObject.quote(jsonRequestObject.toString()) + ");";

        // WebView does not like being called from anything but the UI thread
        webView.post(new Runnable() {
            public void run() {
                webView.loadUrl(javascriptUri);
            }
        });
    }
}
