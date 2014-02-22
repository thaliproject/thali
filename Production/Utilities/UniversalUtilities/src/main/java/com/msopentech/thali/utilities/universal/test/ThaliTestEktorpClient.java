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
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;

/**
 * This class contains all the generic parts of exercising a Ektorp client against a server. But the actual binding
 * depends on Java or Android and so is handled elsewhere.
 */
public class ThaliTestEktorpClient {
    public static final String KeyId = "key";
    public static final String ReplicationTestDatabaseName = "replicationtest";

    public static final int MaximumTestRecords = 10;

    /**
     * Try to connect to a DB with a client key we know is not authorized
     * @param host
     * @param port
     * @param createClientBuilder
     * @param actualServerPublicKey
     * @param actualClientKeyStore
     * @param clientPassPhrase
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static void runBadKeyTest(String host, int port, CreateClientBuilder createClientBuilder,
                                     PublicKey actualServerPublicKey, KeyStore actualClientKeyStore,
                                     char[] clientPassPhrase) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        KeyPair wrongKeys = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();

        HttpClient httpClientWithWrongServerKeyAndRightClientKey =
                createClientBuilder.CreateEktorpClient(host, port, wrongKeys.getPublic(), actualClientKeyStore, clientPassPhrase);

        CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClientWithWrongServerKeyAndRightClientKey);

        try {
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(ThaliTestUtilities.TestDatabaseName, true);
            throw new RuntimeException();
        } catch (Exception e) {
            ThaliTestUtilities.assertFail(e.getCause() instanceof SSLException);
        }

        KeyStore wrongClientKeyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(wrongKeys, "foo",
                        ThaliCryptoUtilities.DefaultPassPhrase);

        HttpClient httpClientWithRightServerKeyAndWrongClientKey =
                createClientBuilder.CreateEktorpClient(host, port, actualServerPublicKey, wrongClientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);

        couchDbInstance = new StdCouchDbInstance(httpClientWithRightServerKeyAndWrongClientKey);
        try {
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(ThaliTestUtilities.TestDatabaseName, true);
            throw new RuntimeException();
        } catch (DbAccessException e) {
        }
    }

    /**
     * Runs a test where we set a user key in one database and then post to another.
     * @param port
     * @param createClientBuilder
     * @param filesDir
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public static void runRetrieveTest(String host, int port, char[] passPhrase, CreateClientBuilder createClientBuilder, File filesDir)
            throws IOException, KeyManagementException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeySpecException, InterruptedException {
        ConfigureRequestObjects configureRequestObjects = new ConfigureRequestObjects(host, port, ReplicationTestDatabaseName, passPhrase, createClientBuilder, filesDir);

        Collection<CouchDbDocument> testDocuments = ThaliTestUtilities.setUpData(configureRequestObjects.thaliCouchDbInstance, 1, MaximumTestRecords, configureRequestObjects.clientPublicKey);
        ThaliTestUtilities.validateDatabaseState(configureRequestObjects.testDatabaseConnector, testDocuments);
        runBadKeyTest(host, port, createClientBuilder, configureRequestObjects.serverPublicKey, configureRequestObjects.clientKeyStore, passPhrase);
    }
}
