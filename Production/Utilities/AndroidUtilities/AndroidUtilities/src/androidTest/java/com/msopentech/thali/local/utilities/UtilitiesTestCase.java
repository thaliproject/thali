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

package com.msopentech.thali.local.utilities;

import android.content.Context;
import android.test.AndroidTestCase;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.toronionproxy.FileUtilities;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.test.RelayWebServerTest;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

public class UtilitiesTestCase extends AndroidTestCase {
    protected RelayWebServer server;
    protected RelayWebServerTest.TestTempFileManager tempFileManager;
    protected ThaliListener thaliListener;
    // This listener is used by a relay test to make sure that we can get a server key even from a server
    // we are not configured to be trusted by.
    protected ThaliListener noSecurityThaliListener;

    @Override
    public void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException, InterruptedException {

        CreateClientBuilder cb = new AndroidEktorpCreateClientBuilder();

        AndroidContextChangeFilesDir thaliListenerContext =
                new AndroidContextChangeFilesDir(getContext(), "thaliListener");
        AndroidOnionProxyManager androidOnionProxyManager =
                new AndroidOnionProxyManager(thaliListenerContext, "androidOnionProxyManager");
        thaliListener = new ThaliListener();
        thaliListener.startServer(new AndroidContext(thaliListenerContext), 0, androidOnionProxyManager);
        // Makes sure the test only runs once the server is up and running.
        thaliListener.waitTillHiddenServiceStarts();

        AndroidContextChangeFilesDir noSecurityThaliListenerContext =
                new AndroidContextChangeFilesDir(getContext(), "noSecurityThaliListener");
        AndroidOnionProxyManager androidOnionProxyManagerNoSecurity =
                new AndroidOnionProxyManager(noSecurityThaliListenerContext, "androidNoSecurityOnionProxyManager");
        noSecurityThaliListener = new ThaliListener();
        noSecurityThaliListener.startServer(
                new AndroidContext(noSecurityThaliListenerContext), 0, androidOnionProxyManagerNoSecurity);
        // Makes sure the test only runs once the server is up and running.
        noSecurityThaliListener.waitTillHiddenServiceStarts();

        server = new RelayWebServer(cb, getContext().getFilesDir(), thaliListener.getHttpKeys());
        tempFileManager = new RelayWebServerTest.TestTempFileManager();

        server.start();
    }

    @Override
    public void tearDown() {

        tempFileManager._clear();

        if (server != null && server.isAlive()) {
            server.stop();
        }

        if (thaliListener != null) {
            thaliListener.stopServer();
        }

        if (noSecurityThaliListener != null) {
            noSecurityThaliListener.stopServer();
        }
    }
}
