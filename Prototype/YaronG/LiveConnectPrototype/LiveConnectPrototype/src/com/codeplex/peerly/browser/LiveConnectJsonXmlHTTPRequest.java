package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonXmlHTTPRequest;
import com.codeplex.peerly.org.json.JSONObject;
import netscape.javascript.JSObject;

public class LiveConnectJsonXmlHTTPRequest extends JsonXmlHTTPRequest {
    private String peerlyXMLHttpRequestManagerObjectName;
    private JSObject window;

    public LiveConnectJsonXmlHTTPRequest(String peerlyXMLHttpRequestManagerObjectName, JSObject window) {
        super();
        this.peerlyXMLHttpRequestManagerObjectName = peerlyXMLHttpRequestManagerObjectName;
        this.window = window;
    }

    @Override
    public void sendResponse(String peerlyXMLHttpRequestManagerObjectName, int key, JSONObject responseObject) {
        window.eval(peerlyXMLHttpRequestManagerObjectName + ".receive(" + key + "," + JSONObject.quote(responseObject.toString()) + ");");
    }
}
