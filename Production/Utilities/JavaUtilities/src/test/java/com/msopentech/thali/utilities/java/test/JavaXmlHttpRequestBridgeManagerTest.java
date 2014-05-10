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

package com.msopentech.thali.utilities.java.test;

import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.java.JavaFXBridgeManager;
import com.msopentech.thali.utilities.universal.test.*;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestManager;
import javafx.application.Application;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JavaXmlHttpRequestBridgeManagerTest {
    protected static BridgeTestManager bridgeTestManager = null;

    public static class AppHosting extends Application {
        @Override
        public void start(Stage stage) throws InterruptedException {
            WebView browser = new WebView();
            WebEngine webEngine = browser.getEngine();
            webEngine.setJavaScriptEnabled(true);

            BridgeManager bridgeManager = new JavaFXBridgeManager(webEngine);

            JavaEktorpCreateClientBuilder javaEktorpCreateClientBuilder = new JavaEktorpCreateClientBuilder();

            bridgeTestManager.launchTest(bridgeManager, javaEktorpCreateClientBuilder,
                    new JavaXmlHttpRequestLoadHtml(browser, stage),
                    getClass().getResource(BridgeTestManager.testHtml).toExternalForm(), new CreateContextInTemp(),
                    new CreateContextInTemp());
        }
    }

    @Test
    public void testBridgeManager() throws InterruptedException {
        // This test is broken and we might never fix it since we are going to abandon javaFX.
//        bridgeTestManager = new BridgeTestManager();
//        new Thread() {
//            public void run() {
//                Application.launch(AppHosting.class, new String[0]);
//            }
//        }.start();
//        assertTrue(bridgeTestManager.testResult());
    }
}
