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

package com.msopentech.ThaliClient;

import com.msopentech.thali.nanohttp.SimpleWebServer;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;

import java.awt.*;
import java.io.Console;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// TODO: Convert this to trivial swing app which goes to system tray
// per http://docs.oracle.com/javase/tutorial/uiswing/misc/systemtray.html

public class ProxyDesktop  {
    private static final int localWebserverPort = 58001;

    public RelayWebServer server;
    public SimpleWebServer host;

    public static void main(String[] rgs) throws InterruptedException, URISyntaxException, IOException {

        final ProxyDesktop instance = new ProxyDesktop();
        instance.initialize();

        // Attempt to launch the default browser to our page
        if(Desktop.isDesktopSupported())
        {
            Desktop.getDesktop().browse(new URI("http://localhost:" + localWebserverPort));
        }

        // Register to shutdown the server properly from a sigterm
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                instance.shutdown();
            }
        });

        // Let user press enter to kill the console session
        Console console = System.console();
        if (console != null) {
            console.format("\nPress ENTER to exit.\n");
            console.readLine();
            instance.shutdown();
        }
        else
        {
            // Don't exit on your own when running without a console (debugging in an IDE).
            while (true)
            {
                Thread.sleep(500);
            }
        }
    }

    public void shutdown()
    {
        server.stop();
        host.stop();
    }

    public void initialize() throws URISyntaxException, MalformedURLException {
        // Initialize the relay
        String filePath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File file = new File(filePath);
        Path applicationPath = file.toPath();
        Path webPath = applicationPath.getParent().getParent().resolve("web");

        try {
            server = new RelayWebServer(new JavaEktorpCreateClientBuilder(), webPath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize the local web server
        File webRoot = webPath.toFile();
        System.out.println("Setting web root to: " + webRoot);
        host = new SimpleWebServer("localhost", localWebserverPort, webRoot, false);

        // Start both listeners
        try {
            System.out.println("Starting WebServer at http://localhost:" + localWebserverPort);
            host.start();

            System.out.println("Starting Relay on http://" + RelayWebServer.relayHost + ":" + RelayWebServer.relayPort);
            server.start();
        } catch(IOException ioe) {
            System.out.println("Exception: " + ioe.toString());
        }
        System.out.println("Started.");
    }

}
