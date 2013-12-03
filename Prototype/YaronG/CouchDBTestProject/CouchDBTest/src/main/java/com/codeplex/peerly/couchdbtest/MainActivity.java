package com.codeplex.peerly.couchdbtest;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.*;
import com.codeplex.peerly.couchdbserverandroid.R;
import com.msopentech.thali.utilities.universal.CouchDBDocumentKeyClassForTests;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliPublicKeyComparer;
import com.msopentech.thali.utilities.universal.ThaliTestEktorpClient;
import com.couchbase.cblite.*;
import com.couchbase.cblite.listener.CBLListener;
import com.couchbase.cblite.router.CBLRequestAuthorization;
import com.couchbase.cblite.router.CBLRouter;
import com.couchbase.cblite.router.CBLURLConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

public class MainActivity extends Activity {
    private CBLListener cblListener = null;
    private final int defaultCouchPort = 9898;
    private final String defaultCouchAddress = "127.0.0.1";
    private final String tjwsSslAcceptor = "com.codeplex.peerly.couchdbtest.ThaliSelfSignedMutualAuthSSLAcceptor";
    private final String deviceKeyAlias = "com.codeplex.peerly.names.devicealias";
    private final String keystoreFileName = "com.codeplex.peerly.names.keystore";
    private final Logger Log = LoggerFactory.getLogger(MainActivity.class);

    public class Authorize implements CBLRequestAuthorization {
        @Override
        public boolean Authorize(CBLServer cblServer, CBLURLConnection cblurlConnection) {
            List<String> pathSegments = CBLRouter.splitPath(cblurlConnection.getURL());

            // For now all we really care about are attempts to access the data database.
            if (pathSegments.size() == 0 || pathSegments.get(0).equals(ThaliTestEktorpClient.TestDatabaseName) == false) {
                return true;
            }

            CBLDatabase keyDatabase = cblServer.getExistingDatabaseNamed(ThaliTestEktorpClient.KeyDatabaseName);

            // No database? Then no one is authorized.
            if (keyDatabase == null) {
                InsecureConnection(cblurlConnection);
                return false;
            }

            CBLRevisionList revisionList = keyDatabase.getAllRevisionsOfDocumentID(ThaliTestEktorpClient.KeyId, true);
            EnumSet<CBLDatabase.TDContentOptions> tdContentOptionses = EnumSet.noneOf(CBLDatabase.TDContentOptions.class);
            CBLRevision revision =
                    keyDatabase.getDocumentWithIDAndRev(
                            ThaliTestEktorpClient.KeyId,
                            revisionList.getAllRevIds().get(revisionList.getAllRevIds().size() - 1),
                            tdContentOptionses);

            ObjectMapper mapper = new ObjectMapper();
            try {
                CouchDBDocumentKeyClassForTests keyClassForTests = mapper.readValue(revision.getJson(), CouchDBDocumentKeyClassForTests.class);
                if (CouchDBDocumentKeyClassForTests.RSAKeyType.equals(keyClassForTests.getKeyType()) == false) {
                    // A 500 would be more appropriate but we are just testing
                    InsecureConnection(cblurlConnection);
                    return false;
                }

                SSLSession sslSession = cblurlConnection.getSSLSession();
                try {
                    javax.security.cert.X509Certificate[] certChain = sslSession.getPeerCertificateChain();
                    if (new ThaliPublicKeyComparer(certChain[certChain.length - 1]
                            .getPublicKey())
                            .KeysEqual(keyClassForTests.generatePublicKey()) == false) {
                        InsecureConnection(cblurlConnection);
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    // A 500 would be better
                    InsecureConnection(cblurlConnection);
                    return false;
                }
            } catch (IOException e) {
                InsecureConnection(cblurlConnection);
                return false;
            }
        }

        private void InsecureConnection(CBLURLConnection cblurlConnection) {
            cblurlConnection.setResponseCode(CBLStatus.FORBIDDEN);
            try {
                cblurlConnection.getResponseOutputStream().close();
            } catch (IOException e) {
                android.util.Log.e("ThaliTestServer", "Error closing empty output stream");
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        MakeSureDeviceKeyStoreExists();

        String filesDir = getFilesDir().getAbsolutePath();
        try {
            // Start the CouchDB Lite server
            CBLServer server = new CBLServer (filesDir);

            Properties tjwsProperties = new Properties();
            tjwsProperties.setProperty(Serve.ARG_ACCEPTOR_CLASS, tjwsSslAcceptor);
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, ThaliCryptoUtilities.PrivateKeyHolderFormat);
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, GetKeyStoreAbsolutePath());
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, new String(ThaliCryptoUtilities.DefaultPassPhrase));

            tjwsProperties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "true");

            tjwsProperties.setProperty(Serve.ARG_BINDADDRESS, defaultCouchAddress);

            Authorize authorize = new Authorize();

            cblListener = new CBLListener(server, defaultCouchPort, tjwsProperties, authorize);

            cblListener.start();
        } catch (IOException e) {
            Log.error("Error starting TDServer", e);
        }

        Log.debug("Got this far, woohoo!");
    }

    /**
     * Return the absolute path of the Peerly device key store.
     *
     * I had wanted this to just be a property of the class but getFilesDir() doesn't seem to be
     * initialized until onCreate is called so setting the value either as a property or via
     * the constructor won't work. Hence this method.
     * @return
     */
    private String GetKeyStoreAbsolutePath() {
       return new File(getFilesDir(), keystoreFileName).getAbsolutePath();
    }

    /**
     * If no key store exists to hold the device's keying information than this method
     * will create on.
     */
    private void MakeSureDeviceKeyStoreExists() {
        File keyStoreFile = new File(GetKeyStoreAbsolutePath());

        if (keyStoreFile.exists()) {
            // TODO: We need to check if the device's cert is expired and renew it, but this will probably require getting a new root chain, so let's wait until that's figured out
            return;
        }

        KeyStore keyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                        ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair(), deviceKeyAlias, ThaliCryptoUtilities.DefaultPassPhrase);

        // TODO: I really need to figure out if I can safely use Java 7 features like try with resources and Android, the fact that Android Studio defaults to not support Java 7 makes me very nervous
        FileOutputStream fileOutputStream = null;
        try {
            // Yes this can swallow exceptions (if you got an exception inside this try and then the finally has an exception, but given what I'm doing here I don't care.
            try {
                fileOutputStream =  new FileOutputStream(keyStoreFile);
                keyStore.store(fileOutputStream, ThaliCryptoUtilities.DefaultPassPhrase);
            } finally {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        if (cblListener != null) {
            cblListener.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
