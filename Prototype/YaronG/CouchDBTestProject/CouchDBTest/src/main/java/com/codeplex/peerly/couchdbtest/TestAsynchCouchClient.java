package com.codeplex.peerly.couchdbtest;

import android.os.AsyncTask;
import android.util.Log;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.android.http.AndroidHttpClient;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by yarong on 10/23/13.
 */
public class TestAsynchCouchClient extends AsyncTask<Integer, Void, Void> {
    @Override
    protected Void doInBackground(Integer... port) {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Log.e("TestAsynchCouchClient", "Somehow we got an exception on InetAddress, pretty amazing actually.", e);
            throw new RuntimeException(e);
        }

        HttpClient httpClient = new AndroidHttpClient.Builder().host(hostName).port(port[0]).build();
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
            Log.e("TestAsynchCouchClient", "The article we got back didn't match the one we sent.");
            throw new RuntimeException("oops");
        }

        Log.i("TestAsynchCouchClient", "Ye Ha!!! It worked!");

        return null;
    }
}
