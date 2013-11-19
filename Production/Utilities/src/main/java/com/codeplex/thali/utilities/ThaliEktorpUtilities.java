package com.codeplex.thali.utilities;

import org.ektorp.android.http.AndroidHttpClient;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by yarong on 11/12/13.
 */
public class ThaliEktorpUtilities {
    /**
     * Creats an Ektorp HTTP Client that will validate that the server it is talking to presents the key specified in
     * serverRSAPublicKey per the HTTPKEY specification. See http://thali.cloudapp.net/mediawiki/index.php?title=Httpkey_URL_Scheme
     * @param hostName
     * @param port
     * @param proxyHostName  if NULL then proxyHostName and port will be ignored
     * @param proxyPort
     * @param serverRSAPublicKey
     * @param clientKeyStore
     * @param clientKeyStorePassPhrase
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static HttpClient getErktopHttpKeyClient(final String hostName, final int port,
                                              final String proxyHostName, final int proxyPort,
                                              final RSAPublicKey serverRSAPublicKey,
                                              final KeyStore clientKeyStore,
                                              final char[] clientKeyStorePassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
        // Adapted from http://stackoverflow.com/questions/2703161/how-to-ignore-ssl-certificate-errors-in-apache-httpclient-4-0 and from configureScheme in StdHttpClient.java in Ektorp
        org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new HttpKeySSLSocketFactory(serverRSAPublicKey, clientKeyStore, clientKeyStorePassPhrase);

        AndroidHttpClient.Builder builder = new AndroidHttpClient
                .Builder()
                .host(hostName)
                .port(port)
                .useExpectContinue(false)  // TJWS used for Thali Android fails on Expect Continue
                .relaxedSSLSettings(true)  // O.k. o.k. this is useless since the custom factory will override it
                .enableSSL(true)
                .sslSocketFactory(sslSocketFactory)
                .socketTimeout(100000)
                .connectionTimeout(100000);

        if (proxyHostName != null) {
            builder.proxy(proxyHostName).proxyPort(proxyPort);
        }

        return builder.build();
    }



    private HttpClient getErktopHttpKeyClient(final String hostName, final int port, final RSAPublicKey serverRSAPublicKey, final KeyStore clientKeyStore, final char[] clientPassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
        return getErktopHttpKeyClientBuilder(hostName, port, serverRSAPublicKey, clientKeyStore, clientPassPhrase).build();
    }

    private StdHttpClient.Builder getErktopHttpKeyClientBuilder(final String hostName, final int port, final RSAPublicKey serverRSAPublicKey, final KeyStore clientKeyStore, final char[] clientPassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
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
                if (serverRSAPublicKey == null) {
                    return;
                }
                PublicKey rootPublicKey = x509Certificates[x509Certificates.length -1].getPublicKey();
                if ((rootPublicKey instanceof RSAPublicKey) == false)
                {
                    throw new RuntimeException("Server must present a RSA key");
                }
                if (ThaliCryptoUtilities.RsaPublicKeyComparer((RSAPublicKey) rootPublicKey, serverRSAPublicKey) == false)
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
        org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(sslContext, org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        StdHttpClient.Builder builder = getEktorpHttpClientBuilder(hostName, port, null, null);
        builder.sslSocketFactory(sslSocketFactory);
        return builder;
    }
}
