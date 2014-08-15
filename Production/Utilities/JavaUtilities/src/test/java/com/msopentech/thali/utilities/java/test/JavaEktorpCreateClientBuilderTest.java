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

package com.msopentech.thali.utilities.java.test;

import com.couchbase.lite.Context;
import com.couchbase.lite.CouchbaseLiteException;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyManager;
import com.msopentech.thali.local.utilities.CreateContextInTemp;
import com.msopentech.thali.toronionproxy.OnionProxyContext;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.test.ThaliTestEktorpClient;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;

public class JavaEktorpCreateClientBuilderTest extends TestCase {
    private static ThaliTestEktorpClient testEktorpClient = null;

    public void setUp() throws IOException, InterruptedException, KeyManagementException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException {
        //ThaliTestUtilities.outputAsMuchLoggingAsPossible();
        //ThaliTestUtilities.configuringLoggingApacheClient();

        // I have to create a single global listener for all tests (which is really a mess in terms of bring sure
        // where bugs come from) because of https://github.com/couchbase/couchbase-lite-java-listener/issues/43
        File torDirectory = Files.createTempDirectory("Tor OP").toFile();
        if (testEktorpClient == null) {
            testEktorpClient = new ThaliTestEktorpClient(ThaliListener.DefaultThaliDeviceHubAddress, 0,
                    ThaliCryptoUtilities.DefaultPassPhrase, new CreateContextInTemp(),
                    new JavaEktorpCreateClientBuilder(), this.getClass(),
                    new JavaOnionProxyManager(new JavaOnionProxyContext(torDirectory)));

            //ThaliTestUtilities.turnCouchbaseLoggingTo11();
        }

        testEktorpClient.setUp();
    }

    public void tearDown() {
        testEktorpClient.tearDown();
    }

    public void testPullReplication() throws InterruptedException, NoSuchAlgorithmException, CouchbaseLiteException,
            URISyntaxException, IOException, InvalidKeySpecException, KeyManagementException,
            UnrecoverableEntryException, KeyStoreException {
        testEktorpClient.testPullReplication();
    }

    public void testPushReplication() throws IOException, NoSuchAlgorithmException, URISyntaxException,
            UnrecoverableEntryException, InterruptedException, CouchbaseLiteException, KeyStoreException,
            InvalidKeySpecException, KeyManagementException {
        testEktorpClient.testPushReplication();
    }

    public void testRetrieve() throws InterruptedException, NoSuchAlgorithmException, IOException,
            KeyManagementException, KeyStoreException, UnrecoverableEntryException, InvalidKeySpecException {
        testEktorpClient.testRetrieve();
    }
}
