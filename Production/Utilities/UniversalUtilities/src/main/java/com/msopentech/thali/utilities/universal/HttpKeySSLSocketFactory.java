package com.msopentech.thali.utilities.universal;

import org.apache.http.conn.ssl.SSLSocketFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Unfortunately Android uses an outdated version of the Apache HTTPClient interfaces and equally unfortunately
 * Ektorp depends on HTTP Client so we have to create our own SSLSocketFactory in order to get the hooks we need.
 * If we were using a modern version of Apache HTTP Client we could have just created the SSLSocketFactory
 * directory from the SSLContext.
 */
public class HttpKeySSLSocketFactory extends SSLSocketFactory {
    protected SSLContext sslContext;

    public HttpKeySSLSocketFactory(final PublicKey serverPublicKey,
                                   final KeyStore clientKeyStore, final char[] clientPassPhrase)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        super((KeyStore) null);

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

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, new SecureRandom());
        this.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }


    @Override
    public Socket createSocket() throws IOException {
         return sslContext.getSocketFactory().createSocket();
    }

}
