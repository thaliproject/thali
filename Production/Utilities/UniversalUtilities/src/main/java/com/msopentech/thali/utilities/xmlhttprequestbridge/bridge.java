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

package com.msopentech.thali.utilities.xmlhttprequestbridge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.msopentech.thali.utilities.universal.*;
import com.msopentech.thali.utilities.webviewbridge.BridgeCallBack;
import com.msopentech.thali.utilities.webviewbridge.BridgeHandler;
import com.msopentech.thali.utilities.webviewbridge.BridgeManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.Locale;
import java.util.Map;

public class Bridge extends BridgeHandler {

    // These two values are for bogus HTTP methods that the client can send the proxy in order to
    // get it to handle either provisioning the local client to the destination
    public static final String ProvisionClientToHub = "ThaliProvisionLocalClientToHub";

    // or (see above) to provision the local hub to a remote hub
    public static final String ProvisionLocalHubToRemoteHub = "ThaliProvisionRemote";

    public static final String JavascriptName = "ThaliXHR";

    private final KeyStore keyStore;

    private final CreateClientBuilder createClientBuilder;

    private final ObjectMapper mapper = new ObjectMapper();

    public Bridge(File workingDirectory, CreateClientBuilder createClientBuilder) {
        super(JavascriptName);
        keyStore = ThaliCryptoUtilities.getThaliKeyStoreByAnyMeansNecessary(workingDirectory);
        this.createClientBuilder = createClientBuilder;
    }

    @Override
    public void call(final String jsonString, final BridgeCallBack bridgeCallBack) {
        new Thread(new Runnable() {
            @Override
        public void run() {
                String response = ProcessRequest(jsonString);
                bridgeCallBack.successHandler(response);
            }
        }).start();
    }

    private String ProcessHostError(String errorMessage, XmlHttpRequest xmlHttpRequest) {
        XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
        xmlHttpResponse.transactionId = xmlHttpRequest != null ? xmlHttpRequest.transactionId : null;
        xmlHttpResponse.status = 502;
        xmlHttpResponse.responseText = errorMessage;
        xmlHttpResponse.headers.put("content-type","text/plain");
        try {
            return mapper.writeValueAsString(xmlHttpResponse);
        } catch (JsonProcessingException e) {
            return "This really shouldn't have happened.";
        }
    }

    private String ProcessRequest(String jsonString) {
        try {
            XmlHttpRequest xmlHttpRequest = mapper.readValue(jsonString, XmlHttpRequest.class);

            if (xmlHttpRequest.type.equals(XmlHttpRequest.typeValue) == false) {
                return ProcessHostError("Received request whose type as " + xmlHttpRequest.type, xmlHttpRequest);
            }

            if (xmlHttpRequest.method.equals(ProvisionClientToHub)) {
                return ExecuteProvisionLocalClientToLocalHub(xmlHttpRequest);
            } else if (xmlHttpRequest.method.equals(ProvisionLocalHubToRemoteHub)) {
                return ExecuteProvisionLocalHubToRemoteHub(xmlHttpRequest);
            } else {
                return ProxyRequest(xmlHttpRequest);
            }
        } catch (Throwable e) {
            return ProcessHostError("Ooops! " + e.getMessage(), null);
        }
    }

    private String ExecuteProvisionLocalClientToLocalHub(XmlHttpRequest xmlHttpRequest) throws URISyntaxException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
            IOException {
        BogusUrlHandler localServerUriNoKey = new BogusUrlHandler(xmlHttpRequest.url);

        HttpClient httpClientNoServerKey =
                createClientBuilder.CreateApacheClient(
                        localServerUriNoKey.getHost(),
                        localServerUriNoKey.getPort(),
                        null,
                        keyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);
        PublicKey serverPublicKey = ThaliClientToDeviceHubUtilities.getServersRootPublicKey(httpClientNoServerKey);

        PublicKey appKey = ThaliCryptoUtilities.RetrieveAppKeyFromKeyStore(keyStore);

        org.ektorp.http.HttpClient httpClientWithServerKey =
                createClientBuilder.CreateEktorpClient(
                        localServerUriNoKey.getHost(),
                        localServerUriNoKey.getPort(),
                        serverPublicKey,
                        keyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);
        ThaliCouchDbInstance serverCouchDbInstance = new ThaliCouchDbInstance(httpClientWithServerKey);

        ThaliClientToDeviceHubUtilities.configureKeyInServersKeyDatabase(appKey, serverCouchDbInstance);

        HttpKeyURL fullyQualifiedServerUri =
                new HttpKeyURL(serverPublicKey, localServerUriNoKey.getHost(), localServerUriNoKey.getPort(),
                                null, null, null);
        XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
        xmlHttpResponse.status = 200;
        xmlHttpResponse.transactionId = xmlHttpRequest.transactionId;
        xmlHttpResponse.responseText = fullyQualifiedServerUri.toString();
        return mapper.writeValueAsString(xmlHttpResponse);
    }

