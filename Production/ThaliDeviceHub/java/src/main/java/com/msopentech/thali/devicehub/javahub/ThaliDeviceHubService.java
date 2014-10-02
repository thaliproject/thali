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

import com.couchbase.lite.JavaContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.CouchDBListener.java.JavaThaliListenerContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyManager;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class ThaliDeviceHubService {
    public static final String tdhJavaSubdirectory = ".thaliTdh";
    public static final String httpKeysFileName = "httpkeys";
    protected ThaliListener thaliListener = null;

    public void startService() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            IOException, InterruptedException {
        ThaliTestUtilities.turnCouchbaseLoggingTo11();
        thaliListener = new ThaliListener();

        File userHomeDirectoryRoot = new File(System.getProperty("user.home"), tdhJavaSubdirectory);
        File userTorOnionProxyRoot = new File(userHomeDirectoryRoot, "TorOnionProxy");

        JavaOnionProxyManager javaOnionProxyManager = new JavaOnionProxyManager(new JavaOnionProxyContext(userTorOnionProxyRoot));
        JavaContext context = new JavaThaliListenerContext(userHomeDirectoryRoot);
        thaliListener.startServer(context, ThaliListener.DefaultThaliDeviceHubPort, javaOnionProxyManager);

        // Writing out HttpKeys to root directory so relays and other clients can find them
        File httpKeysFile = new File(context.getRootDirectory(), httpKeysFileName);
        if (httpKeysFile.exists() && httpKeysFile.delete() == false) {
            throw new RuntimeException("Could not delete httpkey file " + httpKeysFile.getAbsolutePath());
        }

        if (httpKeysFile.createNewFile() == false) {
            throw new RuntimeException("could not create httpkey file " + httpKeysFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(httpKeysFile, thaliListener.getHttpKeys());
    }

    public void stopService() {
        if (thaliListener != null) {
            thaliListener.stopServer();
        }
    }
}
