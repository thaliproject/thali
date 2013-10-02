package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonXmlHTTPRequest;
import com.codeplex.peerly.org.json.JSONObject;
import netscape.javascript.JSObject;

public class LiveConnectJsonXmlHTTPRequest extends JsonXmlHTTPRequest {
    private String responseCallBackName;
    private JSObject window;

    public LiveConnectJsonXmlHTTPRequest(String responseCallBackName, JSObject window) {
        super();
        this.responseCallBackName = responseCallBackName;
        this.window = window;
    }

    @Override
    public void sendResponse(String javascriptCallBackMethodName, int key, JSONObject responseObject) {
        window.call(responseCallBackName, new Object[] { (Object) key, (Object) responseObject.toString() });
    }
}