    private String ExecuteProvisionLocalHubToRemoteHub(XmlHttpRequest xmlHttpRequest) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, URISyntaxException {
        BogusUrlHandler remoteHubUriNoKey = new BogusUrlHandler(xmlHttpRequest.url);
        HttpClient httpClientRemoteHubNoKey =
                createClientBuilder.CreateApacheClient(
                        remoteHubUriNoKey.getHost(),
                        remoteHubUriNoKey.getPort(),
                        null,
                        keyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);
        PublicKey remoteHubPublicKey =
                ThaliClientToDeviceHubUtilities.getServersRootPublicKey(httpClientRemoteHubNoKey);

        HttpKeyURL localHubUri = new HttpKeyURL(xmlHttpRequest.requestText);

        org.ektorp.http.HttpClient remoteHubWithServerKey =
                createClientBuilder.CreateEktorpClient(
                        remoteHubUriNoKey.getHost(),
                        remoteHubUriNoKey.getPort(),
                        remoteHubPublicKey,
                        keyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase);
        ThaliCouchDbInstance remoteHubCouchDbInstance = new ThaliCouchDbInstance(remoteHubWithServerKey);

        ThaliClientToDeviceHubUtilities
                .configureKeyInServersKeyDatabase(localHubUri.getServerPublicKey(), remoteHubCouchDbInstance);

        HttpKeyURL remoteHubUri =
                new HttpKeyURL(remoteHubPublicKey, remoteHubUriNoKey.getHost(), remoteHubUriNoKey.getPort(),
                        null, null, null);
        XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();
        xmlHttpResponse.status = 200;
        xmlHttpResponse.transactionId = xmlHttpRequest.transactionId;
        xmlHttpResponse.responseText = remoteHubUri.toString();
        return mapper.writeValueAsString(xmlHttpResponse);
    }

    private String ProxyRequest(XmlHttpRequest xmlHttpRequest) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException,
            IOException {
        HttpKeyURL remoteServerThaliUrl = new HttpKeyURL(xmlHttpRequest.url);

        BasicHttpEntityEnclosingRequest basicHttpRequest =
                new BasicHttpEntityEnclosingRequest(xmlHttpRequest.method, remoteServerThaliUrl.createHttpsUrl());

        for(Map.Entry<String, String> entry : xmlHttpRequest.headers.entrySet()) {
            // TODO: Test with multiple incoming headers with the same name
            basicHttpRequest.setHeader(entry.getKey(), entry.getValue());
        }

        if (xmlHttpRequest.requestText != null && xmlHttpRequest.requestText.isEmpty() == false) {
            StringEntity stringEntity = new StringEntity(xmlHttpRequest.requestText);
            basicHttpRequest.setEntity(stringEntity);
        }

        HttpHost httpHost = new HttpHost(remoteServerThaliUrl.getHost(), remoteServerThaliUrl.getPort(), "https");

        HttpClient httpClient = createClientBuilder.CreateApacheClient(remoteServerThaliUrl, keyStore,
                ThaliCryptoUtilities.DefaultPassPhrase);

        HttpResponse httpResponse = httpClient.execute(httpHost, basicHttpRequest);

        XmlHttpResponse xmlHttpResponse = new XmlHttpResponse();

        xmlHttpResponse.transactionId = xmlHttpRequest.transactionId;

        xmlHttpResponse.status = httpResponse.getStatusLine().getStatusCode();

        for(Header header : httpResponse.getAllHeaders()) {
            String lowerCaseHeaderValue = header.getName().toLowerCase(Locale.ROOT);
            xmlHttpResponse.headers.put(lowerCaseHeaderValue,
                    xmlHttpResponse.headers.containsValue(lowerCaseHeaderValue) ?
                        xmlHttpResponse.headers.get(lowerCaseHeaderValue) + " , " + header.getValue() :
                        header.getValue());
        }

        if (httpResponse.getEntity() != null) {
            // TODO: Obvious attack, just send us a huge response and we go kaboom! Or hold up the connection. Etc.
            // I'm sure there are other fun things too. Lots of hardening needed here.
            xmlHttpResponse.responseText =
                    IOUtils.toString(
                            httpResponse.getEntity().getContent());
        }

        return mapper.writeValueAsString(xmlHttpResponse);
    }
}
