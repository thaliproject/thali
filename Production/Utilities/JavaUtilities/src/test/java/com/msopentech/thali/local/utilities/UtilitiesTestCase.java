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

import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.test.RelayWebServerTest;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

public class UtilitiesTestCase extends TestCase {
    protected static RelayWebServer server;
    protected static RelayWebServerTest.TestTempFileManager tempFileManager;
    protected static ThaliListener thaliListener;
    // This listener is used by a relay test to make sure that we can get a server key even from a server
    // we are not configured to be trusted by.
    protected static ThaliListener noSecurityThaliListener;
    protected static boolean configRan = false;

    @Override
    public void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException, InterruptedException {

        if (configRan == false) {
            CreateClientBuilder cb = new JavaEktorpCreateClientBuilder();

            thaliListener = new ThaliListener();
            thaliListener.startServer(new CreateContextInTemp(), 0, null);

            noSecurityThaliListener = new ThaliListener();
            noSecurityThaliListener.startServer(new CreateContextInTemp(), 0, null);

            server = new RelayWebServer(cb, new File(System.getProperty("user.dir")), thaliListener.getHttpKeys());
            tempFileManager = new RelayWebServerTest.TestTempFileManager();

            server.start();

            configRan = true;
        }
    }

    public void testNothing() {
        //Avoid no tests found assertion failed error.
    }
}
