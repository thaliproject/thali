
package com.msopentech.ThaliClientCommon;

import com.msopentech.thali.utilities.universal.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

import java.io.File;
import java.lang.System;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.util.Scanner;

import fi.iki.elonen.NanoHTTPD;

public class RelayWebServer extends NanoHTTPD {

    public static final String relayHost = "localhost";
    public static final int relayPort = 58000;

    private final String tdhHost = "localhost";
    private final int tdhPort = 9898;
    private final KeyStore keyStore;
    private final CreateClientBuilder createClientBuilder;
    private final PublicKey serverPublicKey;

    public RelayWebServer(CreateClientBuilder clientBuilder, File keystoreDirectory) throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
        super(relayHost, relayPort);
        keyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(keystoreDirectory);
        createClientBuilder = clientBuilder;

        // Prep keys - refactor ThaliClientToDeviceHubUtilities.GetLocalCouchDbInstance to reuse this code
       HttpClient httpClientNoServerValidation =
                createClientBuilder.CreateApacheClient(tdhHost, tdhPort, null, keyStore, ThaliCryptoUtilities.DefaultPassPhrase);

        serverPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(
                        httpClientNoServerValidation);

        org.ektorp.http.HttpClient httpClientWithServerValidation =
                createClientBuilder.CreateEktorpClient(tdhHost, tdhPort, serverPublicKey, keyStore, ThaliCryptoUtilities.DefaultPassPhrase);

        ThaliCouchDbInstance thaliCouchDbInstance = new ThaliCouchDbInstance(httpClientWithServerValidation);

        // Set up client key in permission database
        KeyStore.PrivateKeyEntry clientPrivateKeyEntry =
                (KeyStore.PrivateKeyEntry) keyStore.getEntry(ThaliCryptoUtilities.ThaliKeyAlias,
                        new KeyStore.PasswordProtection(ThaliCryptoUtilities.DefaultPassPhrase));

        PublicKey clientPublicKey = clientPrivateKeyEntry.getCertificate().getPublicKey();

        ThaliClientToDeviceHubUtilities.configureKeyInServersKeyDatabase(clientPublicKey, thaliCouchDbInstance);

        System.out.println("RelayWebServer initialized");
    }

    @Override
    public Response serve(IHTTPSession session) {

        Map<String, String> files = new HashMap<String, String>();
        Method method = session.getMethod();
        String postBody = null;

        if (Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
                if (files.size() > 0) {
                    String fileName = files.entrySet().iterator().next().getValue();
                    if (!fileName.isEmpty()) {
                        postBody = new Scanner(new File(fileName)).useDelimiter("\\Z").next();
                    }
                }
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        if (Method.POST.equals(method)) {
            try {
                session.parseBody(files);
                if (files.size() > 0) {
                    postBody = files.entrySet().iterator().next().getValue();
                }
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        String uri = session.getUri();
        Map<String, String> parameters = session.getParms();
        Map<String, String> headers = session.getHeaders();

        System.out.println("uri: " + uri);
        System.out.println("method: " + method.toString());
        System.out.println("origin: " + headers.get("origin"));

        // Handle an OPTIONS request without relaying anything
        if (method.name().equals("OPTIONS"))
        {
            Response optionsResponse = new Response("OK");

            // Add appropriate CORS headers
            AppendCorsHeaders(optionsResponse, headers);

            optionsResponse.setStatus(Response.Status.OK);

            return optionsResponse;
        }

        // Make a new request which we will prepare for relaying to TDH
        BasicHttpEntityEnclosingRequest basicHttpRequest;
        basicHttpRequest = new BasicHttpEntityEnclosingRequest(method.name(), "https://" + tdhHost + ":" + tdhPort + uri);

        // Copy headers from incoming request to new relay request
        for(Map.Entry<String, String> entry : headers.entrySet()) {

            // Skip content-length, the library does this automatically
            if (!entry.getKey().equals("content-length")) {
                basicHttpRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }

        // Copy data from source request to new relay request
        if (postBody != null && !postBody.isEmpty()) {
            try {
                StringEntity stringEntity = new StringEntity(postBody);
                basicHttpRequest.setEntity(stringEntity);
                System.out.println("Relay data set to: " + postBody);
            } catch (UnsupportedEncodingException e) {
                // return error?!
                e.printStackTrace();
            }
        }


        // Define an http connection to send the new relay request to the TDH
        HttpHost httpHost = new HttpHost(tdhHost, tdhPort, "https");

        HttpClient httpClient = null;
        HttpClient httpClientNoServerKey = null;
        try {
            System.out.println("Prepping secure HttpClient");

            // Prep an HTTPClient to make the call
            httpClient = createClientBuilder.CreateApacheClient(tdhHost, tdhPort, serverPublicKey, keyStore,
                    ThaliCryptoUtilities.DefaultPassPhrase);

        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        // Actually make the relayed call
        HttpResponse httpResponse = null;

        if (httpClient != null) {
            try {
                System.out.println("Relaying call to TDH: " + httpHost.toURI());
                httpResponse = httpClient.execute(httpHost, basicHttpRequest);
            } catch (IOException e) {
                // return some error
                System.out.println("Relay to TDH failed! \n" + ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
                return null;
            }
        }

        // Create response and copy bits

        Response response = null;
        try {
            if (httpResponse != null) {
                response = new Response(IOUtils.toString(httpResponse.getEntity().getContent()));
            }
        } catch (IOException e) {
            System.out.println("Preparing response to client failed! \n" + ExceptionUtils.getStackTrace(e));
        }

        // Add appropriate CORS headers
        AppendCorsHeaders(response, headers);

        // TODO - set status properly
        switch (httpResponse.getStatusLine().getStatusCode()) {
            case 400:
                response.setStatus(Response.Status.BAD_REQUEST);
                break;
            case 404:
                response.setStatus(Response.Status.NOT_FOUND);
                break;
            case 500:
                response.setStatus(Response.Status.INTERNAL_ERROR);
                break;
            default:
                response.setStatus(Response.Status.OK);
                break;
        }

        for(Header responseHeader : httpResponse.getAllHeaders()) {
            if (responseHeader.getValue().equals("chunked")) {
                response.setChunkedTransfer(true);
            }
            else
            {
                response.addHeader(responseHeader.getName(), responseHeader.getValue());
            }
        }

        return response;
    }

    private void AppendCorsHeaders(Response response, Map<String,String> headers)
    {
        response.addHeader("Access-Control-Allow-Origin", headers.containsKey("origin")?headers.get("origin"):"*");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Headers", "accept, content-type, authorization, origin");
        response.addHeader("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, HEAD");
    }
}

