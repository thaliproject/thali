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
import com.couchbase.lite.*;
import com.couchbase.lite.auth.AuthorizerFactory;
import com.couchbase.lite.auth.AuthorizerFactoryManager;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.listener.SocketStatus;
import com.couchbase.lite.util.Log;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.msopentech.thali.utilities.universal.CblLogTags;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.bouncycastle.crypto.RuntimeCryptoException;

import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Properties;

public class ThaliListener {
    public static final String KeyDatabaseName = "thaliprincipaldatabase";
    public static final String TjwsSslAcceptor = "com.msopentech.thali.CouchDBListener.AcceptAllClientCertsSSLAcceptor";
    public static final String DefaultThaliDeviceHubAddress = "127.0.0.1";
    public static final int DefaultThaliDeviceHubPort = 9898;

    private volatile LiteListener cblListener = null;
    private volatile boolean serverStarted = false;
    private volatile Manager manager = null;
    private volatile PublicKey serverPublicKey = null;
    private volatile ReplicationManager replicationManager = null;
    private Thread replicationManagerThread = null;

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
     * @param context
     * @param port
     */
    public void startServer(final Context context, final int port, final Proxy proxy) throws
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
        serverStarted = true;
        if (context == null) {
            throw new RuntimeException();
        }

        final KeyStore finalClientKeyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(context.getFilesDir());
        serverPublicKey = ThaliCryptoUtilities.getAppKeyFromKeyStore(finalClientKeyStore);

        new Thread(new Runnable() {
            public void run() {
                // Start the CouchDB Lite manager
                try {
                    ArrayList<AuthorizerFactory> authorizerFactoryArrayList = new ArrayList<AuthorizerFactory>();
                    BogusThaliAuthorizerFactory bogusThaliAuthorizerFactory =
                            new BogusThaliAuthorizerFactory(finalClientKeyStore, ThaliCryptoUtilities.DefaultPassPhrase,
                                    proxy);
                    authorizerFactoryArrayList.add(bogusThaliAuthorizerFactory);
                    AuthorizerFactoryManager authorizerFactoryManager =
                            new AuthorizerFactoryManager(authorizerFactoryArrayList);
                    ManagerOptions managerOptions =
                            new ManagerOptions(authorizerFactoryManager);
                    manager = new Manager(context, managerOptions);
                    // This creates the database used to store the keys of remote applications that are authorized to use
                    // the system in case it doesn't already exist.
                    manager.getDatabase(KeyDatabaseName);

                    // replication manager -- add to Thali bogus authorizer.
                    replicationManager = new ReplicationManager(manager);
                    bogusThaliAuthorizerFactory.setReplicationManager(replicationManager);


                    // Provision the TDH in its own key database so it can do replications to itself
                    // https://github.com/thaliproject/thali/issues/45
                    BogusAuthorizeCouchDocument.addDocViaManager(manager,
                            (RSAPublicKey) serverPublicKey);
                } catch (IOException e) {
                    Log.e(CblLogTags.TAG_THALI_LISTENER, "Manager failed to start", e);
                    return;
                } catch (CouchbaseLiteException e) {
                    Log.e(CblLogTags.TAG_THALI_LISTENER, "Manager failed to start", e);
                    return;
                }

                Properties tjwsProperties = new Properties();
                tjwsProperties.setProperty(Serve.ARG_ACCEPTOR_CLASS, TjwsSslAcceptor);
                tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, ThaliCryptoUtilities.PrivateKeyHolderFormat);
                tjwsProperties.setProperty(
                        SSLAcceptor.ARG_KEYSTOREFILE,
                        ThaliCryptoUtilities.getThaliKeyStoreFileObject(context.getFilesDir()).getAbsolutePath());
                tjwsProperties.setProperty(
                        SSLAcceptor.ARG_KEYSTOREPASS, new String(ThaliCryptoUtilities.DefaultPassPhrase));

                tjwsProperties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "true");

                //Allows us to bind to a particular address if that is interesting
                //tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, DefaultThaliDeviceHubAddress);

                // Needed to work around https://github.com/couchbase/couchbase-lite-java-listener/issues/40
                tjwsProperties.setProperty(Serve.ARG_KEEPALIVE_TIMEOUT, "1");

                BogusRequestAuthorization authorize = new BogusRequestAuthorization(KeyDatabaseName);

                replicationManagerThread = new Thread(replicationManager);
                replicationManagerThread.start();

                cblListener = new LiteListener(manager, port, tjwsProperties, authorize, null);
                cblListener.start();


            }
        }).start();

        try {
            Log.w(CblLogTags.TAG_THALI_LISTENER, "Local address is: " + getHttpKeys().getLocalMachineIPHttpKeyURL());
        } catch (InterruptedException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Failed trying to log address", e);
        } catch (UnknownHostException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Failed trying to log address", e);
        }
    }

    public void stopServer() {
        if (replicationManagerThread != null) {
            replicationManager.setShutdownServer();
        }
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

    public ReplicationManager getReplicationManager() throws InterruptedException {
        waitTillServerStarts();

        return replicationManager;
    }

    public HttpKeyTypes getHttpKeys() throws InterruptedException, UnknownHostException {
        // Local access address
        int portToUseForHttpKey = getSocketStatus().getPort();
        String host = InetAddress.getLocalHost().getHostAddress();
        HttpKeyURL localHttpKeyURL = new HttpKeyURL(serverPublicKey, host, portToUseForHttpKey, null, null, null);
        return new HttpKeyTypes(localHttpKeyURL);
    }
}
