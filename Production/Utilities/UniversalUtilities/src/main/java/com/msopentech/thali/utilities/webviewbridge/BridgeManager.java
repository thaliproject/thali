package com.msopentech.thali.utilities.webviewbridge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the infrastructure for the Webview Javascript Bridge that is common to both JavaFX and Android's WebViews.
 * This object is thread safe.
 */
public abstract class BridgeManager implements Bridge {
    private String managerNameInJavascript = "ThaliBridgeManager0";
    private String pathToBridgeManagerJs = "/BridgeManager.js";
    private boolean javascriptLoaded = false;

    protected ConcurrentHashMap<String, BridgeHandler> registeredHandlers = new ConcurrentHashMap<String, BridgeHandler>();

    /**
     * A useful utility for small streams. The stream will be closed by this method.
     *
     * Taken from http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
     * @param inputStream
     * @return Stream as a string
     */
    public static String turnUTF8InputStreamToString(InputStream inputStream) {
        try {
            Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * The manager will pass in the Javascript that controls the bridge on the browser side to this API and expect
     * whatever binding is needed to be used to load that Javascript inside the browser instance. I am using
     * a string because all the WebView frameworks support loading a string and the file is tiny so let's keep it
     * simple.
     * @param javascript
     */
    public abstract void loadManagerJavascript(String javascript);

    /**
     * However this method is implemented it MUST be thread safe.
     * @param handlerName
     *
     */
    public abstract void callBack(String handlerName, String jsonString);

    public void register(BridgeHandler bridgeHandler) {
        if (javascriptLoaded == false) {
            // We do this here rather than in a constructor because otherwise we get unpleasant race conditions
            // between the constructor called by super() from the inherited constructor and from this call back to
            // that inherited object. Nasty.
            loadManagerJavascript(turnUTF8InputStreamToString(getClass().getResourceAsStream(pathToBridgeManagerJs)));
        }

        if (registeredHandlers.putIfAbsent(bridgeHandler.getName(), bridgeHandler) != null) {
            throw new RuntimeException("Already have a handler registered with the given name");
        }
    }

    public void invokeHandler(String handlerName, String jsonString, String successHandlerName, String failureHandlerName) {
        BridgeCallBack bridgeCallBack = new BridgeCallBack(this, successHandlerName, failureHandlerName);

        BridgeHandler bridgeHandler = registeredHandlers.get(handlerName);

        if (bridgeHandler == null) {
            // We throw the error back to javascript so it can 'test' to see if certain interfaces have been
            // registered or not.
            bridgeCallBack.failureHandler("{\"failure\":\"No registered handler with the name" + handlerName +"\"}");
        }

        bridgeHandler.call(jsonString, bridgeCallBack);
    }

    /**
     * This is the variable name that will be bound to in Javascript to expose the bridge manager.
     * @return
     */
    public String getManagerNameInJavascript() {
        return this.managerNameInJavascript;
    }
}
