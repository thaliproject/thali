package com.codeplex.peerly.couchdbdesktop.test;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

public class TestCouchConnectivity {
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

    final Logger Log = LoggerFactory.getLogger(TestCouchConnectivity.class);

    private StdHttpClient.Builder getEktorpHttpClientBuilder(String hostName, int port) {
        StdHttpClient.Builder httpClientBuilder = new StdHttpClient.Builder().host(hostName).port(port).useExpectContinue(false).relaxedSSLSettings(true).enableSSL(true);

        if (UseProxy) {
            httpClientBuilder = httpClientBuilder.proxy(ProxyHost).proxyPort(ProxyPort);
        }

        return httpClientBuilder;
    }

    private org.apache.http.client.HttpClient getApacheHttpClient(String hostName, int port) {
        return getEktorpHttpClientBuilder(hostName, port).configureClient();
    }

    /**
     * Allows us to configure common HTTP Client parameters like the use of a proxy in a single location.
     * @param hostName
     * @param port
     * @return
     */
    private HttpClient getErktopHttpClient(String hostName, int port) {
        return new StdHttpClient(getApacheHttpClient(hostName, port));
    }

    private boolean rsaPublicKeyComparer(RSAPublicKey key1, RSAPublicKey key2) {
        return key1.getPublicExponent().compareTo(key2.getPublicExponent()) == 0 && key1.getModulus().compareTo(key2.getModulus()) == 0;
    }

    private HttpClient getErktopHttpKeyClient(final String hostName, final int port, final RSAPublicKey serverRSAPublicKey) throws NoSuchAlgorithmException, KeyManagementException {
        // Adapted from http://stackoverflow.com/questions/2703161/how-to-ignore-ssl-certificate-errors-in-apache-httpclient-4-0 and from configureScheme in StdHttpClient.java in Ektorp
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                throw new RuntimeException("We should not have gotten a client trusted call, authType was:" + authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                //TODO: We actually need to restrict authTypes to known secure ones
                PublicKey rootPublicKey = x509Certificates[x509Certificates.length -1].getPublicKey();
                if ((rootPublicKey instanceof RSAPublicKey) == false)
                {
                    throw new RuntimeException("Server must present a RSA key");
                }
                if (rsaPublicKeyComparer((RSAPublicKey) rootPublicKey, serverRSAPublicKey) == false)
                {
                    throw new RuntimeException("Presented server root key does not match expected server root key");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[] { trustManager }, null);
        org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext, org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        StdHttpClient.Builder builder = getEktorpHttpClientBuilder(hostName, port);
        builder.sslSocketFactory(sslSocketFactory);
        return builder.build();
    }

    @Test
    public void basicTest() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        //createRetrieveTest(ErlangHost, ErlangPort);
        createRetrieveTest(AndroidHost, AndroidPort);
    }

    /**
     * Used during testing to get the public key being used by the server's root cert
     * @param host
     * @param port
     * @return
     */
    public PublicKey getServersRootPublicKey(String host, int port) throws IOException {
        org.apache.http.client.HttpClient httpClient = getApacheHttpClient(host, port);
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

    public void createRetrieveTest(String host, int port) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        RSAPublicKey serverPublicKey = (RSAPublicKey) getServersRootPublicKey(host, port);

        HttpClient httpClientToUpdate = getErktopHttpKeyClient(host, port, serverPublicKey);
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
