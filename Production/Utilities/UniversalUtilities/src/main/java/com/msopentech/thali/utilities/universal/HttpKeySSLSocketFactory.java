/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/


package com.msopentech.thali.utilities.universal;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpParams;

import javax.net.ssl.*;
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

    // PAY ATTENTION - THIS PART IS GOING TO HURT.
    // So as mentioned at the top of this class Google has decided to put an ancient version of Apache's HTTPClient
    // into Android. That ancient version calls the first two createSocket calls below.
    // Java, however, running a more modern version of Apache calls the third interface below. Of course if we
    // were really writing for modern HttpClient this whole class wouldn't need to exist but that's another
    // issue. But the fun part is that the third createsocket below doesn't exist in Android land. But it compiles
    // (and runs) because the JAR is defined with a version of Apache that supports the interface but when
    // running in Android a different version of the Apache HTTP Client is used which won't call the third
    // method.
    // And yes, this sucks.

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }


    @Override
    public Socket createSocket() throws IOException {
         return sslContext.getSocketFactory().createSocket();
    }

    @Override
    public Socket createSocket(final HttpParams params) throws IOException {
        SSLSocket sock = (SSLSocket) this.createSocket();
        prepareSocket(sock);
        return sock;
    }
}
