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

package com.msopentech.thali.local.utilities;

import com.couchbase.lite.Context;
import com.msopentech.thali.CouchDBListener.HttpKeyTypes;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.CouchDBListener.java.JavaThaliListenerContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyManager;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.test.RelayWebServerTest;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;

public class UtilitiesTestCase extends TestCase {
    // These are used for NetworkPerfTests, I put them here so they don't get nuked when we copy
    // over from Android to Java. Normally this variable is set to null but when given a value it's used to
    // skip creating a new Thali listener on each test pass and instead just listen to an external listener that is
    // always running. That way we don't have to start and stop listeners on each test pass. This is intended
    // for when we are doing manual testing and exploring different perf issues.
    public static HttpKeyTypes perfListenerHttpKeyTypes = null;
    //        new HttpKeyTypes(
//            new HttpKeyURL("httpkey://192.168.1.188:9898/rsapublickey:65537.22912332915818678422150816008567595304572530270766238859922343032791612966824557932947659960333351153388435158284666309248825175974911964431170141623906931040856664130981842177060601883093191311741405530353180334971823580750344435314197473833181898842010566738993075259001808566463348027523141542809926498335324273802899607724831414078729370096517958658346374270205621071263361779683051242363287222987735418011187771204718883145520252089502815273843893528710808519526473112874774561851138101896806013797598277895539034330328094877276084533967507831910523283288815798592996543256860992426377095371666673172691091922277/"),
//            new HttpKeyURL("httpkey://ku7mzpobige5mljf.onion:9898/rsapublickey:65537.22912332915818678422150816008567595304572530270766238859922343032791612966824557932947659960333351153388435158284666309248825175974911964431170141623906931040856664130981842177060601883093191311741405530353180334971823580750344435314197473833181898842010566738993075259001808566463348027523141542809926498335324273802899607724831414078729370096517958658346374270205621071263361779683051242363287222987735418011187771204718883145520252089502815273843893528710808519526473112874774561851138101896806013797598277895539034330328094877276084533967507831910523283288815798592996543256860992426377095371666673172691091922277/"),
//            0
//        );
    public static HttpKeyURL noTorHttpListenerKey;
    public static HttpKeyURL torHttpListenerKey;
    public static int proxyPort;

    public CreateClientBuilder getCreateClientBuilder() {
        return new JavaEktorpCreateClientBuilder();
    }

    public Context getNewRandomCouchBaseContext() throws IOException {
        return new CreateContextInTemp();
    }

    public File getRelayWorkingDirectory() throws IOException {
        return Files.createTempDirectory("RelayTempDirectory").toFile();
    }

    public ThaliListener getStartedListener(String subFolderName) throws IOException, InterruptedException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        JavaThaliListenerContext thaliListenerContext = new CreateContextInTemp();
        File thaliListenerTorDirectory = new File(thaliListenerContext.getFilesDir(), subFolderName);
        JavaOnionProxyManager javaOnionProxyManager =
                new JavaOnionProxyManager(new JavaOnionProxyContext(thaliListenerTorDirectory));
        ThaliListener thaliListener = new ThaliListener();
        thaliListener.startServer(thaliListenerContext, 0, javaOnionProxyManager);
        thaliListener.waitTillHiddenServiceStarts();
        return thaliListener;
    }

    public void testNothing() {
        //Avoid no tests found assertion failed error.
    }
}
