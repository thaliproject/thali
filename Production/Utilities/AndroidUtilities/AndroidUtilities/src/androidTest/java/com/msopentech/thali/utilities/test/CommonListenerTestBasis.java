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

package com.msopentech.thali.utilities.test;

import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.local.utilities.UtilitiesTestCase;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliClientToDeviceHubUtilities;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.test.ConfigureRequestObjects;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

/**
 * Because of a bug in TJWS in Java we can't stop listeners created during testing. So we have to create one
 * set and share them everywhere. That's where this class comes from.
 */
public class CommonListenerTestBasis extends UtilitiesTestCase {
    // Host and port for the relay
    // These go away with https://github.com/thaliproject/ThaliHTML5ApplicationFramework/issues/4
    public static final String relayHost = "127.0.0.1";
    public static final int relayPort = 58500;

    // We have to make these values static because in Java we can't restart the serv
    protected static RelayWebServer server;
    protected static RelayWebServerTest.TestTempFileManager tempFileManager;
    protected static ThaliListener thaliListener;
    public static ConfigureRequestObjects configureRequestObjects = null; // for thaliListener

    protected static ThaliListener secondThaliListener;
    public static ConfigureRequestObjects secondConfigureRequestObjects = null; // for secondThaliListener

    // This listener is used by a relay test to make sure that we can get a server key even from a server
    // we are not configured to be trusted by.
    protected static ThaliListener noSecurityThaliListener;
    protected static boolean configRan = false;

    @Override
    public void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException, InterruptedException {

        if (OsData.getOsType() == OsData.OsType.Android || configRan == false) {
            thaliListener = getStartedListener("thaliListener");
            secondThaliListener = getStartedListener("secondThaliListener");
            noSecurityThaliListener = getStartedListener("noSecurityThaliListener");

            HttpKeyURL thaliListenerTorHttpKey =
                    new HttpKeyURL(thaliListener.getHttpKeys().getOnionHttpKeyURL());
            configureRequestObjects = new ConfigureRequestObjects("127.0.0.1", thaliListener.getSocketStatus().getPort(),
                    thaliListenerTorHttpKey.getHost(),thaliListenerTorHttpKey.getPort(),
                    ThaliCryptoUtilities.DefaultPassPhrase, getCreateClientBuilder(), getNewRandomCouchBaseContext(),
                    null, thaliListener.getSocksProxy());

            HttpKeyURL secondThaliListenerTorHttpKey =
                    new HttpKeyURL(secondThaliListener.getHttpKeys().getOnionHttpKeyURL());
            secondConfigureRequestObjects = new ConfigureRequestObjects("127.0.0.1",
                    secondThaliListener.getSocketStatus().getPort(), secondThaliListenerTorHttpKey.getHost(),
                    secondThaliListenerTorHttpKey.getPort(), ThaliCryptoUtilities.DefaultPassPhrase,
                    getCreateClientBuilder(), getNewRandomCouchBaseContext(), null, secondThaliListener.getSocksProxy());

            // Provision secondThaliListener to trust requests from thaliListener
            ThaliClientToDeviceHubUtilities.configureKeyInServersKeyDatabase(thaliListener.getServerPublicKey(),
                    secondConfigureRequestObjects.thaliCouchDbInstance);

            server = new RelayWebServer(getCreateClientBuilder(), getNewRandomCouchBaseContext().getFilesDir(),
                    thaliListener.getHttpKeys(), relayHost, relayPort);
            tempFileManager = new RelayWebServerTest.TestTempFileManager();

            server.start();
            configRan = true;
        }
    }

    @Override
    public void tearDown() {
        if (OsData.getOsType() == OsData.OsType.Android) {
            tempFileManager._clear();

            if (server != null && server.isAlive()) {
                server.stop();
            }

            if (thaliListener != null) {
                thaliListener.stopServer();
            }

            if (secondThaliListener != null) {
                secondThaliListener.stopServer();
            }

            if (noSecurityThaliListener != null) {
                noSecurityThaliListener.stopServer();
            }
        }
    }

}
