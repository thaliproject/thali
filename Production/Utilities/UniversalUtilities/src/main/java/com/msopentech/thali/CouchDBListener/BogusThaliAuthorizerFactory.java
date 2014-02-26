package com.msopentech.thali.CouchDBListener;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.ReplicatorArguments;
import com.couchbase.lite.Status;
import com.couchbase.lite.auth.Authorizer;
import com.couchbase.lite.auth.AuthorizerFactory;
import com.msopentech.thali.utilities.universal.HttpKeyURL;

import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Map;

/**
 * Created by yarong on 1/13/14.
 */
public class BogusThaliAuthorizerFactory implements AuthorizerFactory {
    public final static String thaliFieldName = "BogusThali";

    private KeyStore clientKeyStore;
    private char[] clientPassPhrase;

    public BogusThaliAuthorizerFactory(KeyStore clientKeyStore, char[] clientPassPhrase) {
        this.clientKeyStore = clientKeyStore;
        this.clientPassPhrase = clientPassPhrase;
    }

    @Override
    public Authorizer getAuthorizer(ReplicatorArguments replicatorArguments) throws CouchbaseLiteException {
        Map<String, Object> authParms =
                replicatorArguments.getPush() ? replicatorArguments.getTargetAuth() : replicatorArguments.getSourceAuth();

        if (authParms == null || authParms.containsKey(thaliFieldName) == false) {
            return null;
        }

        try {
            Map<String, Object> childOfAuthParams = (Map<String, Object>)authParms.get(thaliFieldName);

            if (childOfAuthParams != null && childOfAuthParams.size() != 0) {
                throw new CouchbaseLiteException(thaliFieldName + " must be an empty object",
                        new Status(Status.BAD_REQUEST));
            }
        } catch (ClassCastException e) {
            throw new CouchbaseLiteException(thaliFieldName + " must be a JSON object", new Status(Status.BAD_REQUEST));
        }

        String remoteHttpKeyURL =
                replicatorArguments.getPush() ? replicatorArguments.getTarget() : replicatorArguments.getSource();

        try {
            HttpKeyURL httpKeyURL = new HttpKeyURL(remoteHttpKeyURL);
            String httpsURL = httpKeyURL.createHttpsUrl();
            if (replicatorArguments.getPush()) {
                replicatorArguments.setTarget(httpsURL);
            } else {
                replicatorArguments.setSource(httpsURL);
            }

            return new BogusThaliAuthorizer(httpKeyURL.getServerPublicKey(), clientKeyStore, clientPassPhrase);
        } catch (IllegalArgumentException e) {
            throw new CouchbaseLiteException("remote URL value " + remoteHttpKeyURL + " is not a valid HttpKeyURL.",
                    new Status(Status.BAD_REQUEST));
        } catch (URISyntaxException e) {
            throw new RuntimeException("We got an exception trying to create the HTTPS URL and that shouldn't have happened.", e);
        }
    }
}
