package com.msopentech.thali.utilities.android.test;

import com.couchbase.cblite.*;
import com.couchbase.cblite.router.CBLRequestAuthorization;
import com.couchbase.cblite.router.CBLRouter;
import com.couchbase.cblite.router.CBLURLConnection;
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
public class ThaliTestServiceAuthorize implements CBLRequestAuthorization {
    @Override
    public boolean Authorize(CBLServer cblServer, CBLURLConnection cblurlConnection) {
        List<String> pathSegments = CBLRouter.splitPath(cblurlConnection.getURL());

        // For now all we really care about are attempts to access the data database.
        if (pathSegments.size() == 0 || pathSegments.get(0).equals(ThaliTestEktorpClient.TestDatabaseName) == false) {
            return true;
        }

        CBLDatabase keyDatabase = cblServer.getExistingDatabaseNamed(ThaliTestEktorpClient.KeyDatabaseName);

        // No database? Then no one is authorized.
        if (keyDatabase == null) {
            InsecureConnection(cblurlConnection);
            return false;
        }

        CBLRevisionList revisionList = keyDatabase.getAllRevisionsOfDocumentID(ThaliTestEktorpClient.KeyId, true);
        EnumSet<CBLDatabase.TDContentOptions> tdContentOptionses = EnumSet.noneOf(CBLDatabase.TDContentOptions.class);
        CBLRevision revision =
                keyDatabase.getDocumentWithIDAndRev(
                        ThaliTestEktorpClient.KeyId,
                        revisionList.getAllRevIds().get(revisionList.getAllRevIds().size() - 1),
                        tdContentOptionses);

        ObjectMapper mapper = new ObjectMapper();
        try {
            CouchDBDocumentKeyClassForTests keyClassForTests = mapper.readValue(revision.getJson(), CouchDBDocumentKeyClassForTests.class);
            if (CouchDBDocumentKeyClassForTests.RSAKeyType.equals(keyClassForTests.getKeyType()) == false) {
                // A 500 would be more appropriate but we are just testing
                InsecureConnection(cblurlConnection);
                return false;
            }

            SSLSession sslSession = cblurlConnection.getSSLSession();
            try {
                javax.security.cert.X509Certificate[] certChain = sslSession.getPeerCertificateChain();
                if (new ThaliPublicKeyComparer(certChain[certChain.length - 1]
                        .getPublicKey())
                        .KeysEqual(keyClassForTests.generatePublicKey()) == false) {
                    InsecureConnection(cblurlConnection);
                    return false;
                }
                return true;
            } catch (Exception e) {
                // A 500 would be better
                InsecureConnection(cblurlConnection);
                return false;
            }
        } catch (IOException e) {
            InsecureConnection(cblurlConnection);
            return false;
        }
    }

    private void InsecureConnection(CBLURLConnection cblurlConnection) {
        cblurlConnection.setResponseCode(CBLStatus.FORBIDDEN);
        try {
            cblurlConnection.getResponseOutputStream().close();
        } catch (IOException e) {
            android.util.Log.e("ThaliTestServer", "Error closing empty output stream");
        }
    }
}
