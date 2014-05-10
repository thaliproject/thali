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


package com.msopentech.thali.utilities.webviewbridge;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the infrastructure for the Webview Javascript Bridge that is common to both JavaFX and Android's WebViews.
 * This object is thread safe.
 */
public abstract class BridgeManager implements Bridge {
    protected String callbackManager = "window.thali_callback_manager";
    private String managerNameInJavascript = "ThaliBridgeManager0";
    public static final String pathToBridgeManagerJs = "/BridgeManager.js";

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
     * Executes the submitted Javascript string in the associated WebView. Note, the string is expected to be a
     * .js file.
     * @param javascript
     */
    public abstract void executeJavascript(final String javascript);

    /**
     * However this method is implemented it MUST be thread safe.
     * @param handlerName
     *
     */
    public void callBack(String handlerName, String jsonString) {
        String functionCall = callbackManager + "[\"" + handlerName + "\"]('" + StringEscapeUtils.escapeEcmaScript(jsonString) + "')";
        this.executeJavascript(functionCall);
    }

    public void register(BridgeHandler bridgeHandler) {
        if (registeredHandlers.putIfAbsent(bridgeHandler.getName(), bridgeHandler) != null) {
            throw new RuntimeException("Already have a handler registered with the given name");
        }
    }

    public void registerIfNameNotTaken(BridgeHandler bridgeHandler) {
        registeredHandlers.putIfAbsent(bridgeHandler.getName(), bridgeHandler);
    }

    /**
     * The method that will be called by the Bridge framework from Javascript. E.g. someone calls to the bridge in
     * Javascript and the bridge then marshals the call and calls across the bridge to this method.
     * @param handlerName
     * @param jsonString
     * @param successHandlerName
     * @param failureHandlerName
     */
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
