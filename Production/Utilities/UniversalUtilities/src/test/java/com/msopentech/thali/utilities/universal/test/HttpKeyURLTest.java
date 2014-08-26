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

package com.msopentech.thali.utilities.universal.test;

import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.junit.Test;

import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertTrue;

public class HttpKeyURLTest {
    @Test
    public void testHttpKeyURL() throws URISyntaxException {
        KeyPair keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
        RSAPublicKey serverPublicKey = (RSAPublicKey) keyPair.getPublic();
        String host = "foo.com";
        int port = 413;
        String path = "ick";
        String query = "ark";
        String fragment = "bark";

        HttpKeyURL httpKeyURL = new HttpKeyURL(serverPublicKey, host, port, path, query, fragment);

        // We want one we do manually just to make sure everything is o.k.
        assertTrue(host.equals(httpKeyURL.getHost()));
        assertTrue(port == httpKeyURL.getPort());
        assertTrue(serverPublicKey.getModulus().equals(((RSAPublicKey)httpKeyURL.getServerPublicKey()).getModulus()));
        assertTrue(serverPublicKey.getPublicExponent().equals(((RSAPublicKey)httpKeyURL.getServerPublicKey()).getPublicExponent()));
        assertTrue(path.equals(httpKeyURL.getPath()));
        assertTrue(query.equals(httpKeyURL.getQuery()));
        assertTrue(fragment.equals(httpKeyURL.getFragment()));

        String expectedURL = HttpKeyURL.httpKeySchemeName + "://" + host + ":" + port + "/" +
                HttpKeyURL.rsaKeyType + ":" + serverPublicKey.getPublicExponent() + "." + serverPublicKey.getModulus() +
                "/" + path + "?" + query + "#" + fragment;

        assertTrue(expectedURL.equals(httpKeyURL.toString()));

        String expectedHttpsURL = "https://" + host + ":" + port + "/" + path + "?" + query + "#" + fragment;
        assertTrue(expectedHttpsURL.equals(httpKeyURL.createHttpsUrl()));

        assertTrue(httpKeyURL.equals(httpKeyURL));

        HttpKeyURL secondHttpKeyURL = new HttpKeyURL(expectedURL);

        assertTrue(httpKeyURL.equals(secondHttpKeyURL));

        HttpKeyURL thirdHttpKeyURL = new HttpKeyURL(serverPublicKey, host, port, null, null, null);

        String expectedThirdURL = HttpKeyURL.httpKeySchemeName + "://" + host + ":" + port + "/" +
                HttpKeyURL.rsaKeyType + ":" + serverPublicKey.getPublicExponent() + "." + serverPublicKey.getModulus()
                + "/";

        assertTrue(expectedThirdURL.equals(thirdHttpKeyURL.toString()));
        assertTrue(new HttpKeyURL(expectedThirdURL).equals(thirdHttpKeyURL));

        path = "ick  ?";
        query = "??????    ";
        fragment = "###???///???";
        HttpKeyURL escapedChars = new HttpKeyURL(serverPublicKey, host, port, path, query, fragment);

        expectedHttpsURL = "https://" + host + ":" + port + "/ick%20%20%3F" + "?" + "??????%20%20%20%20" + "#" + "%23%23%23???///???";
        assertTrue(expectedHttpsURL.equals(escapedChars.createHttpsUrl()));
    }
}
