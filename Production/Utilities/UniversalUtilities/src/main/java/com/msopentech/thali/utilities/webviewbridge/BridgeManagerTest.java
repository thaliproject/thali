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

package com.msopentech.thali.utilities.webviewbridge;

/**
 * This is common code to be used by Android and JavaFX for testing purposes.
 */
public class BridgeManagerTest {
    public static Object waitObject = new Object();
    public static String bridgeHandlerTestJs = "/BridgeHandlerTest.js";
    public enum pingStatus { unset, failed, success }
    public static pingStatus seenPing2 = pingStatus.unset;

    public static class BridgeTest extends BridgeHandler {
        public BridgeTest() {
            super("Test");
            seenPing2 = pingStatus.unset;
        }

        @Override
        public void call(final String jsonString, final BridgeCallBack bridgeCallBack) {
            // We throw the processing on a different thread to test that we are reasonably thread safe
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (jsonString.equals("\"Ping\"")) {
                        bridgeCallBack.successHandler("\"Pong\"");
                        return;
                    }

                    if (jsonString.equals("\"Ping1\"")) {
                        bridgeCallBack.failureHandler("\"Pong1\"");
                        return;
                    }

                    if (jsonString.equals("\"Ping2\"")) {
                        seenPing2 = pingStatus.success;
                        synchronized (waitObject) {
                            waitObject.notifyAll();
                        }
                        return;
                    }

                    throw new RuntimeException("Unrecognized call value: " + jsonString);
                }
            }).start();
        }
    }

    /**
     * Launches the test by running the test javascript that calls a bridge handler configured by this code. This
     * method is usually called inside of a context (like the Application start) that isn't directly visible to
     * JUnit. Hence we have the partner method, testResult, that can be called from JUnit that will block until the
     * test completes.
     * @param bridgeManager
     */
    public void launchTest(BridgeManager bridgeManager) {
        BridgeHandler bridgeTest = new BridgeTest();
        bridgeManager.register(bridgeTest);
        String bridgeManagerJs =
                BridgeManager.turnUTF8InputStreamToString(getClass().getResourceAsStream(BridgeManager.pathToBridgeManagerJs));
        bridgeManager.executeJavascript(bridgeManagerJs);
        String testJavaScript =
                BridgeManager.turnUTF8InputStreamToString(getClass().getResourceAsStream(bridgeHandlerTestJs));
        bridgeManager.executeJavascript(testJavaScript);
    }

    /**
     * See launchTest.
     * @return
     * @throws InterruptedException
     */
    public boolean testResult() throws InterruptedException {
        synchronized (waitObject) {
            while(seenPing2 == pingStatus.unset) {
                waitObject.wait();
            }
        }

        return seenPing2 == pingStatus.success;
    }
}
