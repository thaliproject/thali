package com.msopentech.thali.utilities.android.test;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.listener.CBLListener;
import com.couchbase.cblite.listener.CBLSocketStatus;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.util.Properties;

/**
 * Created by yarong on 11/26/13.
 */
public class ThaliTestServer {
    private static final String DeviceKeyAlias = "deviceKeyAlias";
    private static final String KeystoreFileName = "com.msopentech.thali.name.keystore";
    private static final String TjwsSslAcceptor = "com.msopentech.thali.utilities.android.test.ThaliSelfSignedMutualAuthSSLAcceptor";
    private static final String DefaultCouchAddress = "127.0.0.1";

    private CBLListener cblListener = null;
    private boolean serverStarted = false;
    private final Logger Log = LoggerFactory.getLogger(ThaliTestServer.class);
    private KeyStore serverKeyStore;

    private File getKeyStoreFileObject(File filesDir) {
        return new File(filesDir, KeystoreFileName).getAbsoluteFile();
    }

    /**
     * If no key store exists to hold the device's keying information than this method
     * will create on.
     */
    private void MakeSureDeviceKeyStoreExists(File filesDir) {
        File keyStoreFile = getKeyStoreFileObject(filesDir);

        if (keyStoreFile.exists()) {
            keyStoreFile.delete();
        }

        serverKeyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                        ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair(), DeviceKeyAlias, ThaliCryptoUtilities.DefaultPassPhrase);

        // TODO: I really need to figure out if I can safely use Java 7 features like try with resources and Android, the fact that Android Studio defaults to not support Java 7 makes me very nervous
        FileOutputStream fileOutputStream = null;
        try {
            // Yes this can swallow exceptions (if you got an exception inside this try and then the finally has an exception, but given what I'm doing here I don't care.
            try {
                fileOutputStream =  new FileOutputStream(keyStoreFile);
                serverKeyStore.store(fileOutputStream, ThaliCryptoUtilities.DefaultPassPhrase);
            } finally {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void waitTillServerStarts() throws InterruptedException {
        while (cblListener == null && serverStarted) {
            Thread.sleep(100);
        }

        if (serverStarted == false) {
            throw new RuntimeCryptoException("server wasn't started or was stopped.");
        }
    }

    public PublicKey getServerPublicKey() throws InterruptedException, KeyStoreException {
        waitTillServerStarts();

        return serverKeyStore.getCertificate(DeviceKeyAlias).getPublicKey();
    }

    public void startServer(final File filesDir) {
        serverStarted = true;
        if (filesDir == null) {
            throw new RuntimeException();
        }

        MakeSureDeviceKeyStoreExists(filesDir);

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Start the CouchDB Lite server
                    CBLServer server = new CBLServer (filesDir.getAbsolutePath());

                    Properties tjwsProperties = new Properties();
                    tjwsProperties.setProperty(Serve.ARG_ACCEPTOR_CLASS, TjwsSslAcceptor);
                    tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, ThaliCryptoUtilities.PrivateKeyHolderFormat);
                    tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, getKeyStoreFileObject(filesDir).getAbsolutePath());
                    tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, new String(ThaliCryptoUtilities.DefaultPassPhrase));

                    tjwsProperties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "true");

                    tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, DefaultCouchAddress);

                    ThaliTestServiceAuthorize authorize = new ThaliTestServiceAuthorize();

                    cblListener = new CBLListener(server, 0, tjwsProperties, authorize);

                    cblListener.start();
                } catch (IOException e) {
                    Log.error("Error starting TDServer", e);
                }
            }
        }).start();
    }

    public void stopServer() {
        if (cblListener != null) {
            cblListener.stop();
        }
        serverStarted = false;
    }

    public CBLSocketStatus getCBLSocketStatus() throws InterruptedException {
        waitTillServerStarts();

        return cblListener.getSocketStatus();
    }
}
