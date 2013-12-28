package com.msopentech.thali.CouchDBListener;

import java.io.File;

/**
 * Created by yarong on 12/27/13.
 */
public abstract class ThaliListener {
    public static final String KeyDatabaseName = "ThaliPrincipalDatabase";
    public static final String TjwsSslAcceptor = "com.msopentech.thali.CouchDBListener.AcceptAllClientCertsSSLAcceptor";
    public static final String DefaultThaliDeviceHubAddress = "127.0.0.1";
    public static final int DefaultThaliDeviceHubPort = 9898;

    public abstract void startServer(final File filesDir, final int port);
    public abstract void stopServer();
    //TODO: When we get Listener to be in pure Java we need to put getSocketStatus in.
    //public abstract SocketStatus getSocketStatus();
}
