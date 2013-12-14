package com.msopentech.thali.utilities.java.test;

import com.msopentech.thali.utilities.java.JavaFXBridgeManager;
import com.msopentech.thali.utilities.webviewbridge.BridgeCallBack;
import com.msopentech.thali.utilities.webviewbridge.BridgeHandler;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import javafx.application.Application;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by yarong on 12/12/13.
 */
public class JavaFXBridgeManagerTest {
    public static Object waitObject = new Object();
    public static String bridgeHandlerTestJs = "/BridgeHandlerTest.js";
    public enum pingStatus { unset, failed, success }
    public static pingStatus seenPing2 = pingStatus.unset;

    public static class AppHosting extends Application {
        @Override
        public void start(Stage stage) {
            WebView browser = new WebView();
            WebEngine webEngine = browser.getEngine();
            BridgeManager bridgeManager = new JavaFXBridgeManager(webEngine);
            BridgeHandler bridgeTest = new BridgeTest();
            bridgeManager.register(bridgeTest);
            webEngine.executeScript(BridgeManager.turnUTF8InputStreamToString(getClass().getResourceAsStream(bridgeHandlerTestJs)));
        }
    }

    public static class BridgeTest extends BridgeHandler {
        public BridgeTest() {
            super("Test");
            seenPing2 = pingStatus.unset;
        }

        @Override
        public void call(String jsonString, BridgeCallBack bridgeCallBack) {
            if (jsonString.equals("\"Ping\"")) {
                bridgeCallBack.successHandler("\"Pong\"");
            }

            if (jsonString.equals("\"Ping1\"")) {
                bridgeCallBack.failureHandler("\"Pong\"");
            }

            if (jsonString.equals("\"Ping2\"")) {
                seenPing2 = pingStatus.success;
                synchronized (waitObject) {
                    waitObject.notifyAll();
                }
            }
        }
    }

    @Test
    public void testBridgeManager() throws InterruptedException {
        Thread t = new Thread() {
            public void run() {
                Application.launch(AppHosting.class, new String[0]);
            }
        };
        t.start();
        synchronized (waitObject) {
            while(seenPing2 == pingStatus.unset) {
                waitObject.wait();
            }
        }
        assertEquals(seenPing2, pingStatus.success);
    }
}
