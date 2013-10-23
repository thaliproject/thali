/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codeplex.peerly.browser;

import com.codeplex.peerly.common.JsonXmlHTTPRequestJavascriptBridge;
import netscape.javascript.JSObject;

import java.applet.Applet;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author yarong
 */
public class PeerlyApplet extends Applet implements JsonXmlHTTPRequestJavascriptBridge {
    private JsonNanoHTTPDJavascriptInterface jsonNanoHTTPDJavascriptInterface = null;

    /**
     * Returns the managed for HTTP servers
     * <p/>
     * We need this method because the JSObject is not properly formed when the applet's constructor is run so
     * if we try to get the window either in the constructor or on a class field we end up throwing an exception.
     * Therefore we have to wait until we get called from Javascript to be sure we have a good JSObject to get
     * the window from.
     *
     * @return
     */
    public JsonNanoHTTPDJavascriptInterface getJsonNanoHTTPDJavascriptInterface() {
        if (this.jsonNanoHTTPDJavascriptInterface == null) {
            this.jsonNanoHTTPDJavascriptInterface = new JsonNanoHTTPDJavascriptInterface(JSObject.getWindow(this));
        }
        return this.jsonNanoHTTPDJavascriptInterface;
    }

    @Override
    public void sendJsonXmlHTTPRequest(final String peerlyXMLHttpRequestManagerObjectName, final int key, final String requestJsonString, final String proxyIPorDNS, final int proxyPort) {
        final JSObject finalWindow = JSObject.getWindow(this);

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                LiveConnectJsonXmlHTTPRequest liveConnectJsonXmlHTTPRequest = new LiveConnectJsonXmlHTTPRequest(peerlyXMLHttpRequestManagerObjectName, finalWindow);
                liveConnectJsonXmlHTTPRequest.send(peerlyXMLHttpRequestManagerObjectName, key, requestJsonString, proxyIPorDNS, proxyPort);
                return null;
            }
        });
    }
}

