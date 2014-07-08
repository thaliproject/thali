
package com.msopentech.ThaliClient;

import com.msopentech.thali.nanohttp.SimpleWebServer;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;

import java.awt.*;
import java.io.Console;
import java.io.File;
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

    public void initialize()
    {
        // Initialize the relay
        try {
            server = new RelayWebServer(new JavaEktorpCreateClientBuilder(), new File(System.getenv("APP_HOME")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize the local web server
        File webRoot = new File(new File(System.getenv("APP_HOME")), "web");
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
