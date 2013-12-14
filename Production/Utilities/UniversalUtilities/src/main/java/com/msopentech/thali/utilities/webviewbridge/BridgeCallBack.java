package com.msopentech.thali.utilities.webviewbridge;

/**
 * A BridgeHandler will be passed in a BridgeCallBack object if the Javascript requester has asked for a response
 */
public class BridgeCallBack {
    private BridgeManager bridgeManager;
    private String successHandlerName, failureHandlerName;

    public BridgeCallBack(BridgeManager bridgeManager, String successHandlerName, String failureHandlerName) {
        this.bridgeManager = bridgeManager;
        this.successHandlerName = successHandlerName;
        this.failureHandlerName = failureHandlerName;
    }

    public void successHandler(String jsonString) {
        bridgeManager.callBack(successHandlerName, jsonString);
    }

    public void failureHandler(String jsonString) {
        bridgeManager.callBack(failureHandlerName, jsonString);
    }
}
