package com.msopentech.thali.utilities.webviewbridge;

/**
 * The object passed in to Javascript. This is just meant as a protective wrapper around BridgeManager to prevent
 * any 'funny business' from inside of Javascript. This is really just paranoia since the only allowed Javascript
 * is Javascript we wrote ourselves!
 */
public interface Bridge {
    void invokeHandler(String handlerName, String jsonString, String successHandlerName, String failureHandlerName);
}
