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

import java.security.PublicKey;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.msopentech.thali.utilities.universal.CblLogTags;

public class ReplicationManager {
    public static String replicationDatabaseName = "replicationdb";

    private final Manager couchManager;
    private final Database replicationDatabase;
    private final PublicKey serverPublicKey;
    private Thread replicationManagerThread;

    private static final int replicationFrequency = 200; // in milliseconds
    private static final int threadSleepTime = 25; // in milliseconds
    private static final int threadIterations = replicationFrequency / threadSleepTime;

    public ReplicationManager(Manager manager, PublicKey serverPublicKey) {
        this.replicationManagerThread = null;
        this.couchManager = manager;
        this.serverPublicKey = serverPublicKey;
        try {
            this.replicationDatabase = couchManager.getDatabase(replicationDatabaseName);
        } catch(CouchbaseLiteException e) {
            throw new RuntimeException("Unable to open replication database.", e);
        }
    }

    private final String HEXMAP = "0123456789ABCDEF";
    private String convertByteArrayToHexString(byte [] bytes) {
        if(bytes == null) {
            return null;
        }
        StringBuilder hex = new StringBuilder(2 * bytes.length);
        for(byte b : bytes) {
            hex.append(HEXMAP.charAt((b & 0xf0) >> 4)).append(HEXMAP.charAt(((b & 0x0f))));
        }
        return hex.toString();
    }

    // https://github.com/thaliproject/thali/issues/59
    private String requestKey(String from, String to) {
        if((from == null) || (to == null)) {
            throw new RuntimeException("From and to are required to generate request key.  From" + from + ", To: " + to);
        }
        String key = null;
        try {
            String text = from + "::" + to;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            key = convertByteArrayToHexString(hash);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm", e);
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding exception", e);
        }
        return key;
    }

    private Document getReplicationDocument(String docId) {
        if(docId == null) {
            throw new IllegalArgumentException("Database and docId must not be null.");
        }
        return this.replicationDatabase.getDocument(docId);
    }

    // TODO --- try actual object -- thali / pull 57 / json/non-json
    private void addOrUpdateReplication(final ReplicatorArguments replicationArgs) {
        String docId = requestKey(replicationArgs.getSource(), replicationArgs.getTarget());
        Document doc = getReplicationDocument(docId);
        try {
            doc.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    Map<String, Object> properties = newRevision.getUserProperties();
                    properties.put("replicationProps", replicationArgs.getRawProperties(true));
                    newRevision.setUserProperties(properties);
                    return true;
                }
            });
        } catch(CouchbaseLiteException e) {
            Log.e(CblLogTags.TAG_THALI_REPLICATION_MANAGER, "Error adding/updating replication request.  Source: " + replicationArgs.getSource() + ", Target: " + replicationArgs.getTarget(), e);
        }
    }

    private void deleteReplication(ReplicatorArguments replicationArgs) {
        String docId = requestKey(replicationArgs.getSource(), replicationArgs.getTarget());
        Document doc = getReplicationDocument(docId);
        if((doc != null) && (!doc.isDeleted()) && (doc.getCurrentRevisionId() != null)) {
            try {
                doc.delete();
            } catch(CouchbaseLiteException e) {
                throw new RuntimeException("Unable to delete replication request", e);
            }
        }
    }

    public void processReplicationRequest(ReplicatorArguments replicationArgs) throws IllegalArgumentException {
        if(replicationArgs != null) {
            if(replicationArgs.getCancel()) {
                deleteReplication(replicationArgs);
            } else {
                addOrUpdateReplication(replicationArgs);
            }
        }
    }

    private List<ReplicatorArguments> getReplicationRequests() {
        List<ReplicatorArguments> requests = new ArrayList<ReplicatorArguments>();
        Query query = this.replicationDatabase.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        try {
            QueryEnumerator result = query.run();
            for(Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                Document doc = row.getDocument();
                Map<String, Object> rawProps = (Map<String, Object>)doc.getProperty("replicationProps");
                if (rawProps != null) {
                    ReplicatorArguments arg = new ReplicatorArguments(rawProps, couchManager, null);
                    requests.addAll(GroupReplicator.expandGroupReplications(arg, couchManager, serverPublicKey));
                } else {
                    Log.e(CblLogTags.TAG_THALI_REPLICATION_MANAGER, "Stored request body missing.  DocID: " + doc.getId());
                }
            }
        } catch(CouchbaseLiteException e) {
            Log.e(CblLogTags.TAG_THALI_REPLICATION_MANAGER, "Query to get replication requests failed.", e);
        }
        return requests;
    }

    public void start() {
        if(replicationManagerThread == null) {
            replicationManagerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int iterations = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        if (iterations == threadIterations) {
                            List<ReplicatorArguments> requests = getReplicationRequests();
                            if (requests != null && requests.size() > 0) {
                                for (ReplicatorArguments entry : requests) {
                                    try {
                                        Replication rep = couchManager.getReplicator(entry);
                                        rep.start();
                                    } catch (CouchbaseLiteException e) {
                                        Log.e(CblLogTags.TAG_THALI_REPLICATION_MANAGER,
                                                "Error starting replication request.", e);
                                    } catch (java.lang.NullPointerException e) {
                                        Log.e(CblLogTags.TAG_THALI_REPLICATION_MANAGER,
                                                "Error starting replication request.", e);
                                    }
                                }
                            }
                            iterations = 0;
                        }
                        iterations++;

                        try {
                            Thread.sleep(threadSleepTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });

            replicationManagerThread.start();
        }
    }

    public void stop() {
        if (replicationManagerThread != null) {
            replicationManagerThread.interrupt();
            try {
                replicationManagerThread.join();
            } catch(InterruptedException e) {
                // shutting down anyway, punt
            }
            replicationManagerThread = null;
        }
    }
}
