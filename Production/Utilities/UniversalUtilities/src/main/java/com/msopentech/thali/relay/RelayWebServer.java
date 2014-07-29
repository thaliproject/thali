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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msopentech.thali.CouchDBListener.HttpKeyTypes;
import com.msopentech.thali.nanohttp.NanoHTTPD;
import com.msopentech.thali.utilities.universal.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RelayWebServer extends NanoHTTPD {

    // Host and port for the relay
    public static final String relayHost = "127.0.0.1";
    public static final int relayPort = 58000;

    // Host and port for the TDH
    private final String thaliDeviceHubHost = "127.0.0.1";
    private volatile HttpKeyTypes httpKeyTypes;

    private HttpClient httpClient;
    private HttpHost httpHost;
    private Logger LOG = LoggerFactory.getLogger(RelayWebServer.class);
    private final List<String> doNotForwardHeaders = Arrays.asList("date", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade");

    public RelayWebServer(CreateClientBuilder clientBuilder, File keystoreDirectory,
                          HttpKeyTypes httpKeyTypes)
            throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        super(relayHost, relayPort);

        this.httpKeyTypes = httpKeyTypes;

        HttpKeyURL serverHttpKey = new HttpKeyURL(httpKeyTypes.getLocalMachineIPHttpKeyURL());

        // Get local couch DB instance
        ThaliCouchDbInstance thaliCouchDbInstance =
                ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance(
                        keystoreDirectory,
                        clientBuilder,
                        serverHttpKey,
                        ThaliCryptoUtilities.DefaultPassPhrase,
                        null);

        // Get the configured apache HttpClient
        httpClient = clientBuilder.extractApacheClientFromThaliCouchDbInstance(thaliCouchDbInstance);

        // CBL does not currently appear to obey the timeout at all (it is infinite for longpoll requests)
        // so until this bug is resolved or we come up with something smarter we'll also disable socket timeout
        // to give applications a chance at working properly
        // (connection timeout is still configured for a reasonable value)
        httpClient.getParams().setParameter("http.socket.timeout", new Integer(0));

        // Define an http host to address the new relay request to the TDH
        httpHost = new HttpHost(serverHttpKey.getHost(), serverHttpKey.getPort(), "https");
    }

    public void setHttpKeyTypes(HttpKeyTypes httpKeyTypes) {
        this.httpKeyTypes = httpKeyTypes;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String queryString = session.getQueryParameterString();
        String path = session.getUri();
        Map<String, String> headers = session.getHeaders();

        LOG.info("URI + Query: " + path + (queryString == null ? "" : "?" + queryString));
        LOG.info("METHOD: " + method.toString());
        LOG.info("ORIGIN: " + headers.get("origin"));

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
        if (path.equalsIgnoreCase("/_relayutility/localhttpkeys"))
        {
            ObjectMapper mapper = new ObjectMapper();
            String responseBody = null;
            try {
                responseBody = mapper.writeValueAsString(httpKeyTypes);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not generate localhttpkeys output", e);
            }
            Response httpKeyResponse = new Response(responseBody);
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
            basicHttpRequest = buildRelayRequest(method, path, queryString, headers, requestBody);
        } catch (UnsupportedEncodingException e) {
            String message = "Unable to translate body to new request.\n" + ExceptionUtils.getStackTrace(e);
            return GenerateErrorResponse(message);
        } catch (URISyntaxException e) {
            return GenerateErrorResponse("Unable to generate the URL for the local TDH.\n" +
                    ExceptionUtils.getStackTrace(e));
        }

        // Actually make the relayed call
        HttpResponse tdhResponse = null;
        InputStream tdhResponseContent = null;
        Response clientResponse = null;
        try {
            LOG.info("Relaying call to TDH: " + httpHost.toURI());
            tdhResponse = httpClient.execute(httpHost, basicHttpRequest);
            tdhResponseContent = tdhResponse.getEntity().getContent();

            // Create response and set status and body
            // default the MIME_TYPE for now and we'll set it later when we enumerate the headers
            if (tdhResponse != null) {

                // This is horrible awful evil code to deal with CouchBase note properly sending CouchDB responses
                // for some errors. We must fix this in CouchBase - https://github.com/thaliproject/thali/issues/30
                String responseBodyString = null;
                if (tdhResponse.containsHeader("content-type") &&
                        tdhResponse.getFirstHeader("content-type").getValue().equalsIgnoreCase("application/json")) {
                    if (tdhResponse.getStatusLine().getStatusCode() == 404) {
                        responseBodyString = "{\"error\":\"not_found\"}";
                    }
                    if (tdhResponse.getStatusLine().getStatusCode() == 412) {
                        responseBodyString = "{\"error\":\"missing_id\"}";
                    }
                }

                clientResponse = new Response(
                        new RelayStatus(tdhResponse.getStatusLine()),
                        NanoHTTPD.MIME_PLAINTEXT,
                        responseBodyString != null ?
                                IOUtils.toInputStream(responseBodyString) :
                                IOUtils.toBufferedInputStream(tdhResponseContent));
                // If there is a response body we want to send it chunked to enable streaming
                clientResponse.setChunkedTransfer(true);
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
                    LOG.error(e.getMessage());
                }
        }


        // Prep all headers for our final response
        AppendCorsHeaders(clientResponse, headers);
        copyHeadersToResponse(clientResponse, tdhResponse.getAllHeaders());

        return clientResponse;
    }

    // Copy response headers to relayed response
    // Enable chunked transfer where appropriate
    // Skip various hop-to-hop headers
    private void copyHeadersToResponse(Response response, Header[] headers) {
        for(Header responseHeader : headers) {
            if (!doNotForwardHeaders.contains(responseHeader.getName().toLowerCase())) {
                if (responseHeader.getName().equals("content-type")) {
                    response.setMimeType(responseHeader.getValue());
                } else {
                    response.addHeader(responseHeader.getName(), responseHeader.getValue());
                }
            }
        }
    }

    // Prepares a request which will be forwarded to the TDH by copying headers, body, etc
    private BasicHttpEntityEnclosingRequest buildRelayRequest(Method method, String path, String query,
                                                              Map<String, String> headers, byte[] body)
            throws UnsupportedEncodingException, URISyntaxException {
        HttpKeyURL baseUrl = new HttpKeyURL(httpKeyTypes.getLocalMachineIPHttpKeyURL());
        // When NanoHTTPD decodes the incoming URL in the session object it breaks the URL into three parts.
        // There is the path which is URL decoded before being handed over.
        // There is the query string which is NOT URL decoded before being handed over.
        // And there are the params which are the query parameters of the URL broken out and decoded
        // To make sure encoding is handled correctly we have chosen to use the URI class to create the
        // base and provide the URL decoded path. We then manually append the query parameter which is already
        // encoded. The reason for taking this approach is that it supports wacky query strings that don't
        // encode correctly or have other strange behavior.
        String fullHttpsUrl = new URI("https", null, baseUrl.getHost(), baseUrl.getPort(),
                path, null, null).toString() + ((query == null || query.isEmpty()) ? "" : "?" + query);
        BasicHttpEntityEnclosingRequest basicHttpRequest =
                new BasicHttpEntityEnclosingRequest(method.name(), fullHttpsUrl);

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
        LOG.error(message);
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

