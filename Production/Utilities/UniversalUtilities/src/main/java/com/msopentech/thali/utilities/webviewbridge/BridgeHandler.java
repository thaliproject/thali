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
    public abstract void call(final String jsonString, final BridgeCallBack bridgeCallBack);

    /**
     * The name that the handler wants to be called in Javascript
     * @return
     */
    String getName() {
        return handlerName;
    }
}
