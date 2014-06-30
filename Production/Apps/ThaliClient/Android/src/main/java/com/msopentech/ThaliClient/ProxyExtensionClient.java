/* Copyright (c) 2014 Intel Corporation. All rights reserved.
 * Use of this source code is governed by an Apache v2 license that can be
 * found in the LICENSE-APACHE-V2 file. */
package com.msopentech.ThaliClient;

import org.xwalk.app.runtime.extension.XWalkExtensionClient;
import org.xwalk.app.runtime.extension.XWalkExtensionContextClient;
import java.lang.System;
import java.io.*;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.ThaliAndroidClientUtilities.AndroidEktorpCreateClientBuilder;
import android.content.Context;
import android.os.AsyncTask;
import android.content.ContentResolver;

public class ProxyExtensionClient extends XWalkExtensionClient {
    private ContentResolver resolver;

    public RelayWebServer server;

    public ProxyExtensionClient(String name, String jsApiContent, XWalkExtensionContextClient xwalkContext) {
        super(name, jsApiContent, xwalkContext);
        this.resolver = xwalkContext.getContext().getContentResolver();

        new RelayTask().execute(xwalkContext.getContext());


    }

    // Stop the server
    @Override
    public void onDestroy()
    {
      super.onDestroy();
      if (server != null)
          server.stop();
    }


    private class RelayTask extends AsyncTask<Context, Void, Void> {
        private Context context;

        protected Void doInBackground(Context... context) {
            if (context.length >= 0)
            this.context = context[0];
            initialize();
            return null;
        }

        private void initialize()
        {
            // Start the webserver
            try {
                server = new RelayWebServer(new AndroidEktorpCreateClientBuilder(), this.context.getDir("keystore", Context.MODE_WORLD_WRITEABLE ));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                server.start();
            } catch(IOException ioe) {
                System.out.println("Exception: " + ioe.toString());
            }
            System.out.println("Started.");
        }
    }

}
