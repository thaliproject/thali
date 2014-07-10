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

import com.msopentech.thali.utilities.universal.test.ThaliTestEktorpClient;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.ektorp.http.StdHttpClient;
import org.junit.*;
import com.couchbase.lite.JavaContext;


import java.io.*;
import java.net.Proxy;
import java.nio.file.Files;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RelayWebServerTest {

    private static RelayWebServer server;
    private static TestTempFileManager tempFileManager;
    private static ThaliListener thaliListener;

    @BeforeClass
    public static void setUp() throws NoSuchAlgorithmException, IOException, UnrecoverableEntryException, KeyStoreException, KeyManagementException, InterruptedException {

        CreateClientBuilder cb = new JavaEktorpCreateClientBuilder();

        thaliListener = new ThaliListener();

        thaliListener.startServer(new CreateContextInTemp(), ThaliListener.DefaultThaliDeviceHubPort, null);

        int tdhPort = thaliListener.getSocketStatus().getPort();

        server = new RelayWebServer(cb, new File(System.getProperty("user.dir")), tdhPort);
        tempFileManager = new TestTempFileManager();
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

        ByteArrayOutputStream outputStream = invokeServer("OPTIONS " + url + " HTTP/1.1");

        String[] expected = {
                "HTTP/1.1 200 OK",
                "Content-Type: text/html",
                "Date: .*",
                "Access-Control-Allow-Origin: \\*",
                "Access-Control-Allow-Credentials: true",
                "Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD",
                "Access-Control-Allow-Headers: accept, content-type, authorization, origin",
                "Connection: keep-alive",
                "Content-Length: 2",
                "",
                "OK"
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void InvalidDatabaseTest() throws IOException {

        String url = String.format("http://%s:%s/invaliddatabase", RelayWebServer.relayHost, RelayWebServer.relayPort);

        ByteArrayOutputStream outputStream = invokeServer("GET " + url + " HTTP/1.1");

        String[] expected = {
                "HTTP/1.1 400 Bad Request",
                "Content-Type: application/json",
                "Date: .*",
                "mime-version: 1.0",
                "server: .*",
                "keep-alive: timeout=1, max=100",
                "Access-Control-Allow-Origin: \\*",
                "Access-Control-Allow-Credentials: true",
                "Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD",
                "connection: keep-alive",
                "Access-Control-Allow-Headers: accept, content-type, authorization, origin",
                "Transfer-Encoding: chunked",
                "",
                ".*",
                "\\{\"error\":\"Invalid database\",\"status\":400\\}",
                "0",
                ""
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void CreateWriteReadDatabaseTest() throws IOException, InterruptedException, CouchbaseLiteException {

        // Run a delete first
        invokeServer("DELETE " + "/testdatabase" + " HTTP/1.1");

        // Now create
        ByteArrayOutputStream outputStream = invokeServer("PUT /testdatabase HTTP/1.1");

        String[] expected = {
                "HTTP/1.1 201 Created",
                "Content-Type: application/json",
                "Date: .*",
                "mime-version.*",
                "server:.*",
                "keep-alive:.*",
                "Access-Control-Allow-Origin: \\*",
                "Access-Control-Allow-Credentials: true",
                "Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD",
                "location: /testdatabase",
                "connection: keep-alive",
                "Access-Control-Allow-Headers: accept, content-type, authorization, origin",
                "Transfer-Encoding: chunked",
                "",
                "b",
                "\\{\"ok\":true\\}",
                "0",
                ""
        };

        assertResponse(outputStream, expected);

        // Write record
        String postDocument = "POST /testdatabase HTTP/1.1\r\n" +
                              "Content-Length: 20\r\n" +
                              "Host: 127.0.0.1:58000\r\n\r\n" +
                              "{ \"one\":1, \"two\":2 }";

        outputStream = invokeServer(postDocument);

        String[] expectedInsert = {
                "HTTP/1.1 201 Created",
                "Content-Type: application/json",
                "Date: .*",
                "mime-version.*",
                "server:.*",
                "keep-alive:.*",
                "Access-Control-Allow-Origin: \\*",
                "Access-Control-Allow-Credentials: true",
                "Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD",
                "location: /testdatabase",
                "etag: .*",
                "connection: keep-alive",
                "Access-Control-Allow-Headers: accept, content-type, authorization, origin",
                "Transfer-Encoding: chunked",
                "",
                ".*",
                "\\{\"rev\":.*",
                "0",
                ""
        };

        assertResponse(outputStream, expectedInsert);

        // Read records
        String getDocument = "GET /testdatabase/_all_docs?include_docs=true HTTP/1.1";

        outputStream = invokeServer(getDocument);

        String[] expectedRead = {
                "HTTP/1.1 200 OK",
                "Content-Type: application/json",
                "Date: .*",
                "mime-version.*",
                "server:.*",
                "keep-alive:.*",
                "Access-Control-Allow-Origin: \\*",
                "Access-Control-Allow-Credentials: true",
                "Access-Control-Allow-Methods: GET, PUT, POST, DELETE, HEAD",
                "connection: keep-alive",
                "Access-Control-Allow-Headers: accept, content-type, authorization, origin",
                "Transfer-Encoding: chunked",
                "",
                ".*",
                "\\{\"offset\":0,\"total_rows\":1,\"rows\":\\[\\{\"doc\":\\{\"two\":2,\"one\":1.*",
                "0",
                ""
        };

        assertResponse(outputStream, expectedRead);
    }


    protected ByteArrayOutputStream invokeServer(String request) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NanoHTTPD.HTTPSession session = server.createSession(tempFileManager, inputStream, outputStream);

        try {
            session.execute();
        } catch (IOException e) {
            fail(""+e);
            e.printStackTrace();
        }
        return outputStream;
    }

    protected List<String> getOutputLines(ByteArrayOutputStream outputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        return readLinesFromFile(reader);
    }

    protected List<String> readLinesFromFile(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    protected void assertResponse(ByteArrayOutputStream outputStream, String[] expected) throws IOException {
        List<String> lines = getOutputLines(outputStream);
        assertLinesOfText(expected, lines);
    }

    protected void assertLinesOfText(String[] expected, List<String> lines) {
        for (int i = 0; i < expected.length; i++) {
            String line = lines.get(i);
            assertTrue("Output line " + i + " doesn't match expectation.\n" +
                    "  Output: " + line + "\n" +
                    "Expected: " + expected[i], line.matches(expected[i]));
        }
    }

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
