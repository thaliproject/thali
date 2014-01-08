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

package com.msopentech.thali.utilities.universal;

import org.ektorp.CouchDbInstance;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.security.*;

/**
 * Created by yarong on 1/7/14.
 */
public class ThaliTestUrlConnection {

    public static void TestThaliUrlConnection(String host, int port, char[] passPhrase, CreateClientBuilder createClientBuilder, File filesDir)
            throws InterruptedException, UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        CouchDbInstance couchDbInstance = ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance(filesDir, createClientBuilder, host, port, passPhrase);

        couchDbInstance.deleteDatabase(ThaliTestEktorpClient.TestDatabaseName);
        couchDbInstance.createDatabase(ThaliTestEktorpClient.TestDatabaseName);

        KeyStore clientKeyStore = ThaliCryptoUtilities.validateThaliKeyStore(filesDir);

        org.apache.http.client.HttpClient httpClientNoServerValidation =
                createClientBuilder.CreateApacheClient(host, port, null, clientKeyStore, passPhrase);

        PublicKey serverPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(
                        httpClientNoServerValidation);

        String httpsURL = "https://" + host + ":" + port + "/" + ThaliTestEktorpClient.TestDatabaseName + "/";

        HttpsURLConnection httpsURLConnection =
                ThaliUrlConnection.getThaliUrlConnection(httpsURL, serverPublicKey, clientKeyStore, passPhrase);

        httpsURLConnection.setRequestMethod("GET");
        int responseCode = httpsURLConnection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException();
        }
    }
}
