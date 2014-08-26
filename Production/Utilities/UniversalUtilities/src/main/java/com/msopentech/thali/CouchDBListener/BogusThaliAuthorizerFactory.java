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

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.ReplicatorArguments;
import com.couchbase.lite.auth.Authorizer;
import com.couchbase.lite.auth.AuthorizerFactory;
import com.msopentech.thali.utilities.universal.HttpKeyURL;

import java.net.*;
import java.security.KeyStore;

/**
 * Created by yarong on 1/13/14.
 */
public class BogusThaliAuthorizerFactory implements AuthorizerFactory {
    public final static String thaliFieldName = "BogusThali";

    protected final KeyStore clientKeyStore;
    protected final char[] clientPassPhrase;
    protected final Proxy proxy;
    protected ReplicationManager replicationManager;

    public BogusThaliAuthorizerFactory(KeyStore clientKeyStore, char[] clientPassPhrase, Proxy proxy) {
        this.clientKeyStore = clientKeyStore;
        this.clientPassPhrase = clientPassPhrase;
        this.proxy = proxy;
    }

    public void setReplicationManager(ReplicationManager repMgr) {
        this.replicationManager = repMgr;
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

        // is this a managed replication?
        if(replicatorArguments.getManagedReplication()) {
            return new ReplicationManagerAuthorizer(replicationManager, replicatorArguments);
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

            // 127.0.0.1 addresses can't be resolved via Tor so we have to nullify the proxy. We would use localhost
            // when we are replicating between two local database. Unfortunately CouchBase doesn't support local to
            // local replication so we have to fake it by making one of the source/target look remote. In theory
            // we could tighten this up by just removing the proxy for 127.0.0.1 and using Tor for everything else
            // but this is a problem when the machine we are talking to might on the local network but behind a firewall
            // and so not visible from Tor. So for now we only route Tor onion addresses via the proxy. But we need
            // more robust logic since defaulting to Tor is clearly the right thing. We could easily put in a test to
            // see if Tor can 'see' the address and if not default back to not using Tor although that also opens
            // up possible security holes. We need to think this through.
            // TODO: We really need to support local to local replication in a more secure way, looking to match addresses
            // just feels like some weird unicode security hole waiting to happen.
            // https://github.com/thaliproject/thali/issues/69
            boolean doNotUseProxy = httpKeyURL.getHost().endsWith(".onion") == false;

            return new BogusThaliAuthorizer(httpKeyURL.getServerPublicKey(), clientKeyStore, clientPassPhrase,
                    doNotUseProxy ? null : proxy);
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
