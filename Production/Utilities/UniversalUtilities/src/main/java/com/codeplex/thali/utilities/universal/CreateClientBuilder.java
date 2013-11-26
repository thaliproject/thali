package com.codeplex.thali.utilities.universal;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Ektorp using different client builders for Java and for Android, this class lets us abtract that away so we can
 * build a common test base for both.
 */
public abstract class CreateClientBuilder {
    public static final int timeout = 2 * 60 * 1000;

    /**
     *
     * @param host
     * @param port
     * @param serverPublicKey If null then the server won't be validated
     * @param clientKeyStore
     * @param clientKeyStorePassPhrase
     * @return
     */
    abstract public HttpClient CreateApacheClient(String host, int port, PublicKey serverPublicKey, KeyStore clientKeyStore,
                                         char[] clientKeyStorePassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;

    abstract public org.ektorp.http.HttpClient CreateEktorpClient(String host, int port, PublicKey serverPublicKey,
                                                         KeyStore clientKeyStore, char[] clientKeyStorePassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;

    /**
     * Creates a SSL Socket Factory that will validate that the server presented a cert chain that roots with the
     * key serverPublicKey and will present to the server (if asked) the key stored in clientKeyStore
     * @param serverPublicKey if set to null then any key will be accepted from the server
     * @param clientKeyStore make sure there is just one public/private key pair
     * @param clientPassPhrase
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.security.KeyStoreException
     */
    public static SSLSocketFactory getHttpKeySocketFactory(final PublicKey serverPublicKey,
                                                           final KeyStore clientKeyStore, final char[] clientPassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
        // Adapted from http://stackoverflow.com/questions/2703161/how-to-ignore-ssl-certificate-errors-in-apache-httpclient-4-0 and from configureScheme in StdHttpClient.java in Ektorp
        SSLContext sslContext = SSLContext.getInstance("TLS");
        final ThaliPublicKeyComparer thaliPublicKeyComparer = serverPublicKey == null ? null : new ThaliPublicKeyComparer(serverPublicKey);

        TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                throw new RuntimeException("We should not have gotten a client trusted call, authType was:" + authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                //TODO: We actually need to restrict authTypes to known secure ones
                if (serverPublicKey == null) {
                    return;
                }
                PublicKey rootPublicKey = x509Certificates[x509Certificates.length -1].getPublicKey();
                if (thaliPublicKeyComparer.KeysEqual(rootPublicKey) == false)
                {
                    throw new RuntimeException("Presented server root key does not match expected server root key");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, clientPassPhrase);

        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, null);
        return new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }
}
