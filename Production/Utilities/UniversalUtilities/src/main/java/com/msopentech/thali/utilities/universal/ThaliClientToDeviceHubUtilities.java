/*
 * The code below was mostly (but not completely) copied from SSLAcceptor.java in TJWS so the
 * license is included below.
 * tjws - SSLAcceptor.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.
 *  $Id: SSLAcceptor.java,v 1.10 2013/03/02 09:11:56 cvs Exp $
 *  Created on Feb 21, 2007
 *  @author dmitriy
 */

/*
Any changes to the code are covered under the following license:
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

import com.msopentech.thali.CouchDBListener.BogusAuthorizeCouchDocument;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import org.apache.http.HttpException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpRoutedConnection;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.conn.tsccm.BasicPooledConnAdapter;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.security.*;

/**
 * A collection of utilities useful for Thali Clients
 */
public class ThaliClientToDeviceHubUtilities {
    private static final String PEER_CERT_ATTRIBUTE = "com.msopentech.thali.peerCertAttribute";

    /**
     * This creates the CouchDbInstance needed to talk to the local Thali Device Hub. Amongst other things if it
     * detects that there is something wrong with the application's Thali key (like it doesn't exist) then it will
     * create a new key and register it with the Thali Device Hub.
     * Note that in most cases host should be 127.0.0.1 and proxy should be null. We allow these values to be set
     * in order to support certain kinds of testing.
     */
    public static ThaliCouchDbInstance GetLocalCouchDbInstance(File filesDir, CreateClientBuilder createClientBuilder,
                                                               String host, int port, char[] passPhrase, Proxy proxy)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
            IOException {
        assert filesDir != null && filesDir.exists();

        KeyStore clientKeyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(filesDir);

        org.apache.http.client.HttpClient httpClientNoServerValidation =
                createClientBuilder.CreateApacheClient(host, port, null, clientKeyStore, passPhrase, proxy);

        PublicKey serverPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(
                        httpClientNoServerValidation);

        return configureClientAndGetCouchDbInstance(createClientBuilder,
                new HttpKeyURL(serverPublicKey, host, port, null, null, null), passPhrase, proxy, clientKeyStore);
    }

    public static ThaliCouchDbInstance GetLocalCouchDbInstance(File filesDir, CreateClientBuilder createClientBuilder,
                                                               HttpKeyURL serverHttpKey, char[] passPhrase,
                                                               Proxy proxy)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        assert filesDir != null && filesDir.exists();

        KeyStore clientKeyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(filesDir);

        return configureClientAndGetCouchDbInstance(createClientBuilder, serverHttpKey, passPhrase, proxy,
                clientKeyStore);

    }

    private static ThaliCouchDbInstance configureClientAndGetCouchDbInstance(CreateClientBuilder createClientBuilder,
                                                                             HttpKeyURL serverHttpKey, char[] passPhrase,
                                                                             Proxy proxy, KeyStore clientKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableEntryException {
        HttpClient httpClientWithServerValidation =
                createClientBuilder.CreateEktorpClient(serverHttpKey, clientKeyStore, passPhrase, proxy);

        ThaliCouchDbInstance thaliCouchDbInstance = new ThaliCouchDbInstance(httpClientWithServerValidation);

        // Set up client key in permission database
        KeyStore.PrivateKeyEntry clientPrivateKeyEntry =
                ThaliCryptoUtilities.getThaliListenerKeyStoreEntry(clientKeyStore);

        PublicKey clientPublicKey = clientPrivateKeyEntry.getCertificate().getPublicKey();

        configureKeyInServersKeyDatabase(clientPublicKey, thaliCouchDbInstance);
        return thaliCouchDbInstance;
    }

    /**
     *
     * @param keyToProvision
     * @param serverInstance
     */
    public static void configureKeyInServersKeyDatabase(PublicKey keyToProvision, CouchDbInstance serverInstance) {
        BogusAuthorizeCouchDocument authDoc = new BogusAuthorizeCouchDocument(keyToProvision);
        CouchDbConnector keyDatabaseConnector = serverInstance.createConnector(ThaliListener.KeyDatabaseName, true);
        if (keyDatabaseConnector.contains(authDoc.getId()) == false) {
            keyDatabaseConnector.create(authDoc);
        }
    }

    /**
     * This is a horrible hack used by clients to get the server key for the local Thali Device Hub. Eventually we'll
     * introduce something actually reasonably secure for this purposes.
     * @param  httpClient
     * @return
     * @throws java.io.IOException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static PublicKey getServersRootPublicKey(org.apache.http.client.HttpClient httpClient)
            throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        // Taken from http://stackoverflow.com/questions/13273305/apache-httpclient-get-server-certificate
        // And yes we should do this with a request interceptor since it would work in all cases where we get a SSL
        // connection even if the HTTP request fails and I'm too lazy to rewrite it.
        ((AbstractHttpClient) httpClient).addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(org.apache.http.HttpResponse response, HttpContext context) throws HttpException, IOException {
                Object unTypedHttpConnection = context.getAttribute(ExecutionContext.HTTP_CONNECTION);

                // Android doesn't return an object that supports the HttpRoutedConnection interface which makes no sense!
                // What it does return is BasicPooledConnAdapter but that is supposed to support HttpRoutedConnection! But doesn't.
                if (unTypedHttpConnection instanceof BasicPooledConnAdapter)
                {
                    BasicPooledConnAdapter basicPooledConnAdapter = (BasicPooledConnAdapter) unTypedHttpConnection;
                    if (basicPooledConnAdapter.isSecure()) {
                        java.security.cert.Certificate[] certificates = basicPooledConnAdapter.getSSLSession().getPeerCertificates();
                        context.setAttribute(PEER_CERT_ATTRIBUTE, certificates);
                    }
                    return;
                }

                if (unTypedHttpConnection instanceof HttpRoutedConnection) {
                    HttpRoutedConnection httpRoutedConnection = (HttpRoutedConnection) unTypedHttpConnection;
                    if (httpRoutedConnection.isSecure()) {
                        java.security.cert.Certificate[] certificates = httpRoutedConnection.getSSLSession().getPeerCertificates();
                        context.setAttribute(PEER_CERT_ATTRIBUTE, certificates);
                    }
                    return;
                }

                throw new RuntimeException("Unexpected HTTP_CONNECTION: " + unTypedHttpConnection.toString());
            }
        });
        HttpContext httpContext = new BasicHttpContext();
        HttpUriRequest httpUriRequest = new HttpGet("/");
        org.apache.http.HttpResponse apacheHttpResponse = httpClient.execute(httpUriRequest, httpContext);
        java.security.cert.Certificate[] certificates = (java.security.cert.Certificate[]) httpContext.getAttribute(PEER_CERT_ATTRIBUTE);
        // TODO: Where is it written that the last cert is the server's root cert? Are certs guaranteed to be returned in order from leaf to root?
        return certificates[certificates.length - 1].getPublicKey();
    }
}
