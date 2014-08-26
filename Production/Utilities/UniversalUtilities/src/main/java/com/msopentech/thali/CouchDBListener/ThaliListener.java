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
import com.couchbase.lite.Context;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.ManagerOptions;
import com.couchbase.lite.auth.AuthorizerFactory;
import com.couchbase.lite.auth.AuthorizerFactoryManager;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.listener.SocketStatus;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.utilities.universal.CblLogTags;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.bouncycastle.crypto.RuntimeCryptoException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
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
    private volatile OnionProxyManager onionProxyManager = null;
    private volatile HttpKeyURL hiddenServiceAddress = null;
    private volatile Proxy socksProxy = null;

    /**
     * waitTillToOnionProxyStarts() + wait for hidden service to be registered.
     * @throws InterruptedException
     */
    public void waitTillHiddenServiceStarts() throws InterruptedException, IOException {
        waitTillTorOnionProxyStarts();

        while (hiddenServiceAddress == null) {
            Log.v(CblLogTags.TAG_THALI_LISTENER, "Waiting for Hidden service to be registered");
            Thread.sleep(100);
        }
    }

    /**
     * waitTillListenerStarts() + Waits for Tor infrastructure to boot strap and be available for SOCKS communication.
     * @throws InterruptedException
     * @throws IOException
     */
    public void waitTillTorOnionProxyStarts() throws InterruptedException, IOException {
        waitTillListenerStarts();

        while (onionProxyManager.isRunning() == false || onionProxyManager.isNetworkEnabled() == false ||
                socksProxy == null) {
            Log.v(CblLogTags.TAG_THALI_LISTENER, "Waiting for Tor Onion Proxy to start");
            Thread.sleep(100);
        }
    }

    /**
     * Waits until the local CouchDB server is up and running
     * @throws InterruptedException
     */
    public void waitTillListenerStarts() throws InterruptedException {
        while (cblListener == null && serverStarted) {
            Log.v(CblLogTags.TAG_THALI_LISTENER, "Waiting for Listener to start");
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
     * @param onionProxyManager
     */
    public void startServer(final Context context, final int port,
                            final OnionProxyManager onionProxyManager) throws
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException{
        this.onionProxyManager = onionProxyManager;
        serverStarted = true;
        if (context == null) {
            throw new RuntimeException();
        }

        final KeyStore finalClientKeyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(context.getFilesDir());
        serverPublicKey = ThaliCryptoUtilities.getAppKeyFromKeyStore(finalClientKeyStore);

        new Thread(new Runnable() {
            public void run() {
                // First start the Tor listener so we know what SOCKS proxy port we are listening on
                try {
                    if (onionProxyManager.startWithRepeat(120, 5) == false) {
                        Log.e(CblLogTags.TAG_THALI_LISTENER, "Could not start Onion Proxy Manager!");
                        stopServer();
                    }
                    onionProxyManager.enableNetwork(true);
                    socksProxy = new Proxy(
                            Proxy.Type.SOCKS,
                            new InetSocketAddress("127.0.0.1", onionProxyManager.getIPv4LocalHostSocksPort()));

                    // Now we can configure the listener with the proxy to use to talk to SOCKS
                    if (configureManagerObjectForListener(finalClientKeyStore, socksProxy, context)) {
                        configureListener(context, port);
                    }

                    // Now we can configure the hidden service because we know what local port the listener is using
                    String onionDomainName =
                            onionProxyManager.publishHiddenService(DefaultThaliDeviceHubPort, getSocketStatus().getPort());
                    hiddenServiceAddress =
                            new HttpKeyURL(serverPublicKey, onionDomainName, DefaultThaliDeviceHubPort, null, null, null);
                } catch (InterruptedException e) {
                    Log.e(CblLogTags.TAG_THALI_LISTENER, "Could not start TOR Onion Proxy", e);
                } catch (IOException e) {
                    Log.e(CblLogTags.TAG_THALI_LISTENER, "Could not start TOR Onion Proxy", e);
                }
            }
        }).start();

        try {
            // Can't do this in Android because it would be on the main thread and caused an AndroidBlockGuardPolicy.onNetwork
            // If we ever care we can always just run it on a separate thread but it doesn't seem worth it.
            if (OsData.getOsType() != OsData.OsType.Android ) {
                Log.w(CblLogTags.TAG_THALI_LISTENER, "Local address is: " + getHttpKeys().getLocalMachineIPHttpKeyURL());
            }
        } catch (InterruptedException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Failed trying to log address", e);
        } catch (UnknownHostException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Failed trying to log address", e);
        } catch (IOException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Failed trying to log address", e);
        }
    }

    private void configureListener(Context context, int port) {
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

        replicationManager.start();

        cblListener = new LiteListener(manager, port, tjwsProperties, authorize, null);
        cblListener.start();
    }

    /**
     * Configured Manager object
     * @param finalClientKeyStore
     * @param proxy
     * @param context
     * @return True if the config worked and false if there is a problem. Since this method is called from inside of
     * its own thread there is no point in throwing in case of an error. We can only log.
     */
    private boolean configureManagerObjectForListener(KeyStore finalClientKeyStore, Proxy proxy, Context context) {
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
            replicationManager = new ReplicationManager(manager, serverPublicKey);
            bogusThaliAuthorizerFactory.setReplicationManager(replicationManager);

            // Provision the TDH in its own key database so it can do replications to itself
            // https://github.com/thaliproject/thali/issues/45
            BogusAuthorizeCouchDocument.addDocViaManager(manager,
                    (RSAPublicKey) serverPublicKey);
        } catch (IOException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Manager failed to start", e);
            return false;
        } catch (CouchbaseLiteException e) {
            Log.e(CblLogTags.TAG_THALI_LISTENER, "Manager failed to start", e);
            return false;
        }
        return true;
    }

    public void stopServer() {
        if (replicationManager != null) {
            replicationManager.stop();
        }

        if (cblListener != null) {
            cblListener.stop();
        }

        if (onionProxyManager != null) {
            try {
                onionProxyManager.stop();
            } catch (IOException e) {
                Log.e(CblLogTags.TAG_THALI_LISTENER, "Something went wrong while stopping the Tor Onion Proxy", e);
            }
        }

        serverStarted = false;
    }

    public SocketStatus getSocketStatus() throws InterruptedException {
        waitTillListenerStarts();

        return cblListener.getSocketStatus();
    }

    public Manager getManager() throws InterruptedException {
        waitTillListenerStarts();

        return manager;
    }

    public ReplicationManager getReplicationManager() throws InterruptedException {
        waitTillListenerStarts();

        return replicationManager;
    }

    public Proxy getSocksProxy() throws InterruptedException, IOException {
        waitTillTorOnionProxyStarts();

        return socksProxy;
    }

    public HttpKeyTypes getHttpKeys() throws InterruptedException, IOException {
        waitTillHiddenServiceStarts();
        // Local access address
        int portToUseForHttpKey = getSocketStatus().getPort();
        String host = InetAddress.getLocalHost().getHostAddress();
        HttpKeyURL localHttpKeyURL = new HttpKeyURL(serverPublicKey, host, portToUseForHttpKey, null, null, null);
        return new HttpKeyTypes(localHttpKeyURL, hiddenServiceAddress, onionProxyManager.getIPv4LocalHostSocksPort());
    }

    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
}
