package com.codeplex.thali.utilities.test;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import android.util.Log;
import com.codeplex.thali.utilities.ThaliCryptoUtilities;
import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.CBLStatus;
import com.couchbase.cblite.listener.CBLListener;
import com.couchbase.cblite.listener.CBLSocketStatus;
import com.couchbase.cblite.router.CBLRequestAuthorization;
import com.couchbase.cblite.router.CBLURLConnection;

import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

/**
 * Created by yarong on 11/13/13.
 */
public class ThaliTestServer {
    public CBLListener cblListener = null;

    private File filesDir = null;
    private File keystoreFile = null;
    private RSAPublicKey serverRSAPublicKey = null;

    private final RSAPublicKey clientRSAPublicKey;

    private final String ipAddress;
    private final int port;
    private final String tjwsSslAcceptor = "com.couchbase.cblite.listener.CBLSSLAcceptor";
    private final String deviceKeyAlias = "com.codeplex.peerly.names.devicealias";
    private final boolean useSSL;

    /**
     *
     * @param filesDir The directory where the key store file should be stored
     */
    public ThaliTestServer(File filesDir, RSAPublicKey clientRSAPublicKey, String ipAddress, int port, boolean useSSL) {
        this.filesDir = filesDir;
        this.clientRSAPublicKey = clientRSAPublicKey;
        this.port = port;
        this.ipAddress = ipAddress;
        this.useSSL = useSSL;
    }

    public class Authorize implements CBLRequestAuthorization {
        private final RSAPublicKey clientKey;

        public Authorize(RSAPublicKey clientKey) {
            this.clientKey = clientKey;
        }

        private boolean AuthorizationCheck(SSLSession sslSession) {
            // No unauthenticated requests
            if (sslSession == null) {
                return false;
            }

            try {
                javax.security.cert.X509Certificate[] certChain = sslSession.getPeerCertificateChain();
                return ThaliCryptoUtilities.RsaPublicKeyComparer((RSAPublicKey)certChain[certChain.length - 1].getPublicKey(), this.clientKey);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean Authorize(CBLServer cblServer, CBLURLConnection cblurlConnection) {
            SSLSession sslSession = cblurlConnection.getSSLSession();
            boolean result = AuthorizationCheck(sslSession);
            if (result == false) {
                InsecureConnection(cblurlConnection);
            }
            return result;
        }

        private void InsecureConnection(CBLURLConnection cblurlConnection) {
            cblurlConnection.setResponseCode(CBLStatus.FORBIDDEN);
            try {
                cblurlConnection.getResponseOutputStream().close();
            } catch (IOException e) {
                Log.e("ThaliTestServer", "Error closing empty output stream");
            }
        }
    }

    public void ValidateServerStatus(String expectedHostIPAddress, int expectedPort) {
        CBLSocketStatus cblSocketStatus = this.cblListener.getSocketStatus();
        if (cblSocketStatus.isBound() == false) {
            throw new RuntimeException("server isn't bound!");
        }

        if (cblSocketStatus.isClosed() == true) {
            throw new RuntimeException("server is closed!");
        }

        String actualHostName = cblSocketStatus.getInetAddress().getHostAddress();
        if (actualHostName.equals(expectedHostIPAddress) == false)
        {
            throw new RuntimeException("Expected hostname was " + expectedHostIPAddress + " but got instead " + actualHostName);
        }

        int actualPort = cblSocketStatus.getPort();
        if (actualPort != expectedPort) {
            throw new RuntimeException("Expected port was " + expectedPort + " but got instead " + actualPort);
        }
    }

    /**
     *
     * @return The port the server was started on
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public int start() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        // Start the CouchDB Lite server
        CBLServer server = new CBLServer (filesDir.getAbsolutePath());

        if (useSSL) {
            // Create key store
            KeyPair serverKeyPair = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair();

            this.serverRSAPublicKey = (RSAPublicKey) serverKeyPair.getPublic();

            KeyStore keyStore = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(serverKeyPair, deviceKeyAlias, ThaliCryptoUtilities.DefaultPassPhrase);
            keystoreFile = File.createTempFile("keyStore", ".keyStore", filesDir);
            FileOutputStream fileOutputStream = new FileOutputStream(keystoreFile);
            keyStore.store(fileOutputStream, ThaliCryptoUtilities.DefaultPassPhrase);

            Properties tjwsProperties = new Properties();
            tjwsProperties.setProperty(Serve.ARG_ACCEPTOR_CLASS, tjwsSslAcceptor);
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, ThaliCryptoUtilities.PrivateKeyHolderFormat);
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, keystoreFile.getAbsolutePath());
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, new String(ThaliCryptoUtilities.DefaultPassPhrase));
            tjwsProperties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "true");
            tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, ipAddress);

            cblListener = new CBLListener(server, port, tjwsProperties, new Authorize(this.clientRSAPublicKey));
        } else {
            Properties tjwsProperties = new Properties();
            tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, ipAddress);
            cblListener = new CBLListener(server, port, tjwsProperties, null);
        }

        cblListener.start();

        return cblListener.getSocketStatus().getPort();
    }

    public void close() {
        if (cblListener != null) {
            cblListener.stop();
        }

        if (keystoreFile != null) {
            if (keystoreFile.delete() == false) {
                throw new RuntimeException("The keystore file didn't delete successfully! We don't want a bunch of trash collecting so please investigate.");
            }
        }
    }

    public RSAPublicKey getServerRSAPublicKey() {
        return this.serverRSAPublicKey;
    }
}
