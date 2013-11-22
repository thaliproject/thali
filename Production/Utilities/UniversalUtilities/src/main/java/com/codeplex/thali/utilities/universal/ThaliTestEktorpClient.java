package com.codeplex.thali.utilities.universal;

import org.apache.http.HttpException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpRoutedConnection;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;

import java.io.IOException;
import java.security.*;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * This class contains all the generic parts of exercising a Ektorp client against a server. But the actual binding
 * depends on Java or Android and so is handled elsewhere.
 */
public class ThaliTestEktorpClient {
    public static final String KeyDatabaseName = "keydb";
    public static final String KeyId = "key";
    public static final String TestDatabaseName = "test";

    private static final String PEER_CERT_ATTRIBUTE = "com.codeplex.thali.peerCertAttribute";
    private static final int MaximumTestRecords = 10;

    /**
     * This is primarily used as a utility where we need to get the key for a server without knowing it first. We will issue
     * a GET request to "/" and see what key comes back.
     * @param host
     * @param port
     * @param clientKeyStore
     * @param clientPassPhrase
     * @return
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static PublicKey getServersRootPublicKey(String host, int port, KeyStore clientKeyStore,
                                                    char[] clientPassPhrase, org.apache.http.client.HttpClient httpClient)
            throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        // Taken from http://stackoverflow.com/questions/13273305/apache-httpclient-get-server-certificate
        // And yes we should do this with a request interceptor since it would work in all cases where we get a SSL
        // connection even if the HTTP request fails and I'm too lazy to rewrite it.
        ((AbstractHttpClient) httpClient).addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(org.apache.http.HttpResponse response, HttpContext context) throws HttpException, IOException {
                HttpRoutedConnection routedConnection = (HttpRoutedConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                if (routedConnection.isSecure()) {
                    java.security.cert.Certificate[] certificates = routedConnection.getSSLSession().getPeerCertificates();
                    context.setAttribute(PEER_CERT_ATTRIBUTE, certificates);
                }
            }
        });
        HttpContext httpContext = new BasicHttpContext();
        HttpUriRequest httpUriRequest = new HttpGet("/");
        org.apache.http.HttpResponse apacheHttpResponse = httpClient.execute(httpUriRequest, httpContext);
        java.security.cert.Certificate[] certificates = (java.security.cert.Certificate[]) httpContext.getAttribute(PEER_CERT_ATTRIBUTE);
        // TODO: Where is it written that the last cert is the server's root cert? Are certs guaranteed to be returned in order from leaf to root?
        return certificates[certificates.length - 1].getPublicKey();
    }

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
                                              PublicKey clientPublicKey) {
        String bigString = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String littleString = "1234567890";

        // Set key for authorization - Authorization is set up to allow anyone to set the document with the ID keyID but all other requests have to have
        // the key specified in the key document. This isn't meant to be secure, just to test if we are getting certs and validating them correctly.
        if (clientPublicKey != null) {
            couchDbInstance.deleteDatabase(KeyDatabaseName);
            couchDbInstance.createDatabase(KeyDatabaseName);
            CouchDBDocumentKeyClassForTests testRSAKeyClass =
                    new CouchDBDocumentKeyClassForTests(clientPublicKey);
            testRSAKeyClass.setId(KeyId);
            CouchDbConnector keyDbConnector = couchDbInstance.createConnector(KeyDatabaseName, false);
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


    /**
     * Runs a test where we set a user key in one database and then post to another. If the clientPublicKey is
     * null then the key database won't be created.
     * @param httpClient
     * @param clientPublicKey
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public static void runRetrieveTest(HttpClient httpClient, PublicKey clientPublicKey) throws IOException, KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClient);
        CouchDbConnector couchDbConnector = couchDbInstance.createConnector(TestDatabaseName, false);
        validateDatabaseState(couchDbConnector, setUpData(couchDbInstance, 1, MaximumTestRecords, clientPublicKey));
    }
}
