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

import android.util.Log;
import com.couchbase.lite.*;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.router.RequestAuthorization;
import com.couchbase.lite.router.Router;
import com.couchbase.lite.router.URLConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msopentech.thali.utilities.universal.ThaliPublicKeyComparer;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.security.PublicKey;
import java.util.EnumSet;
import java.util.List;

/**
 * This is just for initial boot strapping until we put in place a more formal way to exchange keys. The logic
 * here is supposed to be that anyone in the KeyDatabase can do anything they want to any other database
 * without restriction. There is no protection on the KeyDatabase so anyone can add themselves. Obviously
 * this is insecure as hell but that isn't the point.
 *
 * The code assumes that the named database exists, so please create it if it doesn't exist.
 *
 * Right now we only support RSA keys so each entry is stored use its RSA public key values. If a key
 * exists in the database then it has all permissions, if it doesn't then it has none.
 */
public class BogusRequestAuthorization implements RequestAuthorization {
    protected final String KeyDatabaseName;
    protected final String tag = "BogusRequestAuthorization";

    public BogusRequestAuthorization(String keyDatabaseName) {
        assert keyDatabaseName != null && false == "".equals(keyDatabaseName);
        this.KeyDatabaseName = keyDatabaseName;
    }

    @Override
    public boolean Authorize(Manager manager, URLConnection urlConnection) {
        List<String> pathSegments = Router.splitPath(urlConnection.getURL());

        // Everything is legal with the key database, gives you the warm fuzzies, doesn't it?
        if (pathSegments.size() == 0 || pathSegments.get(0).equals(KeyDatabaseName)) {
            return true;
        }

        Database keyDatabase = null;
        try {
            keyDatabase = manager.getExistingDatabase(KeyDatabaseName);
        } catch (CouchbaseLiteException e) {
            Log.e(tag, "If the DB doesn't exist we should have gotten null, not an exception. So something went wrong.", e);
        }

        // No database? Then no one is authorized.
        if (keyDatabase == null) {
            insecureConnection(urlConnection);
            return false;
        }

        javax.security.cert.X509Certificate[] certChain;

        try {
            certChain = urlConnection.getSSLSession().getPeerCertificateChain();
        } catch (SSLPeerUnverifiedException e) {
            insecureConnection(urlConnection);
            return false;
        }

        if (certChain.length == 0) {
            insecureConnection(urlConnection);
            return false;
        }

        PublicKey publicKey = certChain[certChain.length - 1].getPublicKey();
        if ((publicKey instanceof java.security.interfaces.RSAPublicKey) == false) {
            insecureConnection(urlConnection);
            return false;
        }

        java.security.interfaces.RSAPublicKey rsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
        String keyId = BogusAuthorizeCouchDocument.generateRsaKeyId(rsaPublicKey);

        RevisionList revisionList = keyDatabase.getAllRevisionsOfDocumentID(keyId, true);

        if (revisionList.size() != 1) {
            insecureConnection(urlConnection);
            return false;
        }

        EnumSet<Database.TDContentOptions> tdContentOptions = EnumSet.noneOf(Database.TDContentOptions.class);
        RevisionInternal revision =
                keyDatabase.getDocumentWithIDAndRev(
                        keyId,
                        revisionList.getAllRevIds().get(revisionList.getAllRevIds().size() - 1),
                        tdContentOptions);

        // Looking up the doc by the RSA derived key ID but then doing a security check by comparing the values in the
        // document leads to a potential denial of service attack where someone figures out how to get an ID that
        // matches someone else's but attached to a different key. In theory this is impossible since our key ID fully
        // encodes the public key's value. So if we really believed that then just matching on the ID should be
        // enough. When we right the real code we'll have to model this more carefully.
        ObjectMapper mapper = new ObjectMapper();
        try {
            BogusAuthorizeCouchDocument keyClassForTests = mapper.readValue(revision.getJson(), BogusAuthorizeCouchDocument.class);

            try {
                if (new ThaliPublicKeyComparer(publicKey).KeysEqual(keyClassForTests.generatePublicKey()) == false) {
                    insecureConnection(urlConnection);
                    return false;
                }
                return true;
            } catch (Exception e) {
                // A 500 would be better
                insecureConnection(urlConnection);
                return false;
            }
        } catch (IOException e) {
            insecureConnection(urlConnection);
            return false;
        }
    }

    private void insecureConnection(URLConnection urlConnection) {
        urlConnection.setResponseCode(Status.FORBIDDEN);
        try {
            urlConnection.getResponseOutputStream().close();
        } catch (IOException e) {
            android.util.Log.e("ThaliTestServer", "Error closing empty output stream");
        }
    }
}
