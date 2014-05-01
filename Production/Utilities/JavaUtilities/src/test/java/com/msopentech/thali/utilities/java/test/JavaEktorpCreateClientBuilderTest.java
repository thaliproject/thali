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

import com.couchbase.lite.CouchbaseLiteException;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.test.ThaliTestEktorpClient;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;

public class JavaEktorpCreateClientBuilderTest {
    private final boolean debugApache = true;
    private static ThaliTestEktorpClient testEktorpClient = null;

    @Before
    public void setUp() throws IOException, InterruptedException, KeyManagementException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException {
        if (debugApache) {
            ThaliTestUtilities.configuringLoggingApacheClient();
        }

        // I have to create a new DeleteMe() on each test run due to https://github.com/couchbase/couchbase-lite-java-listener/issues/43
        if (testEktorpClient == null) {
            testEktorpClient = new ThaliTestEktorpClient(ThaliListener.DefaultThaliDeviceHubAddress,
                    ThaliCryptoUtilities.DefaultPassPhrase, new CreateContextInTemp(),
                    new JavaEktorpCreateClientBuilder(), this.getClass());
        }

        testEktorpClient.setUp();
    }

    @After
    public void tearDown() {
        testEktorpClient.tearDown();
    }

    @Test
    public void testPullReplication() throws InterruptedException, NoSuchAlgorithmException, CouchbaseLiteException,
            URISyntaxException, MalformedURLException, InvalidKeySpecException {
        testEktorpClient.testPullReplication();
    }

    @Test
    public void testPushReplication() throws IOException, NoSuchAlgorithmException, URISyntaxException,
            UnrecoverableEntryException, InterruptedException, CouchbaseLiteException, KeyStoreException,
            InvalidKeySpecException, KeyManagementException {
        testEktorpClient.testPushReplication();
    }

    @Test
    public void testRetrieve() throws InterruptedException, NoSuchAlgorithmException, IOException,
            KeyManagementException, KeyStoreException, UnrecoverableEntryException, InvalidKeySpecException {
        testEktorpClient.testRetrieve();
    }
}
