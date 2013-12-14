package com.msopentech.thali.utilities.android.test;

import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.RevisionList;
import com.couchbase.lite.Status;
import com.couchbase.lite.internal.RevisionInternal;
import com.couchbase.lite.router.RequestAuthorization;
import com.couchbase.lite.router.Router;
import com.couchbase.lite.router.URLConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msopentech.thali.utilities.universal.CouchDBDocumentKeyClassForTests;
import com.msopentech.thali.utilities.universal.ThaliPublicKeyComparer;
import com.msopentech.thali.utilities.universal.ThaliTestEktorpClient;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Just for testing purposes. It will only allow requests to the TestDatabaseName if it comes from a client
 * whose key is in KeyDatabaseName.
 */
public class ThaliTestServiceAuthorize implements RequestAuthorization {
    @Override
    public boolean Authorize(Manager manager, URLConnection urlConnection) {
        List<String> pathSegments = Router.splitPath(urlConnection.getURL());

        // For now all we really care about are attempts to access the data database.
        if (pathSegments.size() == 0 || pathSegments.get(0).equals(ThaliTestEktorpClient.TestDatabaseName) == false) {
            return true;
        }

        Database keyDatabase = manager.getExistingDatabase(ThaliTestEktorpClient.KeyDatabaseName);

        // No database? Then no one is authorized.
        if (keyDatabase == null) {
            InsecureConnection(urlConnection);
            return false;
        }

        RevisionList revisionList = keyDatabase.getAllRevisionsOfDocumentID(ThaliTestEktorpClient.KeyId, true);
        EnumSet<Database.TDContentOptions> tdContentOptionses = EnumSet.noneOf(Database.TDContentOptions.class);
        RevisionInternal revision =
                keyDatabase.getDocumentWithIDAndRev(
                        ThaliTestEktorpClient.KeyId,
                        revisionList.getAllRevIds().get(revisionList.getAllRevIds().size() - 1),
                        tdContentOptionses);

        ObjectMapper mapper = new ObjectMapper();
        try {
            CouchDBDocumentKeyClassForTests keyClassForTests = mapper.readValue(revision.getJson(), CouchDBDocumentKeyClassForTests.class);
            if (CouchDBDocumentKeyClassForTests.RSAKeyType.equals(keyClassForTests.getKeyType()) == false) {
                // A 500 would be more appropriate but we are just testing
                InsecureConnection(urlConnection);
                return false;
            }

            SSLSession sslSession = urlConnection.getSSLSession();
            try {
                javax.security.cert.X509Certificate[] certChain = sslSession.getPeerCertificateChain();
                if (new ThaliPublicKeyComparer(certChain[certChain.length - 1]
                        .getPublicKey())
                        .KeysEqual(keyClassForTests.generatePublicKey()) == false) {
                    InsecureConnection(urlConnection);
                    return false;
                }
                return true;
            } catch (Exception e) {
                // A 500 would be better
                InsecureConnection(urlConnection);
                return false;
            }
        } catch (IOException e) {
            InsecureConnection(urlConnection);
            return false;
        }
    }

    private void InsecureConnection(URLConnection urlConnection) {
        urlConnection.setResponseCode(Status.FORBIDDEN);
        try {
            urlConnection.getResponseOutputStream().close();
        } catch (IOException e) {
            android.util.Log.e("ThaliTestServer", "Error closing empty output stream");
        }
    }
}
