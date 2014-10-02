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

import android.test.AndroidTestCase;
import com.couchbase.lite.Context;
import com.couchbase.lite.android.AndroidContext;
import com.msopentech.thali.CouchDBListener.HttpKeyTypes;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class UtilitiesTestCase extends AndroidTestCase {
    // These are used for NetworkPerfTests, I put them here so they don't get nuked when we copy
    // over from Android to Java. Normally this variable is set to null but when given a value it's used to
    // skip creating a new Thali listener on each test pass and instead just listen to an external listener that is
    // always running. That way we don't have to start and stop listeners on each test pass. This is intended
    // for when we are doing manual testing and exploring different perf issues.
    public static HttpKeyTypes perfListenerHttpKeyTypes = null;
//        new HttpKeyTypes(
//            new HttpKeyURL("httpkey://127.0.0.1:9898/rsapublickey:65537.22209781496846834774034581672097452351546561427140992045875854504080883353634755791660883061716439288784728302691356913334938222577099338043910974691598191856193627532526962052281380405955442395151398371926346748890714401828397488082600980525845385389448667815214198541185705971795554553801294606463274482256101627054598097793794963925848521395238138232855004580560899376500329358154743925313223863793879631193380540141448841216942245648007154637498224219805193553958576113824985275275991322349527236239573948700325934819586224497586193806392668941733668501588044446947514282589105210928430675300845600820482076197249/"),
//            new HttpKeyURL("httpkey://arr2d2ofdzjlqzyx.onion:9898/rsapublickey:65537.22209781496846834774034581672097452351546561427140992045875854504080883353634755791660883061716439288784728302691356913334938222577099338043910974691598191856193627532526962052281380405955442395151398371926346748890714401828397488082600980525845385389448667815214198541185705971795554553801294606463274482256101627054598097793794963925848521395238138232855004580560899376500329358154743925313223863793879631193380540141448841216942245648007154637498224219805193553958576113824985275275991322349527236239573948700325934819586224497586193806392668941733668501588044446947514282589105210928430675300845600820482076197249/"),
//            0
//        );
    public static HttpKeyURL noTorHttpListenerKey;
    public static HttpKeyURL torHttpListenerKey;
    public static int proxyPort;

    public CreateClientBuilder getCreateClientBuilder() {
        return new AndroidEktorpCreateClientBuilder();
    }

    public Context getNewRandomCouchBaseContext() {
        return new AndroidContext(
                new AndroidContextChangeFilesDir(getContext(), RandomStringUtils.randomAlphanumeric(20)));
    }

    public ThaliListener getStartedListener(String subFolderName) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, IOException, InterruptedException {
        AndroidContextChangeFilesDir androidContext =
                new AndroidContextChangeFilesDir(getContext(), subFolderName);
        AndroidOnionProxyManager androidOnionProxyManager =
                new AndroidOnionProxyManager(androidContext, subFolderName + "OnionProxyManager");
        ThaliListener thaliListener = new ThaliListener();
        thaliListener.startServer(
                new AndroidContext(new AndroidContextChangeFilesDir(androidContext, subFolderName)),
                0,
                androidOnionProxyManager);
        thaliListener.waitTillHiddenServiceStarts();
        return thaliListener;
    }
}
