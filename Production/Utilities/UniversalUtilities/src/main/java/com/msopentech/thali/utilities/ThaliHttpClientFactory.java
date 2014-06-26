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

package com.msopentech.thali.utilities;

import com.couchbase.lite.support.HttpClientFactory;
import com.msopentech.thali.utilities.universal.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;

import java.net.*;
import java.security.*;
import java.util.List;

public class ThaliHttpClientFactory implements HttpClientFactory {
    protected final HttpKeySSLSocketFactory httpKeySSLSocketFactory;
    protected final PublicKey serverPublicKey;
    protected final KeyStore clientKeyStore;
    protected final char[] clientKeyStorePassPhrase;
    protected final Proxy proxy;

    public ThaliHttpClientFactory(final PublicKey serverPublicKey,
                                  final KeyStore clientKeyStore, final char[] clientKeyStorePassPhrase,
                                  Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        httpKeySSLSocketFactory =
                new HttpKeySSLSocketFactory(serverPublicKey, clientKeyStore, clientKeyStorePassPhrase);
        this.serverPublicKey = serverPublicKey;
        this.clientKeyStore = clientKeyStore;
        this.clientKeyStorePassPhrase = clientKeyStorePassPhrase;
        this.proxy = proxy;
    }

    @Override
    public HttpClient getHttpClient() {
        BasicHttpParams basicHttpParams = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", httpKeySSLSocketFactory, 443));
        try {
            return new HttpKeyHttpClient(serverPublicKey, clientKeyStore, clientKeyStorePassPhrase, proxy, basicHttpParams);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addCookies(List<Cookie> cookies) {
        // Cookies are a security hole, don't use them.
        return;
    }

    @Override
    public void deleteCookie(String name) {
        // Cookies are a security hole, don't use them.
        return;
    }

    @Override
    public CookieStore getCookieStore() {
        return null;
    }
}
