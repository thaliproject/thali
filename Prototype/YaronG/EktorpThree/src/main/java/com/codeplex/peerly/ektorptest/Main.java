package com.codeplex.peerly.ektorptest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static void test() {
        final Logger log = LoggerFactory.getLogger(Main.class);

        String hostName = "127.0.0.1";

        int port = 9898; // Port I use on Android
        //int port = 5984; // Default couch port
        HttpClient httpClient = new StdHttpClient.Builder().host(hostName).port(port).proxy("127.0.0.1").proxyPort(8888).build(); // Routing through Fiddler2
        //HttpClient httpClient = new StdHttpClient.Builder().host(hostName).port(port).build(); // Direct connection

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

        if (checkArticle.getId().equals(id) == false || checkArticle.getRevision().equals(revision) == false ||
                checkArticle.getBlogArticleName().equals(blogArticleName) == false ||
                checkArticle.getBlogArticleContent().equals(blogArticleContent) == false) {
            log.error("The article we got back didn't match the one we sent.");
            throw new RuntimeException("oops");
        }

        log.error("Ye Ha!!! It worked!");
    }

    public static void main(String[] args) {
        test();
    }
}
