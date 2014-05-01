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
        // TODO: BUGBUG - This code tried to prevent an attack (or even accident) where someone sent in a replication
        // with information on how to authenticate to the remote entity and the authentication information was not
        // recognized. The right behavior in the case like that is to fail (e.g. sometimes it's really not safe to
        // ignore an unrecognized argument). But if we don't fail then one of two things could happen, both potentially
        // bad. We make the replication request but without the requested authentication. This could fail (not too bad)
        // or it could suceed but only return information that an unauthenticated called is allowed to see. The confusion
        // from that one won't be much fun. The other possibility is that the replicator uses a default authorization
        // credential which might not be the credential the caller wanted and thus end up exposing to the remote location
        // a different identity than was intended! Bad. Bad. Bad. So what we really need is to get agreement in the Couchbase
        // community for how authorization arguments should be passed in and agree that if they are and the arguments aren't
        // recognized then the authorization will fail in a well defined way.
        // The code below was a first step in this direction. But for now I'm disabling it because I just need to get stuff
        // working at all. But this is a big ugly security hole and we need to fix it.
//        Map<String, Object> authParms =
//                replicatorArguments.getPush() ? replicatorArguments.getTargetAuth() : replicatorArguments.getSourceAuth();
//
//        if (authParms == null || authParms.containsKey(thaliFieldName) == false) {
//            return null;
//        }
//
//        try {
//            Map<String, Object> childOfAuthParams = (Map<String, Object>)authParms.get(thaliFieldName);
//
//            if (childOfAuthParams != null && childOfAuthParams.size() != 0) {
//                throw new CouchbaseLiteException(thaliFieldName + " must be an empty object",
//                        new Status(Status.BAD_REQUEST));
//            }
//        } catch (ClassCastException e) {
//            throw new CouchbaseLiteException(thaliFieldName + " must be a JSON object", new Status(Status.BAD_REQUEST));
//        }

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
            return null;
            // TODO: BUGBUG - When we have real security this will be an exception and not just a null
//            throw new CouchbaseLiteException("remote URL value " + remoteHttpKeyURL + " is not a valid HttpKeyURL.",
//                    new Status(Status.BAD_REQUEST));
        } catch (URISyntaxException e) {
            throw new RuntimeException("We got an exception trying to create the HTTPS URL and that shouldn't have happened.", e);
        }
    }
}
