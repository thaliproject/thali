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

import com.couchbase.lite.*;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliGroupUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GroupReplicator {
    public static final String addressBookDatabaseName = "addressbook";
    private static final Logger LOG = LoggerFactory.getLogger(GroupReplicator.class);
    /**
     * If the replication is a group replication then the group membership will be expanded. If it's a non
     * group replication then submitted replicatorArgument will just be returned. Note that this method
     * is expensive because it does multiple database queries synchronously. So make sure to run it on its own
     * thread.
     * @param replicatorArguments
     * @param couchManager
     * @return
     */
    static public List<ReplicatorArguments> expandGroupReplications(ReplicatorArguments replicatorArguments,
                                                                    Manager couchManager,
                                                                    PublicKey serverPublicKey)
            throws CouchbaseLiteException {
        List<ReplicatorArguments> result = new ArrayList<ReplicatorArguments>();

        boolean sourceIsGroup = ThaliGroupUrl.isThaliGroupUrl(replicatorArguments.getSource(), serverPublicKey);
        boolean targetIsGroup = ThaliGroupUrl.isThaliGroupUrl(replicatorArguments.getTarget(), serverPublicKey);

        if (sourceIsGroup == false && targetIsGroup == false) {
            result.add(replicatorArguments);
            return result;
        }

        if (sourceIsGroup && targetIsGroup) {
            throw new IllegalArgumentException("Both source and target are thali group URIs, that shouldn't even be theoretically possible!");
        }

        ThaliGroupUrl thaliGroupUrl =
                new ThaliGroupUrl(
                        sourceIsGroup ? replicatorArguments.getSource() : replicatorArguments.getTarget(),
                        serverPublicKey);

        if (thaliGroupUrl.getGroupName().equals("all") == false) {
            throw new IllegalArgumentException("We only support group 'all' at the moment");
        }

        List<HttpKeyURL> groupMembership = resolveAllGroupMembership(couchManager);

        for(HttpKeyURL httpKeyURL : groupMembership) {
            ReplicatorArguments translatedReplicatorArguments =
                    new ReplicatorArguments(
                            replicatorArguments.getRawProperties(), null, replicatorArguments.getPrincipal());

            HttpKeyURL httpKeyURLWithPath = transformHttpKeyUrl(httpKeyURL, thaliGroupUrl);

            if (sourceIsGroup) {
                translatedReplicatorArguments.setSource(httpKeyURLWithPath.toString());
            } else {
                translatedReplicatorArguments.setTarget(httpKeyURLWithPath.toString());
            }
            result.add(translatedReplicatorArguments);
        }

        return result;
    }

    static public HttpKeyURL transformHttpKeyUrl(HttpKeyURL httpKeyURL, ThaliGroupUrl thaliGroupUrl) {
        // TODO - Right now we are keeping things simple, we only accept HttpKeyUrls that have no path
        // or just a slash for the path. In the future we need to expand this, especially to support
        // multi-tenant peers who will need to use the HttpKeyUrl path to distinguish tenants.
        String httpKeyURLPath = httpKeyURL.getPath();
        if (httpKeyURLPath != null && httpKeyURLPath.isEmpty() == false) {
            throw new IllegalArgumentException("Right now we only support httpKeyURLs with no path values.");
        }

        // TODO: This mechanism for combining paths is insecure, we need to validate that nothing odd is going
        // to happen, especially in the context of escaping URLs.
        HttpKeyURL httpKeyURLWithPath = new HttpKeyURL(httpKeyURL.getServerPublicKey(), httpKeyURL.getHost(),
                httpKeyURL.getPort(), thaliGroupUrl.getPath(), null, null);

        return httpKeyURLWithPath;
    }

    static public String removeFirstAndLastSlashIfPresent(String path) {
        if (path != null) {
            int startIndex = path.startsWith("/") ? 1 : 0;
            int endIndex = path.length() - (path.endsWith("/") ? -2 : -1 );
            return path.substring(startIndex, endIndex);
        } else {
            return "";
        }
    }

    static public List<HttpKeyURL> resolveAllGroupMembership(Manager couchManager) throws CouchbaseLiteException {
        List<HttpKeyURL> result = new ArrayList<HttpKeyURL>();
        Database database = couchManager.getExistingDatabase(addressBookDatabaseName);
        if (database != null) {
            Query allDocsQuery = database.createAllDocumentsQuery();
            allDocsQuery.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
            for (Iterator<QueryRow> iterator = allDocsQuery.run(); iterator.hasNext(); ) {
                QueryRow row = iterator.next();
                // Todo - We need to deal with conflicting revisions
                // Todo - We need logic to validate addressbook entries so people can't put in garbage!
                Document document = row.getDocument();
                String httpKeyUrlString = (String) document.getProperty("httpKeyUrl");
                if (httpKeyUrlString != null && httpKeyUrlString.isEmpty() == false) {
                    try {
                        result.add(new HttpKeyURL(httpKeyUrlString));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Malformed httpKeyUrl in addressbook");
                    }
                }
            }
        }

        return result;
    }
}
