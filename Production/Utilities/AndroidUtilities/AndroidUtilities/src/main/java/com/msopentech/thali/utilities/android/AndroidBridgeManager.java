/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package com.msopentech.thali.utilities.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.msopentech.thali.utilities.webviewbridge.Bridge;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;

public class AndroidBridgeManager extends BridgeManager implements Bridge {
    protected WebView webview;
    protected Activity activity;
    protected static String mimeTypeForScript = "text/javascript";
    protected static String encodingTypeForScript = "utf-8";

    @SuppressLint("SetJavaScriptEnabled")
    public AndroidBridgeManager(Activity activity, WebView webView) {
        assert activity != null && webView != null;
        this.activity = activity;
        webview = webView;
        webview.getSettings().setJavaScriptEnabled(true);
        Bridge bridge = this;
        webview.addJavascriptInterface(bridge, this.getManagerNameInJavascript());
        // Objects added via addJavascriptInterface only show up if the webview is reloaded
        webView.loadData("", "text/html", null);
    }

    @Override
    @JavascriptInterface
    public void invokeHandler(String handlerName, String jsonString, String successHandlerName, String failureHandlerName) {
        super.invokeHandler(handlerName, jsonString, successHandlerName, failureHandlerName);
    }

    protected String CreateJavascriptUrl(String javaScriptString) {
        return "javascript:(function() {" + javaScriptString + "})()\n";
    }

    @Override
    public void executeJavascript(final String javascriptFileString) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String dataURL = CreateJavascriptUrl(javascriptFileString);
                webview.loadUrl(dataURL);
            }
        });
    }
}
