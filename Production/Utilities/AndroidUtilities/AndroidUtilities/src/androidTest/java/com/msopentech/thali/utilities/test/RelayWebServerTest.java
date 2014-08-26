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

package com.msopentech.thali.utilities.test;

import com.couchbase.lite.CouchbaseLiteException;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.local.utilities.UtilitiesTestCase;
import com.msopentech.thali.nanohttp.NanoHTTPD;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.toronionproxy.OsData;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.HashMap;

// Due to https://github.com/thaliproject/thali/issues/67 we can't restart the listeners in Java. So the code is
// set up to start the listeners and related servers exactly once in Java and never again, we re-use the same
// instances across all the tests. In Android however we can restart so we do.
public class RelayWebServerTest extends CommonListenerTestBasis {

    public void testServerExists() {
        assertNotNull(server);
    }

    public void testOptionsRequest() throws IOException {

        String url = String.format("http://%s:%s/testdatabase", relayHost, relayPort);

        // With no origin provided in request, return a wildcard
        //options(url).then().assertThat().header("Access-Control-Allow-Origin", "*");
        RestTestMethods.testOptions(url, null, 200, new HashMap<String, String>() {{ put("Access-Control-Allow-Origin", "*");}});

        // With origin provided in request, echo origin
//        given().header("Origin", "hostname").options(url)
//                .then().assertThat().headers(
//                "Access-Control-Allow-Origin", "hostname",
//                "Access-Control-Allow-Credentials", "true",
//                "Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, HEAD",
//                "Access-Control-Allow-Headers", "accept, content-type, authorization, origin"
//        ).and().assertThat().statusCode(200);
        RestTestMethods.testOptions(url, new HashMap<String, String>() {{ put("Origin", "hostname"); }}, 200,
                new HashMap<String, String>() {{ put("Access-Control-Allow-Origin", "hostname");
                                         put("Access-Control-Allow-Credentials", "true");
                                         put("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, HEAD");
                                         put("Access-Control-Allow-Headers", "accept, content-type, authorization, origin");}});

    }

    public void testInvalidDatabase() throws IOException {

        String url = String.format("http://%s:%s/invaliddatabase", relayHost, relayPort);

        // With origin provided in request, echo origin

//        given().header("Origin", "hostname").get(url)
//                .then().contentType("application/json")
//                .assertThat().statusCode(404);
         RestTestMethods.testGet(url, new HashMap<String, String>() {{ put("Origin", "hostname"); }}, 404,
                 new HashMap<String, String>() {{ put("Content-Type", "application/json"); }});
    }

    public void testCreateWriteReadDatabase() throws IOException, InterruptedException, CouchbaseLiteException {
        String url = String.format("http://%s:%s/testdatabase", relayHost, relayPort);

        // Run a delete first
        //delete(url);
        RestTestMethods.testDelete(url, null, 200, null);

        // Now create
        //put(url).then().assertThat().statusCode(201);
        RestTestMethods.testPut(url, null, 201, null);

        // Write record
//        given().body("{ \"one\":1, \"two\":2 }").post(url).then()
//                .assertThat().statusCode(201);
        RestTestMethods.testPost(url, null, "{ \"one\":1, \"two\":2 }", 201, null);

        // Read records
        String getUrl = url + "/_all_docs?include_docs=true";

        // Keys from our record should be in the response if 'include_docs' was obeyed
        //get(getUrl).then().assertThat().statusCode(200).body("rows[0].doc.two", equalTo(2));
        RestTestMethods.testGet(getUrl, null, 200, null, new HashMap<String, Object>(){{ put("$.rows[0].doc.two", 2);}});

        // This tests if we are double encoding. If we do then the %5F will get turned into
        // %255F and will fail on CouchBase. If we aren't double encoded then it will get properly
        // decoded and life will be good. We hope.
        getUrl = url + "/_all%5Fdocs?include_docs=true";
        RestTestMethods.testGet(getUrl, null, 200, null, new HashMap<String, Object>(){{ put("$.rows[0].doc.two", 2);}});
    }

    public void testRelayUtilityHttpKey() throws IOException, InterruptedException {
        String url = String.format("http://%s:%s/_relayutility/localhttpkeys", relayHost,
                relayPort);

        // We should validate the JSON with a schema but, um... next time
//        get(url).then()
//                .body("localMachineIPHttpKeyURL",
//                        equalTo(thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL()))
//                .assertThat().statusCode(200);
        RestTestMethods.testGet(url, null, 200, null,
                new HashMap<String, Object>(){{
                    put("$.localMachineIPHttpKeyURL", thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL());
                    put("$.onionHttpKeyURL", thaliListener.getHttpKeys().getOnionHttpKeyURL());}});
    }

    public void testTranslateOnionAddress() throws InterruptedException, IOException {
        // Test against a server that is configured to trust us
        onionAddressTest(thaliListener);
        // Now test against a server that is not configured to trust us
        onionAddressTest(noSecurityThaliListener);
    }

    private void onionAddressTest(ThaliListener thaliListener) throws InterruptedException, IOException {
        // Tests a non onion address
        resolveAddressToHttpKeyUrl(thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL());

        // Onion address
        resolveAddressToHttpKeyUrl(thaliListener.getHttpKeys().getOnionHttpKeyURL());
    }

    private void resolveAddressToHttpKeyUrl(final String httpKeyUrl) throws IOException {
        final String address = httpKeyUrl.split("/")[2];
        String url = String.format("http://%s:%s/_relayutility/translateonion?%s", relayHost,
                relayPort, address);
        RestTestMethods.testGet(url, null, 200, null,
                new HashMap<String, Object>() {{
                    put("$.httpKeyUrl", httpKeyUrl);
                }});
    }

    /*@Test
    public void SeleniumTest() {
        WebDriver driver = new ChromeDriver();
    }*/

    public static class TestTempFileManager extends NanoHTTPD.DefaultTempFileManager {
        public void _clear() {
            super.clear();
        }

        @Override
        public void clear() {
            // ignore
        }
    }
}
