package com.codeplex.peerly.couchdbtest;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.codeplex.peerly.common.KeyStoreManagement;
import com.codeplex.peerly.couchdbserverandroid.R;
import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.listener.CBLListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;

public class MainActivity extends Activity {
    private CBLListener cblListener = null;
    private int defaultCouchPort = 9898;
    private String tjwsSslAcceptor = "Acme.Serve.SSLAcceptor";
    private String deviceKeyAlias = "com.codeplex.peerly.names.devicealias";
    private String keystoreFileName = "com.codeplex.peerly.names.keystore";
    private final Logger Log = LoggerFactory.getLogger(MainActivity.class);


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
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, KeyStoreManagement.PrivateKeyHolderFormat);
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, new File(getFilesDir(), keystoreFileName).getAbsolutePath());
            tjwsProperties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, KeyStoreManagement.DefaultPassPhrase.toString());

            cblListener = new CBLListener(server, defaultCouchPort, tjwsProperties);

            cblListener.start();

            if (cblListener.serverStatus() != 0)
            {
                throw new RuntimeException("CouchDB Server didn't start up correctly, alas you will have to check the log to see why.");
            }

            // Lets see if the server is running!
            int port = cblListener.getListenPort();
            new TestAsynchCouchClient().execute(port);

        } catch (IOException e) {
            Log.error("Error starting TDServer", e);
        }

        Log.debug("Got this far, woohoo!");
    }

    /**
     * If no key store exists to hold the device's keying information than this method
     * will create on.
     * TODO: We need to check if the device's cert is expired and renew it, but this will probably require getting a new root chain, so let's wait until that's figured out
     */
    private void MakeSureDeviceKeyStoreExists() {
        String keyStoreFilePath = new File(getFilesDir(), keystoreFileName).getAbsolutePath();

        if (new File(keyStoreFilePath).exists()) {
            return;
        }

        KeyStore keyStore =
                KeyStoreManagement.CreatePKCS12KeyStoreWithNewPublicPrivateKeyPair(
                        KeyStoreManagement.GeneratePeerlyAcceptablePublicPrivateKeyPair(), deviceKeyAlias, KeyStoreManagement.DefaultPassPhrase);

        try {
            keyStore.store(new FileOutputStream(keyStoreFilePath), KeyStoreManagement.DefaultPassPhrase);
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
