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
import com.msopentech.thali.utilities.universal.ThaliGroupUrl;
import org.junit.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ThaliGroupUrlTest {
    @Test
    public void testGroupUrl() {
        KeyPair keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
        RSAPublicKey serverPublicKey = (RSAPublicKey) keyPair.getPublic();
        String groupName = "all";
        String path = "test/bar";

        ThaliGroupUrl thaliGroupUrl = new ThaliGroupUrl(serverPublicKey, groupName, path);

        assertEquals(HttpKeyURL.rsaKeyToHttpKeyString(thaliGroupUrl.getServerKey()),
                HttpKeyURL.rsaKeyToHttpKeyString(serverPublicKey));
        assertEquals(thaliGroupUrl.getGroupName(), groupName);
        assertEquals(thaliGroupUrl.getPath(), path);

        String handCraftGroupUrl = ThaliGroupUrl.groupUrlPrefix + ":/" +
                HttpKeyURL.rsaKeyToHttpKeyString(serverPublicKey) + "/" +
                groupName + "/httpkeysimple/" + path;
        assertEquals(thaliGroupUrl.toString(), handCraftGroupUrl);

        ThaliGroupUrl thaliGroupUrlReverse = new ThaliGroupUrl(thaliGroupUrl.toString(), serverPublicKey);

        assertEquals(HttpKeyURL.rsaKeyToHttpKeyString(thaliGroupUrlReverse.getServerKey()),
                HttpKeyURL.rsaKeyToHttpKeyString(serverPublicKey));
        assertEquals(thaliGroupUrlReverse.getGroupName(), groupName);
        assertEquals(thaliGroupUrlReverse.getPath(), path);
        assertEquals(thaliGroupUrlReverse.toString(), thaliGroupUrl.toString());

        try {
            KeyPair secondKeyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            new ThaliGroupUrl(thaliGroupUrl.toString(), secondKeyPair.getPublic());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
