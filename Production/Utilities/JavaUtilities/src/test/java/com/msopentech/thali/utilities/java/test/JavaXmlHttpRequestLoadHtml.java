/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package com.msopentech.thali.utilities.java.test;

import com.msopentech.thali.utilities.xmlhttprequestbridge.BridgeTestLoadHtml;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;

public class JavaXmlHttpRequestLoadHtml implements BridgeTestLoadHtml {
    private final WebView browser;
    private final Stage stage;

    public JavaXmlHttpRequestLoadHtml(WebView browser, Stage stage) {
        this.browser = browser;
        this.stage = stage;
    }

    @Override
    public void LoadWebPage(String url) {
        stage.setTitle("I am a test of JavaXmlHttpRequestLoad");
        Scene scene = new Scene(browser);
        stage.setScene(scene);


        // taken from http://stackoverflow.com/questions/17387981/javafx-webview-webengine-firebuglite-or-some-other-debugger
//        browser.getEngine().documentProperty().addListener(new ChangeListener<Document>() {
//            @Override public void changed(ObservableValue<? extends Document> prop, Document oldDoc, Document newDoc) {
//                enableFirebug(browser.getEngine());
//            }
//        });

        browser.getEngine().load(url);
        stage.show();
    }

    /**
     * Enables Firebug Lite for debugging a webEngine.
     * @param engine the webEngine for which debugging is to be enabled.
     */
//    private static void enableFirebug(final WebEngine engine) {
//        engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
//    }
}
