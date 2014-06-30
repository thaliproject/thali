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
/*
package com.msopentech.thali.utilities.universal.test;

import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.junit.Test;
import org.apache.http.client.HttpClient;
import org.ektorp.http.StdHttpClient;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertTrue;

class TestCreateClientBuilder extends CreateClientBuilder {
    @Override
    public org.ektorp.http.HttpClient CreateEktorpClient(String host, int port, PublicKey serverPublicKey,
                                                         KeyStore clientKeyStore, char[] clientKeyStorePassPhrase,
                                                         Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        return new StdHttpClient(
                CreateApacheClient(
                        host,
                        port,
                        serverPublicKey,
                        clientKeyStore,
                        clientKeyStorePassPhrase,
                        proxy));
    }
}

public class RelayWebServerTest {
    @Test
    public void testRelay() throws URISyntaxException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, KeyStoreException, KeyManagementException {

        ThaliListener tl = new ThaliListener();
        tl.startServer(new CreateContextInTemp(), ThaliListener.DefaultThaliDeviceHubPort, null);

        RelayWebServer server = new RelayWebServer(new TestCreateClientBuilder(), new File(System.getProperty("user.dir")));
    }
}
*/