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
