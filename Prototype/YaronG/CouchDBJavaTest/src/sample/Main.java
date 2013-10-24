package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);

        final Logger log = LoggerFactory.getLogger(Main.class);

        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Log.e("TestAsynchCouchClient", "Somehow we got an exception on InetAddress, pretty amazing actually.", e);
            throw new RuntimeException(e);
        }

        HttpClient httpClient = new StdHttpClient.Builder().host(hostName).port(5984).build();
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
    }
}
