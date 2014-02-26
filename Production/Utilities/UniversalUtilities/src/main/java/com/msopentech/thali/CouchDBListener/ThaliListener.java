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

package com.msopentech.thali.CouchDBListener;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.ManagerOptions;
import com.couchbase.lite.auth.AuthorizerFactory;
import com.couchbase.lite.auth.AuthorizerFactoryManager;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.listener.SocketStatus;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by yarong on 12/27/13.
 */
public class ThaliListener {
    public static final String KeyDatabaseName = "thaliprincipaldatabase";
    public static final String TjwsSslAcceptor = "com.msopentech.thali.CouchDBListener.AcceptAllClientCertsSSLAcceptor";
    public static final String DefaultThaliDeviceHubAddress = "127.0.0.1";
    public static final int DefaultThaliDeviceHubPort = 9898;

    private LiteListener cblListener = null;
    private boolean serverStarted = false;
    private final Logger Log = LoggerFactory.getLogger(ThaliListener.class);
    private Manager manager = null;

    private void waitTillServerStarts() throws InterruptedException {
        while (cblListener == null && serverStarted) {
            Thread.sleep(100);
        }

        if (serverStarted == false) {
            throw new RuntimeCryptoException("server wasn't started or was stopped.");
        }
    }

    /**
     * Starts the server on a new thread using a key and database files recorded in the specified directory and listening on
     * the specified port.
     * @param filesDir
     * @param port
     */
    public void startServer(final File filesDir, final int port) {
        serverStarted = true;
        if (filesDir == null) {
            throw new RuntimeException();
        }

        KeyStore clientKeyStore = ThaliCryptoUtilities.validateThaliKeyStore(filesDir);
        if (clientKeyStore == null) {
            if (ThaliCryptoUtilities.getThaliKeyStoreFileObject(filesDir).exists() == false) {
                clientKeyStore = ThaliCryptoUtilities.createNewThaliKeyInKeyStore(filesDir);
            } else {
                Log.error("Device key store came up as invalid.");
                throw new RuntimeException("Device key store came up as invalid.");
            }
        }

        final KeyStore finalClientKeyStore = clientKeyStore;

        new Thread(new Runnable() {
            public void run() {
                // Start the CouchDB Lite manager
                try {
                    ArrayList<AuthorizerFactory> authorizerFactoryArrayList = new ArrayList<AuthorizerFactory>();
                    BogusThaliAuthorizerFactory bogusThaliAuthorizerFactory = new BogusThaliAuthorizerFactory(finalClientKeyStore, ThaliCryptoUtilities.DefaultPassPhrase);
                    authorizerFactoryArrayList.add(bogusThaliAuthorizerFactory);
                    AuthorizerFactoryManager authorizerFactoryManager = new AuthorizerFactoryManager(authorizerFactoryArrayList);
                    ManagerOptions managerOptions =
                            new ManagerOptions(authorizerFactoryManager);
                    manager = new Manager(filesDir, managerOptions);
                    // This creates the database used to store the keys of remote applications that are authorized to use
                    // the system in case it doesn't already exist.
                    manager.getDatabase(KeyDatabaseName);
                } catch (IOException e) {
                    Log.error("Manager failed to start", e);
                    return;
                } catch (CouchbaseLiteException e) {
                    Log.error("Manager failed to start", e);
                    return;
                }

                Properties tjwsProperties = new Properties();
                tjwsProperties.setProperty(Serve.ARG_ACCEPTOR_CLASS, TjwsSslAcceptor);
                tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, ThaliCryptoUtilities.PrivateKeyHolderFormat);
                tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, ThaliCryptoUtilities.getThaliKeyStoreFileObject(filesDir).getAbsolutePath());
                tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, new String(ThaliCryptoUtilities.DefaultPassPhrase));

                tjwsProperties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "true");

                //tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, DefaultThaliDeviceHubAddress);

                BogusRequestAuthorization authorize = new BogusRequestAuthorization(KeyDatabaseName);

                cblListener = new LiteListener(manager, port, tjwsProperties, authorize);

                cblListener.start();
            }
        }).start();
    }

    public void stopServer() {
        if (cblListener != null) {
            cblListener.stop();
        }
        serverStarted = false;
    }

    public SocketStatus getSocketStatus() throws InterruptedException {
        waitTillServerStarts();

        return cblListener.getSocketStatus();
    }

    public Manager getManager() throws InterruptedException {
        waitTillServerStarts();

        return manager;
    }
}
