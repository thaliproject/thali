package com.msopentech.thali.utilities.java;

import com.msopentech.thali.utilities.webviewbridge.Bridge;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

/**
 * Created by yarong on 12/12/13.
 */
public class JavaFXBridgeManager extends BridgeManager {
    protected WebEngine webEngine;
    protected String callbackManager = "window.thali_callback_manager";

    public JavaFXBridgeManager(WebEngine webEngine) {
        assert webEngine != null;
        this.webEngine = webEngine;

        webEngine.setJavaScriptEnabled(true);
        JSObject jsObject = (JSObject) webEngine.executeScript("window");
        Bridge bridge = this;
        jsObject.setMember(this.getManagerNameInJavascript(), bridge);
    }

    @Override
    public void loadManagerJavascript(String javascript) {
        webEngine.executeScript(javascript);
    }

    @Override
    public void callBack(String handlerName, String jsonString) {
        String functionCall = callbackManager + "[\"" + handlerName + "\"]('" + jsonString + "')";
        webEngine.executeScript(functionCall);
    }
}
