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

package com.msopentech.thali.utilities.universal;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class HttpKeyURL {
    public static final String httpKeySchemeName = "httpkey";
    public static final String rsaKeyType = "rsapublickey";

    private final String host;
    private final Integer port;
    protected final PublicKey serverPublicKey;
    private final String path; // Does not contain the leading slash
    private final String query;
    private final String fragment;
    private final String stringRepresentationOfUri;

    public HttpKeyURL(String httpKeyUrlString) throws IllegalArgumentException {
        try {
            stringRepresentationOfUri = httpKeyUrlString;
            URI uri = new URI(httpKeyUrlString);
            if (httpKeySchemeName.equals(uri.getScheme()) == false) {
                throw new IllegalArgumentException("Scheme must be" + httpKeySchemeName);
            }

            // uri.getAuthority() includes the full authority which includes the port, but we just want the host
            String authority = uri.getAuthority();
            host = authority.split(":")[0];
            port = uri.getPort();

            // The first character will always be '/' so we skip that
            assert uri.getPath().charAt(0) == '/';
            String preprocessedPath = uri.getPath().substring(1);
            int locationOfPathStart = preprocessedPath.indexOf('/');
            String identityKeyString =
                    locationOfPathStart == -1 ? preprocessedPath : preprocessedPath.substring(0, locationOfPathStart);
            serverPublicKey = rsaKeyStringToRsaPublicKey(identityKeyString);
            // The path, if it exists, starts with a '/', so the second term of the or checks if the path contains
            // more than just the '/' character.
            path = locationOfPathStart == -1 || preprocessedPath.substring(locationOfPathStart).length() <= 1 ?
                    "" :
                    preprocessedPath.substring(locationOfPathStart + 1);
            query = uri.getQuery();
            fragment = uri.getFragment();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Couldn't parse string into URL", e);
        }
    }

    /**
     *
     * @param serverPublicKey
     * @param host
     * @param port
     * @param path - the value after the leading '/' in the URL, can be set to null
     * @param query can be set to null
     * @param fragment can be set to null
     * @throws IllegalArgumentException
     */
    public HttpKeyURL(PublicKey serverPublicKey, String host, int port, String path, String query, String fragment) {
        if ((serverPublicKey instanceof RSAPublicKey) == false) {
            throw new IllegalArgumentException("We only support serverPublicKey of type RSAPublicKey at the moment.");
        }

        this.host = host;
        this.port = port;
        this.serverPublicKey = serverPublicKey;
        this.path = path == null ? "" : path;
        this.query = query;
        this.fragment = fragment;

        String httpKeyPath = "/" + rsaKeyToHttpKeyString((RSAPublicKey) getServerPublicKey()) + "/" +
                (getPath() == null ? "" : getPath());

        try {
            stringRepresentationOfUri =
                    new URI(httpKeySchemeName, null, getHost(), getPort(), httpKeyPath, getQuery(), getFragment())
                            .toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Could not parse arguments into a URL", e);
        }
    }

    public String getHost() { return host; }

    public int getPort() { return port; }

    public PublicKey getServerPublicKey() { return serverPublicKey; }

    public String getPath() {
        if (path == null) {
            throw new AssertionError("Path should never be null!");
        }
        return path;
    }

    public String getQuery() { return query; }

    public String getFragment() { return fragment; }

    public String toString() { return stringRepresentationOfUri; }

    public static String rsaKeyToHttpKeyString(RSAPublicKey publicKey) {
        assert publicKey != null;
        return rsaKeyType + ":" + publicKey.getPublicExponent().toString() + "." + publicKey.getModulus().toString();
    }

    /**
     * Takes the part of the httpkey url that encodes the server's public key and turns it into a RSAPublicKey object.
     * @param rsaKeyValue
     * @return
     * @throws IllegalArgumentException
     */
    public static RSAPublicKey rsaKeyStringToRsaPublicKey(String rsaKeyValue) throws IllegalArgumentException {
        assert rsaKeyValue != null && rsaKeyValue.length() > 1 && rsaKeyValue.startsWith(rsaKeyType + ":");

        String[] splitString = rsaKeyValue.substring(rsaKeyType.length() + 1).split("\\.");
        if (splitString.length != 2) {
            throw new IllegalArgumentException("rsaKeyValue must have a single dot, instead it had: " + rsaKeyValue);
        }

        RSAPublicKeySpec rsaPublicKeySpec =
                new RSAPublicKeySpec(new BigInteger(splitString[1]), new BigInteger(splitString[0]));
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("can't process submitted key syntax", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("can't process submitted key syntax", e);
        }
    }

    public boolean equals(HttpKeyURL secondKey) {
        try {
            return secondKey != null && getHost().equals(secondKey.getHost()) && (getPort() == secondKey.getPort()) &&
                    ThaliPublicKeyComparer.RsaPublicKeyComparer(
                            (RSAPublicKey)getServerPublicKey(),
                            (RSAPublicKey)secondKey.getServerPublicKey()) &&
                    nullOrEqual(getPath(), secondKey.getPath()) && nullOrEqual(getQuery(), secondKey.getQuery()) &&
                    nullOrEqual(getFragment(), secondKey.getFragment()) && toString().equals(secondKey.toString()) &&
                    createHttpsUrl().equals(secondKey.createHttpsUrl());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Couch Clients typically need an actual HTTPS URL for the couch endpoint as well as a SSL Factory to handle
     * security (in the case of Thali). This method produces the HTTPS URL based on the HttpKeyURL.
     * @return
     */
    public String createHttpsUrl() throws URISyntaxException {
        return new URI("https", null, getHost(), getPort(), "/" + getPath(), getQuery(), getFragment()).toString();
    }

    private boolean nullOrEqual(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
