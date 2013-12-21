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
 * The object passed in to Javascript. This is just meant as a protective wrapper around BridgeManager to prevent
 * any 'funny business' from inside of Javascript. This is really just paranoia since the only allowed Javascript
 * is Javascript we wrote ourselves!
 */
public interface Bridge {
    void invokeHandler(String handlerName, String jsonString, String successHandlerName, String failureHandlerName);
}
