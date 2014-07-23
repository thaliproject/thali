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

package com.msopentech.thali.utilities.java.test;

import com.couchbase.lite.CouchbaseLiteException;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.nanohttp.NanoHTTPD;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;

public class RelayWebServerTest {

    private static RelayWebServer server;
    private static TestTempFileManager tempFileManager;
    private static ThaliListener thaliListener;

    @BeforeClass
    public static void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException, InterruptedException {

        CreateClientBuilder cb = new JavaEktorpCreateClientBuilder();

        thaliListener = new ThaliListener();

        thaliListener.startServer(new CreateContextInTemp(), ThaliListener.DefaultThaliDeviceHubPort, null);

        server = new RelayWebServer(cb, new File(System.getProperty("user.dir")), thaliListener.getHttpKeys());
        tempFileManager = new TestTempFileManager();

        server.start();
    }

    @AfterClass
    public static void tearDown() {

        tempFileManager._clear();

        if (server != null && server.isAlive())
            server.stop();

        if (thaliListener != null)
            thaliListener.stopServer();
    }

    @Test
    public void testServerExists() {
        assertNotNull(server);
    }

    @Test
    public void OptionsRequestTest() throws IOException {

        String url = String.format("http://%s:%s/testdatabase", RelayWebServer.relayHost, RelayWebServer.relayPort);

        // With no origin provided in request, return a wildcard
        options(url).then().assertThat().header("Access-Control-Allow-Origin", "*");

        // With origin provided in request, echo origin
        given().header("Origin", "hostname").options(url)
                .then().assertThat().headers(
                    "Access-Control-Allow-Origin", "hostname",
                    "Access-Control-Allow-Credentials", "true",
                    "Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, HEAD",
                    "Access-Control-Allow-Headers", "accept, content-type, authorization, origin"
        ).and().assertThat().statusCode(200);
    }

    @Test
    public void InvalidDatabaseTest() throws IOException {

        String url = String.format("http://%s:%s/invaliddatabase", RelayWebServer.relayHost, RelayWebServer.relayPort);

        // With origin provided in request, echo origin

        given().header("Origin", "hostname").get(url)
                .then().contentType("application/json")
                .assertThat().statusCode(404);

    }

    @Test
    public void CreateWriteReadDatabaseTest() throws IOException, InterruptedException, CouchbaseLiteException {

        String url = String.format("http://%s:%s/testdatabase", RelayWebServer.relayHost, RelayWebServer.relayPort);

        // Run a delete first
        delete(url);

        // Now create
        put(url).then().assertThat().statusCode(201);

        // Write record
        given().body("{ \"one\":1, \"two\":2 }").post(url).then()
                .assertThat().statusCode(201);

        // Read records
        String getUrl = url + "/_all_docs?include_docs=true&_nonce=CffIZghmuLjQXFKL";

        // Keys from our record should be in the response if 'include_docs' was obeyed
        get(getUrl).then().assertThat().statusCode(200).body("rows[0].doc.two", equalTo(2));

    }

    @Test
    public void RelayUtilityHttpKeyTest() throws UnknownHostException, InterruptedException {

        String url = String.format("http://%s:%s/_relayutility/localhttpkeys", RelayWebServer.relayHost,
                RelayWebServer.relayPort);

        // We should validate the JSON with a schema but, um... next time
        get(url).then()
                .body("localMachineIPHttpKeyURL",
                        equalTo(thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL()))
                .assertThat().statusCode(200);
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
