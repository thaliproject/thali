package com.msopentech.thali.devicehub.universal;

import com.msopentech.thali.utilities.webviewbridge.BridgeManager;

/**
 * Created by yarong on 12/26/13.
 */
public class ThaliDeviceHubUx {
    protected final static String rootUxLocalPath = "/ThaliDeviceHubUx.html";
    public static String getRootUxHtmlAsString() {
        return BridgeManager.turnUTF8InputStreamToString(ThaliDeviceHubUx.class.getResourceAsStream(rootUxLocalPath));
    }
}
