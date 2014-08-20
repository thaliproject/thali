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

/*
Some of the code used here was adapted from the NetCipher project which is licensed as:

This file contains the license for Orlib, a free software project to
provide anonymity on the Internet from a Google Android smartphone.

For more information about Orlib, see https://guardianproject.info/

If you got this file as a part of a larger bundle, there may be other
license terms that you should be aware of.
===============================================================================
Orlib is distributed under this license (aka the 3-clause BSD license)

Copyright (c) 2009-2010, Nathan Freitas, The Guardian Project

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.

    * Neither the names of the copyright owners nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*****
Orlib contains a binary distribution of the JSocks library:
http://code.google.com/p/jsocks-mirror/
which is licensed under the GNU Lesser General Public License:
http://www.gnu.org/licenses/lgpl.html

*****

 */


package com.msopentech.thali.utilities.universal;

import com.msopentech.thali.toronionproxy.OsData;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

import java.net.Proxy;
import java.security.*;

/**
 * Following the lead from NetCipher we need to hijack the createClientConnectionManager method in order to properly
 * implement DNS resolution when we are using a Tor Proxy.
 */
public class HttpKeyHttpClient extends DefaultHttpClient {
    protected final Proxy proxy;
    protected final SchemeRegistry schemeRegistry;
    protected int torProxyRequestRetryCount = 10;
    protected int maxConnections = 20;

    public HttpKeyHttpClient(PublicKey serverPublicKey, KeyStore clientKeyStore, char[] clientKeyStorePassPhrase,
            Proxy proxy, HttpParams params) throws UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException {
        super(params);
        schemeRegistry = new SchemeRegistry();
        HttpKeySSLSocketFactory httpKeySSLSocketFactory = new HttpKeySSLSocketFactory(serverPublicKey, clientKeyStore,
                clientKeyStorePassPhrase);
        schemeRegistry.register((new Scheme("https", httpKeySSLSocketFactory, 443)));

        // Try to up retries to deal with how flaky the Tor Hidden Service channels seem to be.
        // Note that modern Apache would set this via a Param but that Param doesn't seem to exist
        // in Android land. This sucks because we can't be sure if the user is using the default
        // retry handler or different one. And no, comparing getHttpRequestRetryHandler() against
        // DefaultHttpRequestRetryHandler.INSTANCE doesn't work. :( In fact, INSTANCE isn't even available
        // in Android.
        if (proxy != null && this.getHttpRequestRetryHandler() instanceof DefaultHttpRequestRetryHandler) {
            DefaultHttpRequestRetryHandler currentHandler =
                    (DefaultHttpRequestRetryHandler) this.getHttpRequestRetryHandler();
            if (currentHandler.getRetryCount() < torProxyRequestRetryCount) {
                this.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(torProxyRequestRetryCount,
                        true));
            }
        }

        this.proxy = proxy;
    }

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        switch(OsData.getOsType()) {
            case Android:
                return androidCreateClientConnectionManager();
            default:
                return javaCreateClientConnectionManager();
        }
    }

    protected ClientConnectionManager javaCreateClientConnectionManager() {
        // Note that HttpKeySocksProxyClientConnOperator only supports SOCKS connections
        // which is why we return a standard ThreadSafeClientConnManager when there is no
        // proxy and return a version with createConnectionOperator overridden only when
        // the connection is for SOCKS.

        ThreadSafeClientConnManager connManager;

        // It turns out that there is a bug in the Apache code we are using that if you use the argument
        // constructor below it will ignore anything you set in terms of httpParams and so we only get
        // 2 connections per destination which doesn't work for the realy who is sending lots of connections
        // to the local TDH. The work around is to use the single argument constructor below. Unfortunately this
        // constructor doesn't work in Android who has an even older version of the code, hence why we have to
        // methods.
        if (proxy == null) {
            connManager = new ThreadSafeClientConnManager(schemeRegistry);
        }
        else {
            connManager = new ThreadSafeClientConnManager(schemeRegistry) {
                @Override
                protected ClientConnectionOperator createConnectionOperator(SchemeRegistry schreg) {
                    return new HttpKeySocksProxyClientConnOperator(schreg, proxy);
                }
            };
        }

        connManager.setDefaultMaxPerRoute(maxConnections);
        connManager.setMaxTotal(maxConnections);
        return connManager;
    }

    protected ClientConnectionManager androidCreateClientConnectionManager() {
        // Note that HttpKeySocksProxyClientConnOperator only supports SOCKS connections
        // which is why we return a standard ThreadSafeClientConnManager when there is no
        // proxy and return a version with createConnectionOperator overridden only when
        // the connection is for SOCKS.

        ThreadSafeClientConnManager connManager;
        HttpParams httpParams = getParams();
        httpParams.setParameter(ConnManagerParams.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(maxConnections));
        httpParams.setParameter(ConnManagerParams.MAX_TOTAL_CONNECTIONS, maxConnections);

        if (proxy == null) {
            connManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        }
        else {
            connManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry) {
                @Override
                protected ClientConnectionOperator createConnectionOperator(SchemeRegistry schreg) {
                    return new HttpKeySocksProxyClientConnOperator(schreg, proxy);
                }
            };
        }

        return connManager;
    }
}
