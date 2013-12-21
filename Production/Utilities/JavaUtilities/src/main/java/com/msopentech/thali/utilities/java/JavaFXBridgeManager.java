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

package com.msopentech.thali.utilities.java;

import com.msopentech.thali.utilities.webviewbridge.Bridge;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public class JavaFXBridgeManager extends BridgeManager {
    protected WebEngine webEngine;

    public JavaFXBridgeManager(WebEngine webEngine) {
        assert webEngine != null;
        this.webEngine = webEngine;

        webEngine.setJavaScriptEnabled(true);
        JSObject jsObject = (JSObject) webEngine.executeScript("window");
        Bridge bridge = this;
        jsObject.setMember(this.getManagerNameInJavascript(), bridge);
    }

    @Override
    public void executeJavascript(final String javascript) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.executeScript(javascript);
            }
        });
    }
}
