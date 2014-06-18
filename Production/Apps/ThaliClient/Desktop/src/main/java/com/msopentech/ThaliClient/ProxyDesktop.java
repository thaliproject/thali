
package com.msopentech.ThaliClient;

import java.io.File;
import java.lang.System;
import java.io.*;
import com.msopentech.ThaliClientCommon.RelayWebServer;
import com.msopentech.thali.utilities.java.*;
import fi.iki.elonen.SimpleWebServer;

public class ProxyDesktop  {
    public RelayWebServer server;
    public SimpleWebServer host;

    public static void main(String[] rgs) throws InterruptedException {

        final ProxyDesktop instance = new ProxyDesktop();
        instance.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                instance.shutdown();
                System.out.println("Exiting!");
            }
        });

        while (true)
        {
            Thread.sleep(500);
        }

    }

    public void shutdown()
    {
        server.stop();
        host.stop();
    }

    public void initialize()
    {
        // Start the webservers
        try {
            server = new RelayWebServer(new JavaEktorpCreateClientBuilder(), new File(System.getProperty("user.dir")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        File webRoot = new File(new File(System.getProperty("user.dir")).getParent(), "web");
        System.out.println("Setting web root to: " + webRoot);
        host = new SimpleWebServer("localhost", 8081, webRoot, false);

        try {
            System.out.println("Starting WebServer on " + host.getListeningPort());
            host.start();

            System.out.println("Starting Relay on " + server.getListeningPort());
            server.start();
        } catch(IOException ioe) {
            System.out.println("Exception: " + ioe.toString());
        }
        System.out.println("Started.");
    }

}
