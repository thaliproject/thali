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

import android.test.AndroidTestCase;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.relay.RelayWebServer;
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

    @Override
    public void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException, InterruptedException {

        CreateClientBuilder cb = new AndroidEktorpCreateClientBuilder();

        thaliListener = new ThaliListener();

        thaliListener.startServer(new AndroidContext(getContext()), 0, null);

        server = new RelayWebServer(cb, getContext().getFilesDir(), thaliListener.getHttpKeys());
        tempFileManager = new RelayWebServerTest.TestTempFileManager();

        server.start();
    }

    @Override
    public void tearDown() {

        tempFileManager._clear();

        if (server != null && server.isAlive())
            server.stop();

        if (thaliListener != null)
            thaliListener.stopServer();
    }
}
