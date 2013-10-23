package com.codeplex.peerly.couchdbtest;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import com.couchbase.cblite.*;
import com.couchbase.cblite.listener.CBLHTTPServer;
import com.couchbase.cblite.listener.CBLListener;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        CBLListener cblListener = null;
        String filesDir = getFilesDir().getAbsolutePath();
        try {
            CBLServer server = new CBLServer (filesDir);

            cblListener = new CBLListener(server, 9898);
            cblListener.start();

            int port = cblListener.getListenPort();

            HttpClient httpClient = new StdHttpClient.Builder()
                                        .url("http://localhost:" + port)
                                        .build();
            CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClient);
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector("test", true);

            TestBlogClass testArticle = new TestBlogClass();
            String blogArticleName = "foo";
            String blogArticleContent = "AbcDef!";
            testArticle.setBlogArticleName(blogArticleName);
            testArticle.setBlogArticleContent(blogArticleContent);
            couchDbConnector.create(testArticle);

            String id = testArticle.getId();
            String revision = testArticle.getRevision();

            TestBlogClass checkArticle = couchDbConnector.get(TestBlogClass.class, id);

            if (checkArticle.getId() != id || checkArticle.getRevision() != revision || checkArticle.getBlogArticleName() != blogArticleName
                    || checkArticle.getBlogArticleContent() != blogArticleContent) {
                Log.e ("MainActivity", "The article we got back didn't match the one we sent.");
                throw new RuntimeException("oops");
            }


        } catch (IOException e) {
            Log.e ("MainActivity", "Error starting TDServer", e);
        } finally {
            if (cblListener != null) {
                cblListener.stop();
            }
        }


        Log.d("MainActivity", "Got this far, woohoo!");
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
