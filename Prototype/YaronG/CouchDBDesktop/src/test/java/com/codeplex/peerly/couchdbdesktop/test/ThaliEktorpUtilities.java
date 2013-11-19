package com.codeplex.peerly.couchdbdesktop.test;

import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;

import java.security.*;
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
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     */
    public static HttpClient getErktopHttpKeyClient(final String hostName, final int port,
                                              final String proxyHostName, final int proxyPort,
                                              final RSAPublicKey serverRSAPublicKey,
                                              final KeyStore clientKeyStore,
                                              final char[] clientKeyStorePassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
        // Adapted from http://stackoverflow.com/questions/2703161/how-to-ignore-ssl-certificate-errors-in-apache-httpclient-4-0 and from configureScheme in StdHttpClient.java in Ektorp
        org.apache.http.conn.ssl.SSLSocketFactory sslSocketFactory = new HttpKeySSLSocketFactory(serverRSAPublicKey, clientKeyStore, clientKeyStorePassPhrase);

        StdHttpClient.Builder builder = new StdHttpClient
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

}
