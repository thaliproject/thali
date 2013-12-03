package com.msopentech.thali.utilities.java;

import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import org.apache.http.client.HttpClient;
import org.ektorp.http.StdHttpClient;

import java.security.*;

/**
 * Created by yarong on 11/22/13.
 */
public class JavaEktorpCreateClientBuilder extends CreateClientBuilder {
    /**
     *
     * @param host
     * @param port
     * @param serverPublicKey
     * @param clientKeyStore
     * @param clientKeyStorePassPhrase
     * @return
     */
    protected static StdHttpClient.Builder getEktorpHttpClientBuilder(String host, int port, PublicKey serverPublicKey,
                                                                      KeyStore clientKeyStore,
                                                                      char[] clientKeyStorePassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return new StdHttpClient
                .Builder()
                .host(host)
                .port(port)
                .useExpectContinue(false)
                .relaxedSSLSettings(true)
                .enableSSL(true)
                .socketTimeout(timeout)
                .connectionTimeout(timeout)
                .sslSocketFactory(CreateClientBuilder.getHttpKeySocketFactory(serverPublicKey, clientKeyStore,
                        clientKeyStorePassPhrase));
    }

    @Override
    public HttpClient CreateApacheClient(String host, int port, PublicKey serverPublicKey, KeyStore clientKeyStore,
                                         char[] clientKeyStorePassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return getEktorpHttpClientBuilder(host, port, serverPublicKey, clientKeyStore, clientKeyStorePassPhrase).configureClient();
    }

    @Override
    public org.ektorp.http.HttpClient CreateEktorpClient(String host, int port, PublicKey serverPublicKey,
                                                         KeyStore clientKeyStore, char[] clientKeyStorePassPhrase) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return getEktorpHttpClientBuilder(host, port, serverPublicKey, clientKeyStore, clientKeyStorePassPhrase).build();
    }
}
