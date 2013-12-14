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
 *
 *  It also includes code that Yaron Y. Goland on assignment to Open Tech intends to submit
 *  to CouchBase Mobile but has not actually committed yet.
 */

package com.msopentech.thali.utilities.android.test;

import Acme.Serve.Serve;
import android.util.Log;
import com.couchbase.lite.listener.LiteSSLAcceptor;
import com.couchbase.lite.listener.SocketStatus;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Created by yarong on 11/26/13.
 */
public class ThaliSelfSignedMutualAuthSSLAcceptor extends LiteSSLAcceptor {

    private static final String KEYSTOREPASS = "changeme";

    /**
     * This class is based on CBLSSLAcceptor and in addition to the changes it makes it also puts in
     * place a trust store for validating client certs that accepts everything.
     * @param inProperties
     * @param outProperties
     * @throws java.io.IOException
     */
    public void init(Map inProperties, Map outProperties) throws IOException {
        if (inProperties.containsKey(this.ARG_PORT) == false) {
            // For some odd reason the SSL Acceptor uses a different value for ARG_PORT than server does,
            // rather than surface this to users we just copy the port value across.
            // For another odd reasons SSL Acceptor only accepts the port value if it is a string.
            // Which would make sense if in other acceptors Integers weren't used.
            Object serveArgPort = inProperties.get(Serve.ARG_PORT);
            if (serveArgPort != null) {
                String sslArgPort;
                if (serveArgPort instanceof String) {
                    sslArgPort = (String)serveArgPort;
                } else if (serveArgPort instanceof Integer) {
                    sslArgPort = serveArgPort.toString();
                } else {
                    throw new RuntimeException(this.ARG_PORT + "is set to a value that is not an integer or a string.");
                }
                inProperties.put(this.ARG_PORT, sslArgPort);
            }
        }

        if ((inProperties.containsKey(this.ARG_IFADDRESS) == false) && (inProperties.containsKey(Serve.ARG_BINDADDRESS))) {
            inProperties.put(this.ARG_IFADDRESS, inProperties.get(Serve.ARG_BINDADDRESS));
        }

        javax.net.ssl.SSLServerSocketFactory sslSoc = null;
        // init keystore
        KeyStore keyStore = null;
        FileInputStream istream = null;
        String keystorePass = null;
        android = System.getProperty("java.vm.name") != null && System.getProperty("java.vm.name").startsWith("Dalvik");
        try {
            String keystoreType = getWithDefault(inProperties, ARG_KEYSTORETYPE, KEYSTORETYPE);
            keyStore = KeyStore.getInstance(keystoreType);
            String keystoreFile = (String) inProperties.get(ARG_KEYSTOREFILE);
            if (keystoreFile == null)
                keystoreFile = getKeystoreFile();
            istream = new FileInputStream(keystoreFile);
            keystorePass = getWithDefault(inProperties, ARG_KEYSTOREPASS, KEYSTOREPASS);
            keyStore.load(istream, keystorePass.toCharArray());
        } catch (Exception e) {
            throw (IOException)new IOException(e.toString()).initCause(e);
        } finally {
            if (istream != null)
                istream.close();
        }

        try {
            // Register the JSSE security Provider (if it is not already there)
            if (android == false)
                try {
                    Security.addProvider((java.security.Provider) Class
                            .forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
                } catch (Throwable t) {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                    // TODO think to do not propagate absence of the provider, since other can still work
                    throw (IOException)new IOException(t.toString()).initCause(t);
                }

            // Create an SSL context used to create an SSL socket factory
            String protocol = getWithDefault(inProperties, ARG_PROTOCOL, TLS);
            SSLContext context = SSLContext.getInstance(protocol);

            // Create the key manager factory used to extract the server key
            String algorithm = getWithDefault(inProperties, ARG_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);

            String keyPass = getWithDefault(inProperties, ARG_KEYPASS, keystorePass);

            keyManagerFactory.init(keyStore, keyPass.toCharArray());

            // Initialize the context with the key managers and trust managers
            TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                    return;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                    return;
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            context.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager } , new java.security.SecureRandom());

            // Create the proxy and return
            sslSoc = context.getServerSocketFactory();

        } catch (Exception e) {
            System.err.println("SSLsocket creation:  " + e);
            e.printStackTrace();
            throw (IOException)new IOException(e.toString()).initCause(e);
        }

        int port = PORT;
        if (inProperties.get(ARG_PORT) != null)
            try {
                port = Integer.parseInt((String) inProperties.get(ARG_PORT));
            } catch (NumberFormatException nfe) {

            }
        else if (inProperties.get(Serve.ARG_PORT) != null)
            port = ((Integer) inProperties.get(Serve.ARG_PORT)).intValue();
        if (inProperties.get(ARG_BACKLOG) == null)
            if (inProperties.get(ARG_IFADDRESS) == null)
                socket = sslSoc.createServerSocket(port);
            else
                socket = sslSoc.createServerSocket(port, BACKLOG,
                        InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));
        else if (inProperties.get(ARG_IFADDRESS) == null)
            socket = sslSoc.createServerSocket(port, new Integer((String) inProperties.get(ARG_BACKLOG)).intValue());
        else
            socket = sslSoc.createServerSocket(port, new Integer((String) inProperties.get(ARG_BACKLOG)).intValue(),
                    InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));

        initServerSocket(socket, "true".equals(inProperties.get(ARG_CLIENTAUTH)));
        if (outProperties != null)
            outProperties.put(Serve.ARG_BINDADDRESS, socket.getInetAddress().getHostName());
    }

    private ServerSocket getLocalSocket() {
        // There are race conditions where the server is being initialized on one thread while a
        // caller is on another thread. In that case we can end up with the acceptor having been
        // initialized but not the socket.
        while(this.socket == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("ThaliSelfSignedMutualAuthSSLAcceptor", "getPort sleep somehow got interrupted", e);
                throw new RuntimeException("getPort sleep somehow got interrupted", e);
            }
        }

        return this.socket;
    }

    @Override
    public SocketStatus getSocketStatus() {
        return new SocketStatus(getLocalSocket());
    }

    protected String getWithDefault(Map args, String name, String defValue) {
        String result = (String) args.get(name);
        if (result == null)
            return defValue;
        return result;
    }

    private String getKeystoreFile() {
        return (this.keystoreFile);
    }
}
