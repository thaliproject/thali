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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import com.couchbase.lite.replicator.Replication;

public class ReplicationManager implements Runnable {
    private static final String replicationDatabaseName = "replicationdb";

    private ConcurrentHashMap<String, Object> replicationMap;
    private Manager couchManager;
    private static boolean shutdownServer = false;

    private static final int replicationFrequency = 30; // in seconds
    private static final int threadSleepTime = 100; // in milliseconds
    private static final int threadIterations = (replicationFrequency * 1000) / threadSleepTime;

    public ReplicationManager(Manager manager) {
        this.replicationMap = new ConcurrentHashMap<String, Object>();
        this.couchManager = manager;
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

    private String requestKey(String from, String to) {
        if((from == null) || (to == null)) {
            return null;
        }
        String key = null;
        try {
            String text = from + "::" + to;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            key = convertByteArrayToHexString(hash);
        } catch(NoSuchAlgorithmException e) {
            key = null;
        } catch(UnsupportedEncodingException e) {
            key = null;
        }
        return key;
    }

    private synchronized void manageReplicationRequest(ReplicatorArguments replicationArgs, boolean add) throws IllegalArgumentException {
        String from = replicationArgs.getSource();
        String to = replicationArgs.getTarget();
        if((from == null) || (to == null)) {
            throw new IllegalArgumentException("Replication requires valid source and target.");
        }
        String key = requestKey(from, to);
        if(add) {
            replicationMap.put(key, replicationArgs);
        } else {
            replicationMap.remove(key);
        }
    }

    private Database getDatabase() {
        Database db = null;
        try {
            db = couchManager.getDatabase(replicationDatabaseName);
        } catch(CouchbaseLiteException e) {
            throw new RuntimeException("Unable to open database.", e);
        }
        return db;
    }

    private Document getReplicationDocument(Database db, String docId) {
        if((db == null) || (docId == null)) {
            return null;
        }
        return db.getDocument(docId);
    }

    private void addOrUpdateReplication(ReplicatorArguments replicationArgs) {
        String docId = requestKey(replicationArgs.getSource(), replicationArgs.getTarget());
        Database db = getDatabase();
        Document doc = getReplicationDocument(db, docId);
        final String argJson = replicationArgs.getPropertiesAsJson();
        try {
            doc.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    Map<String, Object> properties = newRevision.getUserProperties();
                    properties.put("arg_json", argJson);
                    newRevision.setUserProperties(properties);
                    return true;
                }
            });
        } catch(CouchbaseLiteException e) {
            // what to do???
            e.printStackTrace();
        }
    }

    private void deleteReplication(ReplicatorArguments replicationArgs) {
        String docId = requestKey(replicationArgs.getSource(), replicationArgs.getTarget());
        Database db = getDatabase();
        Document doc = getReplicationDocument(db, docId);
        if((doc != null) && (!doc.isDeleted()) && (doc.getCurrentRevisionId() != null)) {
            try {
                doc.delete();
            } catch(CouchbaseLiteException e) {
                throw new RuntimeException("Unable to delete replication request", e);
            }
        }
    }

    private void removeReplicationRequest(ReplicatorArguments replicationArgs) throws IllegalArgumentException
    {
        manageReplicationRequest(replicationArgs, false);
    }

    private void addReplicationRequest(ReplicatorArguments replicationArgs) throws IllegalArgumentException
    {
        manageReplicationRequest(replicationArgs, true);
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

    private Map<String, String> getReplicationRequests() {
        Map<String, String> requests = new HashMap<String, String>();
        Database db = getDatabase();
        Query query = db.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        try {
            QueryEnumerator result = query.run();
            for(Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                Document doc = row.getDocument();
                String argJson = (String)doc.getProperty("arg_json");
                if (argJson != null) {
                    requests.put(row.getDocumentId(), argJson);
                } else {
                    System.out.println("request missing -- request body empty");
                }
            }
        } catch(CouchbaseLiteException e) {
            // TODO -- query failed to run!!!
            e.printStackTrace();;
        }
        return requests;
    }

    public void setShutdownServer() {
        shutdownServer = true;
    }

    public void run() {
        int iterations = 0;
        while(!shutdownServer) {
            if(iterations == threadIterations) {
                // do the check every ten seconds
                Map<String, String> requests = getReplicationRequests();
                if(requests != null && requests.size() > 0) {
                    for(Map.Entry<String, String> entry : requests.entrySet()) {
                        try {
                            ReplicatorArguments args = ReplicatorArguments.getReplicatorArgumentsFromJson(entry.getValue(), couchManager, null);
                            Replication rep = couchManager.getReplicator(args);
                            rep.start();
                        } catch(CouchbaseLiteException e) {
                            // TODO -- log something
                            continue;
                        } catch(java.lang.NullPointerException e) {
                            // TODO -- log something -- manager code not checking for null
                            continue;
                        }
                    }
                }
                iterations = 0;
            }
            iterations++;

            try {
                Thread.sleep(threadSleepTime);
            } catch(InterruptedException e) {
                // interupted
                shutdownServer = true;
                continue;
            }
        }
    }


}
