package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonXmlHTTPRequest;
import com.codeplex.peerly.org.json.JSONObject;
import netscape.javascript.JSObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LiveConnectJsonXmlHTTPRequest extends JsonXmlHTTPRequest {
    private String peerlyXMLHttpRequestManagerObjectName;
    private JSObject window;
    private static Object lockObject = new Object();

    public LiveConnectJsonXmlHTTPRequest(String peerlyXMLHttpRequestManagerObjectName, JSObject window) {
        super();
        this.peerlyXMLHttpRequestManagerObjectName = peerlyXMLHttpRequestManagerObjectName;
        this.window = window;
    }

    @Override
    public void sendResponse(String peerlyXMLHttpRequestManagerObjectName, int key, JSONObject responseObject) {
        String quotedJsonString = JSONObject.quote(responseObject.toString());
        // I have run into repeated issues with pipelined methods like GET where the applet will throw an exception
        // rather than allow for multiple simultaneous eval calls. This is bizarre as the Oracle docs say
        // liveconnect is supposed to be thread safe. My suspicion is that there is more here than I know but
        // for now I'm using this hammer of an approach of synchronizing the eval call so no one else from
        // XMLHTTPRequest will make simultaneous eval calls.
        // TODO: If this approach is really necessary then we need a global lock object to be used by *anyone* (including
        // LiveConnectJsonNanoHTTPD) from calling into the applet while we have an outstanding call.
        //synchronized (lockObject) {
        window.eval("window." + peerlyXMLHttpRequestManagerObjectName + ".receive(" + key + "," + quotedJsonString + ");");
        //}
    }
}
