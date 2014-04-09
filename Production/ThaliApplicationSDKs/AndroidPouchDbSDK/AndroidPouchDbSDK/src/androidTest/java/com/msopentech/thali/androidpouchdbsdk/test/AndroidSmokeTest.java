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

package com.msopentech.thali.androidpouchdbsdk.test;

import android.test.ActivityInstrumentationTestCase2;
import com.couchbase.lite.JavaContext;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.androidpouchdbsdk.app.MainActivity;
import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestManager;

import java.io.File;

public class AndroidSmokeTest extends ActivityInstrumentationTestCase2<MainActivity> {
    protected BridgeTestManager bridgeTestManager = null;

    public AndroidSmokeTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() {
        bridgeTestManager = new BridgeTestManager();
    }

    public void testSmoke() throws InterruptedException {
        bridgeTestManager.launchTest(
                getActivity().bridgeManager, new AndroidContext(getActivity().getApplicationContext()));
        assertTrue(bridgeTestManager.testResult());
    }
}
