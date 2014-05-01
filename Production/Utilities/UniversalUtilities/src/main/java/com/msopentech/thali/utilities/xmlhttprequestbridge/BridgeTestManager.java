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

package com.msopentech.thali.utilities.xmlhttprequestbridge;

import com.couchbase.lite.Context;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.webviewbridge.BridgeCallBack;
import com.msopentech.thali.utilities.webviewbridge.BridgeHandler;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;

public class BridgeTestManager {
    public static Object waitObject = new Object();
    public static String testHtml = "/xhrtest/test.html";
    public static String testJs = "/xhrtest/test.js";
    public enum pingStatus { unset, failed, success }
    public static pingStatus seenPing = pingStatus.unset;
    public ThaliListener thaliListenerStandardPort, thaliListenerPlusOnePort;

    public static class BridgeTest extends BridgeHandler {
        public BridgeTest() {
            super("Test");
            seenPing = pingStatus.unset;
        }

        @Override
        public void call(final String jsonString, final BridgeCallBack bridgeCallBack) {
            // We throw the processing on a different thread to test that we are reasonably thread safe
            new Thread(new Runnable() {
                @Override
                public void run() {
                    seenPing = jsonString.equals("\"Good\"") ? pingStatus.success : pingStatus.failed;
                    synchronized (waitObject) {
                        waitObject.notifyAll();
                    }
                }
            }).start();
        }
    }

    public static class LogHandler extends BridgeHandler {

        public LogHandler() {
            super("log");
        }

        @Override
        public void call(String jsonString, BridgeCallBack bridgeCallBack) {
            Log.e("xhrtest", jsonString);
        }
    }

    /**
     * Launches the test by running the test javascript that calls a bridge handler configured by this code. This
     * method is usually called inside of a context (like the Application start) that isn't directly visible to
     * JUnit. Hence we have the partner method, testResult, that can be called from JUnit that will block until the
     * test completes.
     * @param bridgeManager
     * @param createClientBuilder
     * @param  bridgeTestLoadHtml
     * @param  contextForStandardPort
     * @param  contextForPortPlusOne
     */
    public void launchTest(
            BridgeManager bridgeManager, CreateClientBuilder createClientBuilder,
            BridgeTestLoadHtml bridgeTestLoadHtml, Context contextForStandardPort, Context contextForPortPlusOne)
            throws InterruptedException {
        startServers(contextForStandardPort, contextForPortPlusOne);

        BridgeHandler xmlhttpBridge = new Bridge(contextForStandardPort.getFilesDir(), createClientBuilder);
        bridgeManager.register(xmlhttpBridge);

        BridgeHandler bridgeTestHandler = new BridgeTest();
        bridgeManager.register(bridgeTestHandler);

        BridgeHandler logHandler = new LogHandler();
        bridgeManager.register(logHandler);

        bridgeTestLoadHtml.LoadWebPage(getClass().getResource(testHtml).toExternalForm());
    }

    /**
     * See description of other launchTest. This one is for situations where the WebView already has a xmlhttpBridge
     * and has already loaded its web page. So we just run the test.js directly in the existing context.
     * @param bridgeManager
     * @param contextForStandardPort
     * @param contextForPortPlusOne
     * @throws InterruptedException
     */
    public void launchTest(BridgeManager bridgeManager, Context contextForStandardPort, Context contextForPortPlusOne)
            throws InterruptedException {
        startServers(contextForStandardPort, contextForPortPlusOne);

        BridgeHandler bridgeTestHandler = new BridgeTest();
        bridgeManager.register(bridgeTestHandler);

        BridgeHandler logHandler = new LogHandler();
        bridgeManager.register(logHandler);

        String testJsString =
                BridgeManager.turnUTF8InputStreamToString(getClass().getResourceAsStream(testJs));
        bridgeManager.executeJavascript(testJsString);
    }

    public boolean testResult() throws InterruptedException {
        synchronized(waitObject) {
            while(seenPing == pingStatus.unset) {
                waitObject.wait();
            }
        }
        thaliListenerStandardPort.stopServer();
        thaliListenerPlusOnePort.stopServer();
        return seenPing == pingStatus.success;
    }

    protected void startServers(Context contextForDefaultPort, Context contextForPlusOnePort) throws InterruptedException {
        thaliListenerStandardPort = new ThaliListener();
        thaliListenerStandardPort.startServer(contextForDefaultPort, ThaliListener.DefaultThaliDeviceHubPort);

        thaliListenerPlusOnePort = new ThaliListener();
        thaliListenerPlusOnePort.startServer(contextForPlusOnePort, ThaliListener.DefaultThaliDeviceHubPort + 1);

        // This is a poor man's synch solution to make sure the test doesn't start before the listeners are running.
        thaliListenerStandardPort.getSocketStatus();
        thaliListenerPlusOnePort.getSocketStatus();
    }
}
