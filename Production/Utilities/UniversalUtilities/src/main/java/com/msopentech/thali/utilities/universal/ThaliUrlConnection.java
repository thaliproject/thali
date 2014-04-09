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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;import javax.net.ssl.HttpsURLConnection;import javax.net.ssl.KeyManagerFactory;import javax.net.ssl.SSLContext;import javax.net.ssl.SSLSession;import javax.net.ssl.TrustManager;import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.Override;import java.lang.RuntimeException;import java.lang.String;import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;import java.security.KeyStore;import java.security.KeyStoreException;import java.security.NoSuchAlgorithmException;import java.security.PublicKey;import java.security.SecureRandom;import java.security.UnrecoverableKeyException;import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * This class is nice because it can be made to work identically in both Android and Java. However Ektorp
 * currently only works with Apache HTTP clients so as long as we are committed to Ektorp we are committed
 * to Apache Client. So I keep this around for tests but otherwise we are focused on HttpClient (problems and all)
 * so we don't have to double down efforts on two different solution.
 */
public class ThaliUrlConnection {
    private static Logger logger = LoggerFactory.getLogger(ThaliUrlConnection.class);

    public static HttpsURLConnection getThaliUrlConnection(String httpsURL, final PublicKey serverPublicKey,
                                                          final KeyStore clientKeyStore, final char[] clientPassPhrase)
            throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        URL url = new URL(httpsURL);
        URLConnection urlConnection = url.openConnection();
        if ((urlConnection instanceof HttpsURLConnection) == false) {
            throw new RuntimeException("Received url of value \" + httpsURL + \" but it didn't produce a HttpsURLConnection.");
        }

        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;

        final ThaliPublicKeyComparer thaliPublicKeyComparer =
                serverPublicKey == null ? null : new ThaliPublicKeyComparer(serverPublicKey);

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

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, new SecureRandom());

        HostnameVerifier allowAllHostnames = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };

        httpsURLConnection.setHostnameVerifier(allowAllHostnames);
        httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());

        return httpsURLConnection;
    }
}
