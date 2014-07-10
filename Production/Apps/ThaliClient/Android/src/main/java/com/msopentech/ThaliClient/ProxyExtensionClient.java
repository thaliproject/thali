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
