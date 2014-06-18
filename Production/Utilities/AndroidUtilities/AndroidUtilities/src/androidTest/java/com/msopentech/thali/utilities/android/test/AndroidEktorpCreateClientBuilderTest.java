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

package com.msopentech.thali.utilities.android.test;

import android.test.AndroidTestCase;
import com.couchbase.lite.Context;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.test.ThaliTestEktorpClient;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;

import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;

public class AndroidEktorpCreateClientBuilderTest extends AndroidTestCase {
    private static ThaliTestEktorpClient testEktorpClient = null;

    @Override
    public void setUp() throws InterruptedException, UnrecoverableEntryException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {

        // TODO: The hard coding of the socks proxy and addresses, onion address, etc. are hacks until we put in
        // automated TOR Onion Proxy support. That will be in the next check in.

        /*
        Please go look at the detailed instructions in JavaUtilites\JavaEktorpCreateClientBuilderTest.java.
        You will use the Tor Onion Proxy running on the host machine. So make sure it is running. Please
        copy the SAME IP address and onion address below as you used in Java (assuming you are running your
        Android client with the same PC host). Also make sure to issue 'adb forward tcp:9600 tcp:9600'.
         */

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("192.168.1.188", 9150));

        //ThaliListener.DefaultThaliDeviceHubAddress

        if (testEktorpClient == null) {
            testEktorpClient = new ThaliTestEktorpClient(ThaliListener.DefaultThaliDeviceHubAddress, 9600,
                    "hrl5z4arnyciqsll.onion", 10000,
                    ThaliCryptoUtilities.DefaultPassPhrase, new AndroidContext(getContext()),
                    new AndroidEktorpCreateClientBuilder(), this.getClass(), proxy);

            //ThaliTestUtilities.turnCouchbaseLoggingTo11();
        }
        testEktorpClient.setUp();
    }

    @Override
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
