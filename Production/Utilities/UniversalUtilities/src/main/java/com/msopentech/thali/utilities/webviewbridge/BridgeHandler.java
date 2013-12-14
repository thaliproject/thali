package com.msopentech.thali.utilities.webviewbridge;

/**
 * Defines the method called when a registered class is called from Javascript in a WebView.
 */
public abstract class BridgeHandler {
    protected String handlerName;

    public BridgeHandler(String name) {
        if (name == null || "".equals(name)) {
            throw new RuntimeException("Illegal name");
        }
        this.handlerName = name;
    }

    /**
     * If the registered object is sent a request then this method is called. The WebView will be blocked until
     * this method returns. So if any non-trivial work is to happen it's best to spawn it off on a separate thread
     * and return this thread immediately. The callback is asynchronous and multi-thread safe so you can send back
     * the response when it's ready without blocking anything.
     * @param jsonString Argument passed in from Javascript
     * @param bridgeCallBack If a callback was requested then a callback object will be sent, otherwise this will be NULL
     */
    public abstract void call(String jsonString, BridgeCallBack bridgeCallBack);

    /**
     * The name that the handler wants to be called in Javascript
     * @return
     */
    String getName() {
        return handlerName;
    }
}
