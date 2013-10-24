package com.codeplex.peerly.couchdbtest;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.listener.CBLListener;

import java.io.IOException;

public class MainActivity extends Activity {
    private CBLListener cblListener = null;
    private int defaultCouchPort = 9898;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        String filesDir = getFilesDir().getAbsolutePath();
        try {
            // Start the CouchDB Lite server
            CBLServer server = new CBLServer (filesDir);
            cblListener = new CBLListener(server, defaultCouchPort);
            cblListener.start();

            if (cblListener.getStatus().equals("0") == false)
            {
                throw new RuntimeException("CouchDB Server didn't start up correctly, alas you will have to check the log to see why.");
            }

            // Lets see if the server is running!
            int port = cblListener.getListenPort();
            //new TestAsynchCouchClient().execute(port);

        } catch (IOException e) {
            Log.e ("MainActivity", "Error starting TDServer", e);
        }

        Log.d("MainActivity", "Got this far, woohoo!");
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
