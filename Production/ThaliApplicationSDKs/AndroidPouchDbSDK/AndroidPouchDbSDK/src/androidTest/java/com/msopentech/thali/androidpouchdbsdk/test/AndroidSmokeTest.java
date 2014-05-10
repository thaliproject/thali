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

import android.content.*;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.*;
import com.msopentech.thali.androidpouchdbsdk.app.MainActivity;
import com.msopentech.thali.utilities.android.*;
import com.msopentech.thali.utilities.xmlhttprequestbridge.*;

import java.io.File;

public class AndroidSmokeTest extends ActivityInstrumentationTestCase2<MainActivity> implements BridgeTestLoadHtml {
    protected BridgeTestManager bridgeTestManager = null;

    public AndroidSmokeTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() {
        bridgeTestManager = new BridgeTestManager();
    }

    public void testSmoke() throws InterruptedException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            throw new RuntimeException("This test only runs in KitKat or higher!");
        }

        Context androidContext = getActivity().getApplicationContext();

        bridgeTestManager.launchTest(getActivity().bridgeManager, new AndroidEktorpCreateClientBuilder(), this,
                "file:///android_asset/xhrtest/test.html",
                new ContextInTempDirectory(androidContext), new ContextInTempDirectory(androidContext));
        assertTrue(bridgeTestManager.testResult());
    }

    @Override
    public void LoadWebPage(final String url) {
        if (getActivity().webView == null) {
            throw new RuntimeException("We got called before onCreate!");
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().webView.loadUrl(url);
            }
        });
    }
}
