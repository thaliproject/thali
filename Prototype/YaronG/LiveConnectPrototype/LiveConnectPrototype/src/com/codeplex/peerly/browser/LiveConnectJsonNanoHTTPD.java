/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonNanoHTTPD;
import netscape.javascript.JSObject;
import com.codeplex.peerly.org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author yarong
 */
public class LiveConnectJsonNanoHTTPD extends JsonNanoHTTPD
{
    private String requestCallBackName;
    private JSObject window;

    public LiveConnectJsonNanoHTTPD(int port, String requestCallBackName, JSObject window) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        super(port, null);
        this.requestCallBackName = requestCallBackName;
        this.window = window;
    }

    @Override
    protected void deliverRequestJsonToJavascript(JSONObject jsonRequestObject)
    {
        window.call(requestCallBackName, new Object[] {jsonRequestObject.toString()});
    }
}
