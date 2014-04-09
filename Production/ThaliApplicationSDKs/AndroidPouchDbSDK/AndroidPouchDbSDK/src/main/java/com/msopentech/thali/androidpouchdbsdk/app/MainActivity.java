package com.msopentech.thali.androidpouchdbsdk.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.android.AndroidBridgeManager;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.webviewbridge.BridgeHandler;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import com.msopentech.thali.utilities.xmlhttprequestbridge.Bridge;
import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestManager;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    public static final String pathToIndexHtml = "file:///android_asset/index.html";
    public static final String PouchSDKTag = "PouchSDK";

    public BridgeManager bridgeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webView = (WebView) findViewById(R.id.webview);

        bridgeManager = new AndroidBridgeManager(this, webView);
        BridgeHandler xmlhttpBridge = new Bridge(getFilesDir(), new AndroidEktorpCreateClientBuilder());
        bridgeManager.register(xmlhttpBridge);

        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setBlockNetworkImage(false);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);

        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(PouchSDKTag,
                        "errorCode: " + errorCode + ", description: " + description + ", failingUrl: " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e(PouchSDKTag, consoleMessage.message() + " - " + consoleMessage.messageLevel().toString() + " - "
                        + consoleMessage.lineNumber() + " - " + consoleMessage.sourceId());
                return false;
            }
        });

        webView.loadUrl(pathToIndexHtml);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
