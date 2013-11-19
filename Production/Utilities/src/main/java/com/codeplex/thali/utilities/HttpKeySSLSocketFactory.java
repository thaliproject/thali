package com.codeplex.thali.utilities;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import static com.codeplex.thali.utilities.ThaliCryptoUtilities.RsaPublicKeyComparer;

/**
 * This hack is necessary because Android ships with an old (and never, apparently, to be updated) version of the Apache
 * HTTP libraries and Ektorp uses the Apache libraries so I get to use this hack described in http://stackoverflow.com/questions/7622004/android-making-https-request
 * in order to insert a trust manager into the client context so I can manually validate the server key.
 */
public class HttpKeySSLSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory {
    SSLContext sslContext;

    public HttpKeySSLSocketFactory(final RSAPublicKey serverRSAPublicKey, final KeyStore clientKeyStore,
                                   final char[] clientKeystorePassPhrase)
            throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        super(null);

        sslContext = SSLContext.getInstance("TLS");

        TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                throw new RuntimeException("We should not have gotten a client trusted call, authType was:" + authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                //TODO: We actually need to restrict authTypes to known secure ones
                PublicKey rootPublicKey = x509Certificates[x509Certificates.length -1].getPublicKey();
                if (false == (rootPublicKey instanceof RSAPublicKey))
                {
                    throw new RuntimeException("Server must present a RSA key");
                }
                if (false == RsaPublicKeyComparer((RSAPublicKey) rootPublicKey, serverRSAPublicKey))
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
        keyManagerFactory.init(clientKeyStore, clientKeystorePassPhrase);
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, null);
        this.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }
}
