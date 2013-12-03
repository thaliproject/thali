package com.msopentech.thali.utilities.android;

import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import org.apache.http.client.HttpClient;
import org.ektorp.android.http.AndroidHttpClient;

import java.security.*;

/**
 * Created by yarong on 12/2/13.
 */
public class AndroidEktorpCreateClientBuilder extends CreateClientBuilder {
    protected static AndroidHttpClient.Builder getEktorpHttpClientBuilder(String host, int port, PublicKey serverPublicKey,
                                                                      KeyStore clientKeyStore,
                                                                      char[] clientKeyStorePassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        return new AndroidHttpClient
                .Builder()
                .host(host)
                .port(port)
                .useExpectContinue(false)
                .relaxedSSLSettings(true)
                .enableSSL(true)
                .socketTimeout(timeout)
                .connectionTimeout(timeout)
                .sslSocketFactory(
                        CreateClientBuilder.getHttpKeySocketFactory(serverPublicKey, clientKeyStore, clientKeyStorePassPhrase));
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
