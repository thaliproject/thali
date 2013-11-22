package com.codeplex.thali.utilities.java;

import com.codeplex.thali.utilities.universal.ThaliHttpClientUtilities;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.ektorp.http.StdHttpClient;

import java.security.*;

/**
 * Created by yarong on 11/20/13.
 */
public class ThaliJavaEktorpUtilities {
    public static final int timeout = 2 * 60 * 1000;

    /**
     * Creates a Ektorp StdHttpClient builder configured to be useful for Thali scenarios.
     * @param hostName
     * @param port
     * @return
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static StdHttpClient.Builder getEktorpHttpClientBuilder(String hostName, int port) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return new StdHttpClient.Builder().host(hostName).port(port).useExpectContinue(false).relaxedSSLSettings(true).enableSSL(true).socketTimeout(timeout).connectionTimeout(timeout);
    }

    /**
     * Build a Ektorp StdHttpClient.Builder that will execute a HTTPKey request. This means it will validate that the server
     * presents the serverPublicKey over a SSL connection and will, if asked, present the client key.
     * @param hostName
     * @param port
     * @param serverPublicKey
     * @param clientKey
     * @param clientPassPhrase
     * @return
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static StdHttpClient.Builder getEktorpHttpKeyClientBuilder(String hostName, int port, PublicKey serverPublicKey,
                                                    KeyStore clientKey, char[] clientPassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLSocketFactory sslSocketFactory =
                ThaliHttpClientUtilities.getHttpKeySocketFactory(serverPublicKey, clientKey, clientPassPhrase);
        return getEktorpHttpClientBuilder(hostName, port).sslSocketFactory(sslSocketFactory);
    }
}
