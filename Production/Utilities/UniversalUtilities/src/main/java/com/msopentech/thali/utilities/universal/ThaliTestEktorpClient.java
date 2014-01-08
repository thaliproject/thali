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

import com.msopentech.thali.CouchDBListener.BogusAuthorizeCouchDocument;
import com.msopentech.thali.CouchDBListener.ThaliListener;
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
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * This class contains all the generic parts of exercising a Ektorp client against a server. But the actual binding
 * depends on Java or Android and so is handled elsewhere.
 */
public class ThaliTestEktorpClient {
    public static final String KeyId = "key";
    public static final String TestDatabaseName = "test";

    private static final int MaximumTestRecords = 10;

    /**
     * Checks if the docs exist in the database. Note that the DocType implements an overload for equals that
     * will properly compare instances of that class.
     * @param couchDbConnector
     * @param docsToCheck
     */
    private static void validateDatabaseState(CouchDbConnector couchDbConnector, CouchDbDocument[] docsToCheck) {
        List<String> docIds = couchDbConnector.getAllDocIds();

        assert docIds.size() == docsToCheck.length;

        for(int i = 0; i < docsToCheck.length; ++i) {
            assert docIds.contains(docsToCheck[i].getId());
            CouchDbDocument remoteDocument = couchDbConnector.get(docsToCheck[i].getClass(), docsToCheck[i].getId());
            assert docsToCheck[i].equals(remoteDocument);
        }
    }

    /**
     * Deletes a test database name and then fills it with random records of type TestBlogClass and
     * returns the records so they can be tested against. Note that it is possible for 0 docs to be
     * generated which can be useful for some kinds of tests and surprising for others.
     * @param couchDbInstance
     * @param  minimumTestRecords
     * @param maximumTestRecords
     * @param clientPublicKey This can be null if we are doing regression testing of no SSL and SSL without client auth scenarios
     */
    private static CouchDbDocument[] setUpData(CouchDbInstance couchDbInstance,
                                              int minimumTestRecords, int maximumTestRecords,
                                              PublicKey clientPublicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String bigString = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String littleString = "1234567890";

        // Set key for authorization - Authorization is set up to allow anyone to set the document with the ID keyID but all other requests have to have
        // the key specified in the key document. This isn't meant to be secure, just to test if we are getting certs and validating them correctly.
        if (clientPublicKey != null) {
            couchDbInstance.deleteDatabase(ThaliListener.KeyDatabaseName);
            couchDbInstance.createDatabase(ThaliListener.KeyDatabaseName);
            BogusAuthorizeCouchDocument testRSAKeyClass =
                    new BogusAuthorizeCouchDocument(clientPublicKey);
            testRSAKeyClass.setId(testRSAKeyClass.getId());
            CouchDbConnector keyDbConnector = couchDbInstance.createConnector(ThaliListener.KeyDatabaseName, false);
            keyDbConnector.create(testRSAKeyClass);
        }

        couchDbInstance.deleteDatabase(TestDatabaseName);
        couchDbInstance.createDatabase(TestDatabaseName);
        CouchDbConnector couchDbConnector = couchDbInstance.createConnector(TestDatabaseName, false);

        Stack<CouchDbDocument> generatedDocs = new Stack<CouchDbDocument>();

        Random random = new Random();
        int numberOfDocuments = random.nextInt((maximumTestRecords - minimumTestRecords)+1) + minimumTestRecords;
        for(int i = 0; i < numberOfDocuments; ++i) {
            CouchDBDocumentBlogClassForTests testBlog = new CouchDBDocumentBlogClassForTests();
            testBlog.setBlogArticleName(String.valueOf(random.nextBoolean() ? bigString : littleString));
            testBlog.setBlogArticleContent(String.valueOf(random.nextBoolean() ? bigString : littleString));
            couchDbConnector.create(testBlog);
            generatedDocs.push(testBlog);
        }

        return generatedDocs.toArray(new CouchDbDocument[generatedDocs.size()]);
    }

    public static void runBadKeyTest(String host, int port, CreateClientBuilder createClientBuilder,
                                     PublicKey actualServerPublicKey, KeyStore actualClientKeyStore,
                                     char[] clientPassPhrase) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        KeyPair wrongKeys = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();

        HttpClient httpClientWithWrongServerKeyAndRightClientKey =
                createClientBuilder.CreateEktorpClient(host, port, wrongKeys.getPublic(), actualClientKeyStore, clientPassPhrase);

        CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClientWithWrongServerKeyAndRightClientKey);

        try {
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(TestDatabaseName, true);
            assert false;
        } catch (Exception e) {
            assert e.getCause() instanceof SSLException;
        }

        KeyStore wrongClientKeyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(wrongKeys, "foo",
                        ThaliCryptoUtilities.DefaultPassPhrase);

        HttpClient httpClientWithRightServerKeyAndWrongClientKey =
                createClientBuilder.CreateEktorpClient(host, port, actualServerPublicKey, wrongClientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);

        couchDbInstance = new StdCouchDbInstance(httpClientWithRightServerKeyAndWrongClientKey);
        try {
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(TestDatabaseName, true);
            assert false;
        } catch (DbAccessException e) {
        }
    }


    /**
     * Runs a test where we set a user key in one database and then post to another. If the clientPublicKey is
     * null then the key database won't be created.
     * @param host
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
            throws IOException, KeyManagementException, NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException, InvalidKeySpecException {
        CouchDbInstance couchDbInstance = ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance(filesDir, createClientBuilder, host, port, passPhrase);

        CouchDbConnector couchDbConnector = couchDbInstance.createConnector(TestDatabaseName, false);

        KeyStore clientKeyStore = ThaliCryptoUtilities.validateThaliKeyStore(filesDir);

        org.apache.http.client.HttpClient httpClientNoServerValidation =
                createClientBuilder.CreateApacheClient(host, port, null, clientKeyStore, passPhrase);

        PublicKey serverPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(
                        httpClientNoServerValidation);

        KeyStore.PrivateKeyEntry clientPrivateKeyEntry =
                (KeyStore.PrivateKeyEntry)
                        clientKeyStore.getEntry(ThaliCryptoUtilities.ThaliKeyAlias, new KeyStore.PasswordProtection(passPhrase));

        PublicKey clientPublicKey = clientPrivateKeyEntry.getCertificate().getPublicKey();

        validateDatabaseState(couchDbConnector, setUpData(couchDbInstance, 1, MaximumTestRecords, clientPublicKey));
        runBadKeyTest(host, port, createClientBuilder, serverPublicKey, clientKeyStore, passPhrase);
    }
}
