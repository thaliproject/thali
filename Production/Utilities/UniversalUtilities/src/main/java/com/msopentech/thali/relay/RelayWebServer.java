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

package com.msopentech.thali.relay;

import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.nanohttp.NanoHTTPD;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliClientToDeviceHubUtilities;
import com.msopentech.thali.utilities.universal.ThaliCouchDbInstance;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.*;

public class RelayWebServer extends NanoHTTPD {

    // Host and port for the relay
    public static final String relayHost = "127.0.0.1";
    public static final int relayPort = 58000;

    // Host and port for the TDH
    private final String thaliDeviceHubHost = "127.0.0.1";
    private int thaliDeviceHubPort;

    private HttpClient httpClient;
    private HttpHost httpHost;
    private Logger Log = LoggerFactory.getLogger(RelayWebServer.class);;
    private final List<String> doNotForwardHeaders = Arrays.asList("date", "connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade");

    public RelayWebServer(CreateClientBuilder clientBuilder, File keystoreDirectory) throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
         this(clientBuilder, keystoreDirectory, ThaliListener.DefaultThaliDeviceHubPort);
    }

    public RelayWebServer(CreateClientBuilder clientBuilder, File keystoreDirectory, int thaliDeviceHubPort) throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
        super(relayHost, relayPort);

        this.thaliDeviceHubPort = thaliDeviceHubPort;

        // Get local couch DB instance
        ThaliCouchDbInstance thaliCouchDbInstance = ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance(keystoreDirectory, clientBuilder, thaliDeviceHubHost, thaliDeviceHubPort, ThaliCryptoUtilities.DefaultPassPhrase, null);

        // Get the configured apache HttpClient
        httpClient = clientBuilder.extractApacheClientFromThaliCouchDbInstance(thaliCouchDbInstance);

        // CBL does not currently appear to obey the timeout at all (it is infinite for longpoll requests)
        // so until this bug is resolved or we come up with something smarter we'll also disable socket timeout
        // to give applications a chance at working properly
        // (connection timeout is still configured for a reasonable value)
        httpClient.getParams().setParameter("http.socket.timeout", new Integer(0));

        // Define an http host to address the new relay request to the TDH
        httpHost = new HttpHost(thaliDeviceHubHost, thaliDeviceHubPort, "https");

        // Set max connections for this route (Default is 2)
        //ThreadSafeClientConnManager cm = (ThreadSafeClientConnManager)httpClient.getConnectionManager();
        //cm.setMaxTotal(20);
        //cm.setMaxForRoute(new HttpRoute(httpHost, null, false), 20);
    }

    @Override
    public Response serve(IHTTPSession session) {

        Method method = session.getMethod();
        String queryString = session.getQueryParameterString();
        String uri = session.getUri();
        Map<String, String> headers = session.getHeaders();

        // If there is a query string, append it to URI
        if (queryString != null && !queryString.isEmpty()) {
            uri = uri.concat("?" + queryString);
        }

        Log.info("URI: " + uri);
        Log.info("METHOD: " + method.toString());
        Log.info("ORIGIN: " + headers.get("origin"));

        // Handle an OPTIONS request without relaying anything for now
        // TODO: Relay OPTIONS properly, but manage the CORS aspects so
        // we don't introduce dependencies on couch CORS configuration
        if (method.name().equalsIgnoreCase("OPTIONS")) {
            Response optionsResponse = new Response("OK");
            AppendCorsHeaders(optionsResponse, headers);
            optionsResponse.setStatus(Response.Status.OK);
            return optionsResponse;
        }

        // Handle request for local HTTP Key URL
        // TODO: Temporary fake values, need to build hook to handle magic URLs and generate proper HTTPKey
        if (uri.equalsIgnoreCase("/relayutility/localhttpkey"))
        {
            Response httpKeyResponse = new Response("{'httpkey':'427172846852162286227732782294920302420713842275481985193987416465727827594332841946536424113226184082100799979846263322298149064624948841718223595871002487468854371825902763487876571562308540746622769324666936426716328322661006174187432292824234387672928185522171868214215265962193686663919735268176833103576891577777488691009982184273100527780539366654312983859430294532482669543564769536996694547788895124139427128553090154213261621141595978827486497762585373289857851966036673745423578288467224472884115824176989596378133819214820984895929617664984282716722195955274042499434493624'}");
            AppendCorsHeaders(httpKeyResponse, headers);
            httpKeyResponse.setMimeType("application/json");
            httpKeyResponse.setStatus(Response.Status.OK);
            return httpKeyResponse;
        }

        // Get the body of the request if appropriate
        byte[] requestBody = new byte[0];
        if (method.equals(Method.PUT) || method.equals(Method.POST)) {
            try {
                ByteBuffer bodyBuffer = ((HTTPSession) session).getBody();
                if (bodyBuffer != null) {
                    requestBody = new byte[bodyBuffer.remaining()];
                    bodyBuffer.get(requestBody);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return GenerateErrorResponse(e.getMessage());
            }
        }


        // Make a new request which we will prepare for relaying to TDH
        BasicHttpEntityEnclosingRequest basicHttpRequest = null;
        try {
            basicHttpRequest = buildRelayRequest(method, uri, headers, requestBody);
        } catch (UnsupportedEncodingException e) {
            String message = "Unable to translate body to new request.\n" + ExceptionUtils.getStackTrace(e);
            return GenerateErrorResponse(message);
        }

        // Actually make the relayed call
        HttpResponse tdhResponse = null;
        InputStream tdhResponseContent = null;
        Response clientResponse = null;
        try {
            Log.info("Relaying call to TDH: " + httpHost.toURI());
            tdhResponse = httpClient.execute(httpHost, basicHttpRequest);
            tdhResponseContent = tdhResponse.getEntity().getContent();

                // Create response and set status and body
                // default the MIME_TYPE for now and we'll set it later when we enumerate the headers
                if (tdhResponse != null) {
                    clientResponse = new Response(
                            new RelayStatus(tdhResponse.getStatusLine()),
                            NanoHTTPD.MIME_PLAINTEXT,
                            IOUtils.toString(tdhResponseContent));
                }

        } catch (IOException e) {
            String message = "Reading response failed!\n" + ExceptionUtils.getStackTrace(e);
            return GenerateErrorResponse(message);
        }
        finally {
            // Make sure the read stream is closed so we don't exhaust our pool
            if (tdhResponseContent != null)
                try {
                    tdhResponseContent.close();
                } catch (IOException e) {
                    Log.error(e.getMessage());
                }
        }


        // Prep all headers for our final response
        AppendCorsHeaders(clientResponse, headers);
        CopyHeadersToResponse(clientResponse, tdhResponse.getAllHeaders());

        return clientResponse;
    }

    // Copy response headers to relayed response
    // Enable chunked transfer where appropriate
    // Skip various hop-to-hop headers
    private void CopyHeadersToResponse(Response response, Header[] headers) {

        for(Header responseHeader : headers) {
            if (!doNotForwardHeaders.contains(responseHeader.getName().toLowerCase())) {
                if (responseHeader.getValue().equals("chunked")) {
                    response.setChunkedTransfer(true);
                }
                else if (responseHeader.getName().equals("content-type")) {
                    response.setMimeType(responseHeader.getValue());
                } else {
                    response.addHeader(responseHeader.getName(), responseHeader.getValue());
                }
            }
        }
    }

    // Prepares a request which will be forwarded to the TDH by copying headers, body, etc
    private BasicHttpEntityEnclosingRequest buildRelayRequest(Method method, String uri, Map<String, String> headers, byte[] body) throws UnsupportedEncodingException {
        BasicHttpEntityEnclosingRequest basicHttpRequest =
                new BasicHttpEntityEnclosingRequest(method.name(), "https://" + thaliDeviceHubHost + ":" + thaliDeviceHubPort + uri);

        // Copy headers from incoming request to new relay request
        for(Map.Entry<String, String> entry : headers.entrySet()) {

            // Skip content-length, the library does this automatically
            if (!entry.getKey().equals("content-length")) {
                basicHttpRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }

        // Copy data from source request to new relay request
        if (body != null && body.length > 0) {
            ByteArrayEntity bodyEntity = new ByteArrayEntity(body);
            basicHttpRequest.setEntity(bodyEntity);
        }

        return basicHttpRequest;
    }

    // Oversimplified error response for failures in the relay
    private Response GenerateErrorResponse(String message)
    {
        Log.error(message);
        return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: " + message);
    }

    // Append requested headers or default if none found
    // Note all incoming headers are forced lowercase by NanoHTTPD.
    private void AppendCorsHeaders(Response response, Map<String,String> headers)
    {
        response.addHeader("Access-Control-Allow-Origin", headers.containsKey("origin")?headers.get("origin"):"*");

        response.addHeader("Access-Control-Allow-Credentials", "true");

        response.addHeader("Access-Control-Allow-Headers",
                headers.containsKey("access-control-request-headers")?headers.get("access-control-request-headers"):"accept, content-type, authorization, origin");

        response.addHeader("Access-Control-Allow-Methods",
                headers.containsKey("access-control-request-method")?headers.get("access-control-request-method"):"GET, PUT, POST, DELETE, HEAD");
    }
}

