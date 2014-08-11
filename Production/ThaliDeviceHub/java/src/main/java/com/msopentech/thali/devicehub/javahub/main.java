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

package com.msopentech.thali.devicehub.javahub;

import com.msopentech.thali.devicehub.universal.ThaliDeviceHubUx;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class main extends Application {
    protected ThaliDeviceHubService thaliDeviceHubService;
    protected Scene scene;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        thaliDeviceHubService = new ThaliDeviceHubService();
        thaliDeviceHubService.startService();

        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();

        String rootHtmlFileAsString = ThaliDeviceHubUx.getRootUxHtmlAsString();
        webEngine.loadContent(rootHtmlFileAsString);

        // We will probably want to add functionality like the address book directly to the hub,
        // for now. The bridgeManager is needed for that. Check out JavaFXBridgeManagerTest in
        // JavaUtilities for an example.
        // BridgeManager bridgeManager = new JavaFXBridgeManager(webEngine);

        stage.setTitle("Thali Device Hub");
        scene = new Scene(browser);
        stage.setScene(scene);
        stage.show();
    }
}
