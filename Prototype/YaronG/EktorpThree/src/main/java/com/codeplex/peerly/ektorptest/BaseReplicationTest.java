package com.codeplex.peerly.ektorptest;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ReplicationCommand;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 10/27/13
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class BaseReplicationTest {
    private final String AndroidHost = "127.0.0.1";
    private final int AndroidPort = 9898;

    private final String ErlangHost = "127.0.0.1";
    private final int ErlangPort = 5984;

    private final Boolean UseProxy = false;
    private final String ProxyHost = "127.0.0.1";
    private final int ProxyPort = 8888; // Fiddler2

    private final String TestDatabaseName = "test";
    private final int MaximumTestRecords = 10;

    final Logger Log = LoggerFactory.getLogger(BaseReplicationTest.class);

    /**
     * Allows us to configure common HTTP Client parameters like the use of a proxy in a single location.
     * @param hostName
     * @param port
     * @return
     */
    private HttpClient getTestClient(String hostName, int port) {
        StdHttpClient.Builder httpClientBuilder = new StdHttpClient.Builder().host(hostName).port(port);

        if (UseProxy) {
            httpClientBuilder = httpClientBuilder.proxy(ProxyHost).proxyPort(ProxyPort);
        }

        return httpClientBuilder.build();
    }

    @Test
    public void basicTest() {
        //createRetrieveTest(ErlangHost, ErlangPort);
        createRetrieveTest(AndroidHost, AndroidPort);
    }

    public void createRetrieveTest(String host, int port) {
        HttpClient httpClientToUpdate = getTestClient(host, port);
        CouchDbInstance couchDbInstanceToUpdate = new StdCouchDbInstance(httpClientToUpdate);
        CouchDbConnector couchDbConnector = couchDbInstanceToUpdate.createConnector(TestDatabaseName, false);
        validateDatabaseState(couchDbConnector, setUpData(couchDbInstanceToUpdate, TestDatabaseName, 1, 50));
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
        CouchDbConnector couchDbConnector = couchDbInstance.createConnector(databaseName, false);

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
}
