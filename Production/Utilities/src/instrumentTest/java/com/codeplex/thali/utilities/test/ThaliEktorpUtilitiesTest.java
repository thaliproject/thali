package com.codeplex.thali.utilities.test;

import android.test.InstrumentationTestCase;
import com.codeplex.thali.utilities.ThaliCryptoUtilities;
import com.codeplex.thali.utilities.ThaliEktorpUtilities;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Random;

public class ThaliEktorpUtilitiesTest extends InstrumentationTestCase {
    private ThaliTestServer thaliTestServer = null;
    private HttpClient httpClient = null;
    private CouchDbInstance couchDbInstance = null;

    private int testServerPort;

    private KeyStore clientKeyStore;

    private final String localHost = "127.0.0.1";
    private final String testDatabaseName = "androidtesting";
    private final int maximumNumberOfRandomRecordsToCreate = 10;
    private final String clientKeyStoreAlias = "clientKeyStoreAlias";
    private final String proxyHost = null;
    private final int proxyPort = 8888;
    private final boolean useSSL = false;

    @Override
    public void setUp() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {

        clientKeyStore = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair(), clientKeyStoreAlias, ThaliCryptoUtilities.DefaultPassPhrase);
        // Just for fun, to see if we can pull it back out right.
        RSAPublicKey clientPublicKey = (RSAPublicKey) clientKeyStore.getCertificate(clientKeyStoreAlias).getPublicKey();
        thaliTestServer = new ThaliTestServer(getInstrumentation().getContext().getFilesDir(), clientPublicKey, localHost, 0, useSSL);
        testServerPort = thaliTestServer.start();
        thaliTestServer.ValidateServerStatus(localHost, testServerPort);
    }

    @Override
    public void tearDown() {
        if (couchDbInstance != null) {
            //couchDbInstance.deleteDatabase(testDatabaseName);
        }

        if (thaliTestServer != null) {
            thaliTestServer.close();
        }

        if (httpClient != null) {
            httpClient.shutdown();
        }
    }

    /**
     * Deletes a test database name and then fills it with random records of type TestBlogClass and
     * returns the records so they can be tested against. Note that it is possible for 0 docs to be
     * generated which can be useful for some kinds of tests and surprising for others.
     * @param couchDbInstance
     */
    private TestBlogClass[] setUpData(CouchDbInstance couchDbInstance, String databaseName, int minimumRestRecords, int maximumTestRecords) {
        couchDbInstance.deleteDatabase(databaseName);
        couchDbInstance.createDatabase(databaseName);
        CouchDbConnector couchDbConnector = couchDbInstance.createConnector(databaseName, true);

        Random random = new Random();
        int numberOfDocuments = random.nextInt((maximumTestRecords - minimumRestRecords)+1) + minimumRestRecords;
        TestBlogClass[] generatedDocs = new TestBlogClass[numberOfDocuments];
        for(int i = 0; i < numberOfDocuments; ++i) {
            generatedDocs[i] = new TestBlogClass();
            generatedDocs[i].setBlogArticleName(String.valueOf(random.nextInt()));
            generatedDocs[i].setBlogArticleContent(String.valueOf(random.nextInt()));
            couchDbConnector.create(generatedDocs[i]);
        }
        return generatedDocs;
    }

    /**
     * Checks if the docs exist in the database. Note that the DocType implements an overload for equals that
     * will properly compare instances of that class.
     * @param couchDbConnector
     * @param docsToCheck
     */
    private void validateDatabaseState(CouchDbConnector couchDbConnector, TestBlogClass[] docsToCheck) {
        List<String> docIds = couchDbConnector.getAllDocIds();

        assertTrue(docIds.size() == docsToCheck.length);

        for(int i = 0; i < docsToCheck.length; ++i) {
            assertTrue(docIds.contains(docsToCheck[i].getId()));
            assertTrue(docsToCheck[i].equals(couchDbConnector.get(docsToCheck[i].getClass(), docsToCheck[i].getId())));
        }
    }

    public void testGetErktopHttpKeyClient() throws Exception {
        httpClient = ThaliEktorpUtilities.getErktopHttpKeyClient(localHost, testServerPort, proxyHost, proxyPort, thaliTestServer.getServerRSAPublicKey(), clientKeyStore, ThaliCryptoUtilities.DefaultPassPhrase);
        couchDbInstance = new StdCouchDbInstance(httpClient);
        try {
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(testDatabaseName, false);
            validateDatabaseState(couchDbConnector, setUpData(couchDbInstance, testDatabaseName, 1, maximumNumberOfRandomRecordsToCreate));
        } catch (Exception e) {
            thaliTestServer.ValidateServerStatus(localHost, testServerPort);
            throw e;
        }
    }
}
