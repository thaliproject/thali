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

package com.msopentech.thali.CouchDBListener;

import com.couchbase.lite.auth.Authorizer;
import com.couchbase.lite.support.HttpClientFactory;
import com.msopentech.thali.utilities.ThaliHttpClientFactory;

import java.net.URL;
import java.security.*;
import java.util.Map;

/**
 * Created by yarong on 1/13/14.
 */
public class BogusThaliAuthorizer implements Authorizer {
    private final PublicKey serverPublicKey;
    private final KeyStore clientKeyStore;
    private final char[] clientPassPhrase;
    private final HttpClientFactory httpClientFactory;

    public BogusThaliAuthorizer(PublicKey serverPublicKey, KeyStore clientKeyStore, char[] clientPassPhrase) {
        assert serverPublicKey != null && clientKeyStore != null && clientPassPhrase != null;
        this.serverPublicKey = serverPublicKey;
        this.clientKeyStore = clientKeyStore;
        this.clientPassPhrase = clientPassPhrase;

        try {
            httpClientFactory = new ThaliHttpClientFactory(serverPublicKey, clientKeyStore, clientPassPhrase);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean usesCookieBasedLogin() {
        return false;
    }

    @Override
    public Map<String, String> loginParametersForSite(URL url) {
        return null;
    }

    @Override
    public String loginPathForSite(URL url) {
        return null;
    }

    @Override
    public HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }
}
