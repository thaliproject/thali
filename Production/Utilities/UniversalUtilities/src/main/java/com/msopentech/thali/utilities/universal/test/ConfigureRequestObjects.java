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

package com.msopentech.thali.utilities.universal.test;

import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliClientToDeviceHubUtilities;
import com.msopentech.thali.utilities.universal.ThaliCouchDbInstance;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.ektorp.CouchDbConnector;

import java.io.File;
import java.io.IOException;
import java.security.*;

/**
 * Running a non-trivial test, one where clients talk to servers and such requires a whole mess of objects.
 * This class generates them.
 */
public class ConfigureRequestObjects {
    public final ThaliCouchDbInstance thaliCouchDbInstance;
    public final CouchDbConnector testDatabaseConnector;
    public final CouchDbConnector replicationDatabaseConnector;
    public final PublicKey clientPublicKey;
    public final PublicKey serverPublicKey;
    public final KeyStore clientKeyStore;

    public ConfigureRequestObjects(String host, int port, char[] passPhrase,
                                   CreateClientBuilder createClientBuilder, File filesDir)
            throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException, KeyStoreException,
            KeyManagementException  {
        thaliCouchDbInstance = ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance(filesDir, createClientBuilder, host, port, passPhrase);

        testDatabaseConnector = thaliCouchDbInstance.createConnector(ThaliTestUtilities.TestDatabaseName, false);

        clientKeyStore = ThaliCryptoUtilities.validateThaliKeyStore(filesDir);

        org.apache.http.client.HttpClient httpClientNoServerValidation =
                createClientBuilder.CreateApacheClient(host, port, null, clientKeyStore, passPhrase);

        serverPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(
                        httpClientNoServerValidation);

        KeyStore.PrivateKeyEntry clientPrivateKeyEntry =
                (KeyStore.PrivateKeyEntry)
                        clientKeyStore.getEntry(ThaliCryptoUtilities.ThaliKeyAlias, new KeyStore.PasswordProtection(passPhrase));

        clientPublicKey = clientPrivateKeyEntry.getCertificate().getPublicKey();

        replicationDatabaseConnector = thaliCouchDbInstance.createConnector(
                ThaliTestEktorpClient.ReplicationTestDatabaseName, false);
    }
}
