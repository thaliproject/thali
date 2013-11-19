package com.codeplex.thali.utilities.test;

import com.codeplex.thali.utilities.ThaliEktorpUtilities;
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
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by yarong on 11/12/13.
 */
public class TestThaliEktorpUtilities {
    private final String PEER_CERT_ATTRIBUTE = "com.codeplex.peerly.peerCertAttribute";
    private final String MachineHost = "127.0.0.1";

    private final String AndroidHost = MachineHost;
    private final int AndroidPort = 9898;

    private final String ErlangHost = MachineHost;
    private final int ErlangPort = 5984;

    private final String ProxyHost = MachineHost;
    private final int ProxyPort = 8888; // Fiddler2

    private final String TestDatabaseName = "test";
    private final int MaximumTestRecords = 10;

    private Boolean UseProxy = false;

    final Logger Log = LoggerFactory.getLogger(TestThaliEktorpUtilities.class);

    //@Test
    public void basicTest() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        //createRetrieveTest(ErlangHost, ErlangPort);
        createRetrieveTest(AndroidHost, AndroidPort, UseProxy ? ProxyHost : null, ProxyPort);
    }

    /**
     * Used during testing to get the public key being used by the server's root cert
     * @param host
     * @param port
     * @return
     */
    public PublicKey getServersRootPublicKey(String host, int port, String proxyHost, int proxyPort) throws IOException {

        StdHttpClient.Builder builder = new StdHttpClient
                .Builder()
                .host(host)
                .port(port)
                .useExpectContinue(false)  // TJWS used for Thali Android fails on Expect Continue
                .relaxedSSLSettings(true)
                .enableSSL(true);

        if (proxyHost != null) {
            builder.proxy(proxyHost).proxyPort(proxyPort);
        }

        org.apache.http.client.HttpClient httpClient = builder.configureClient();
        // Taken from http://stackoverflow.com/questions/13273305/apache-httpclient-get-server-certificate
        ((AbstractHttpClient) httpClient).addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(org.apache.http.HttpResponse response, HttpContext context) throws HttpException, IOException {
                HttpRoutedConnection routedConnection = (HttpRoutedConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                if (routedConnection.isSecure()) {
                    Certificate[] certificates = routedConnection.getSSLSession().getPeerCertificates();
                    context.setAttribute(PEER_CERT_ATTRIBUTE, certificates);
                }
            }
        });
        HttpContext httpContext = new BasicHttpContext();
        HttpUriRequest httpUriRequest = new HttpGet("/");
        org.apache.http.HttpResponse apacheHttpResponse = httpClient.execute(httpUriRequest, httpContext);
        assertTrue(apacheHttpResponse.getStatusLine().getStatusCode() == 200);
        Certificate[] certificates = (Certificate[]) httpContext.getAttribute(PEER_CERT_ATTRIBUTE);
        // TODO: Where is it written that the last cert is the server's root cert? Are certs guaranteed to be returned in order from leaf to root?
        return certificates[certificates.length - 1].getPublicKey();
    }

    public void createRetrieveTest(String host, int port, String proxyHost, int proxyPort) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        RSAPublicKey serverPublicKey = (RSAPublicKey) getServersRootPublicKey(host, port, proxyHost, proxyPort);

        HttpClient httpClientToUpdate = ThaliEktorpUtilities.getErktopHttpKeyClient(host, port, proxyHost, proxyPort, serverPublicKey);
        CouchDbInstance couchDbInstanceToUpdate = new StdCouchDbInstance(httpClientToUpdate);
        CouchDbConnector couchDbConnector = couchDbInstanceToUpdate.createConnector(TestDatabaseName, true);
        validateDatabaseState(couchDbConnector, setUpData(couchDbInstanceToUpdate, TestDatabaseName, 1, MaximumTestRecords));
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
}
