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

import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: None of this code handles URL escaping at all, so if any values that need to be escaped are submitted then
// 'bad stuff' is going to happen.
public class ThaliGroupUrl {
    public static final String groupUrlPrefix = "thaligroup";
    private static final int ownerGroup = 1;
    private static final int groupNameGroup = 2;
    private static final int pathGroup = 3;
    // TODO: This check is insecure as all get out!
    // Note: Android (of course) uses a different parser for regex than Java. Java supports named groups
    // which makes this regex much more readable. Android does not. Thanks Android!
    private static final Pattern thaliGroupUriPattern =
            Pattern.compile("^" + groupUrlPrefix + ":/([^/]*)/([^/]*)/httpkeysimple($|/.*)");

    private final RSAPublicKey serverKey;
    private final String groupName;
    private final String path;
    private final String url;

    public ThaliGroupUrl(String url, PublicKey expectedServerPublicKey) {
        Matcher matcher = thaliGroupUriPattern.matcher(url);
        if (matcher.matches() == false) {
            throw new IllegalArgumentException("Malformed thaligroup URL");
        }

        serverKey = HttpKeyURL.rsaKeyStringToRsaPublicKey(matcher.group(ownerGroup));
        if (ThaliPublicKeyComparer.RsaPublicKeyComparer((RSAPublicKey)expectedServerPublicKey, serverKey) == false) {
            throw new IllegalArgumentException("thaligroup URL's owner value doesn't match expected value");
        }

        groupName = matcher.group(groupNameGroup);

        String rawPath = matcher.group(pathGroup);

        path = rawPath == null ? "" : (rawPath.startsWith("/") ? rawPath.substring(1) : rawPath);

        this.url = url;
    }

    /**
     *
     * @param serverPublicKey
     * @param groupName
     * @param path Unless you want to stick in an extra '/' the path MUST NOT begin with '/'
     */
    public ThaliGroupUrl(PublicKey serverPublicKey, String groupName, String path) {
        // TODO: This code is extremely fragile, it needs serious hardening.
        this(groupUrlPrefix + ":/" + HttpKeyURL.rsaKeyToHttpKeyString((RSAPublicKey) serverPublicKey) +
        "/" + groupName + "/httpkeysimple/" + (path == null ? "" : path),
                serverPublicKey);
    }

    public RSAPublicKey getServerKey() {
        return serverKey;
    }

    public String getGroupName() {
        return groupName;
    }

    /**
     *
     * @return Returned value will be empty or start with a /
     */
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return url;
    }

    public static boolean isThaliGroupUrl(String url, PublicKey expectedServerPublicKey) {
        try {
            ThaliGroupUrl thaliGroupUrl = new ThaliGroupUrl(url, expectedServerPublicKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
