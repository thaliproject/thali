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

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestLoadHtml;
import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Android does not like Activities to live in the test part of the project so I have to define it here
 * even though it is for testing only.
 */
public class AndroidXmlHttpRequestTestActivity extends Activity implements BridgeTestLoadHtml {
    protected BridgeManager bridgeManager;
    protected WebView webView;

    @Override
    public void LoadWebPage(final String url) {
        readResourceIntoDirectory(BridgeManager.pathToBridgeManagerJs);

        try {
            loadManifestFilesIntoDirectory("/xhrtest/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final File testHtmlFile = new File(getFilesDir(), "/xhrtest/test.html");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(testHtmlFile.toURI().toString());
            }
        });
    }

    private void loadManifestFilesIntoDirectory(String resourceDirectoryPath) throws IOException {
        File directoryAtDestination = new File(getFilesDir(), resourceDirectoryPath);
        if (directoryAtDestination.exists()) {
            FileUtils.deleteDirectory(directoryAtDestination);
        }
        if (directoryAtDestination.mkdirs() == false) {
            throw new RuntimeException("Couldn't create destination!");
        }

        final String manifestFileName = "manifest.csv";
        String[] files = IOUtils.toString(getClass().getResourceAsStream(resourceDirectoryPath + manifestFileName)).split(",");
        for(String fileName : files) {
            readResourceIntoDirectory(resourceDirectoryPath + fileName);
        }
    }

    /**
     * Reads in the resource with the specified resourcePath and then creates a file
     * with the same path in the application's local storage
     * @param resourcePath
     */
    private void readResourceIntoDirectory(String resourcePath) {
        File resourceFile = new File(getFilesDir(), resourcePath);

        InputStream resourceInputStream = getClass().getResourceAsStream(resourcePath);

        FileOutputStream resourceFileOutputStream = null;
        try {
            resourceFileOutputStream = new FileOutputStream(resourceFile);
            IOUtils.copy(resourceInputStream, resourceFileOutputStream);
            resourceFileOutputStream.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (resourceInputStream != null) {
                    resourceInputStream.close();
                }
                if (resourceFileOutputStream != null) {
                    resourceFileOutputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        //TODO: Oh, this is all a huge security hole.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("xmlhttptest",
                        "errorCode: " + errorCode + ", description: " + description + ", failingUrl: " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.e("xmlhttptest", consoleMessage.message() + " - " + consoleMessage.messageLevel().toString() + " - "
                        + consoleMessage.lineNumber() + " - " + consoleMessage.sourceId());
                return false;
            }
        });

        bridgeManager = new AndroidBridgeManager(this, webView);
    }

    public void runTest(BridgeTestManager bridgeTestManager, Context androidContext) throws InterruptedException {
        bridgeTestManager.launchTest(bridgeManager, new AndroidEktorpCreateClientBuilder(), this,
                getClass().getResource(BridgeTestManager.testHtml).toExternalForm(),
                new ContextInTempDirectory(androidContext), new ContextInTempDirectory(androidContext));
    }
}
