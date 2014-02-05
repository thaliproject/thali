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

package com.msopentech.thali.utilities.android.test;

import android.test.AndroidTestCase;
import android.util.Log;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.msopentech.thali.CouchDBListener.AndroidThaliListener;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliReplicationCommand;
import com.msopentech.thali.utilities.universal.test.ThaliTestEktorpClient;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;
import org.ektorp.CouchDbConnector;
import org.ektorp.ReplicationStatus;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AndroidEktorpCreateClientBuilderTest extends AndroidTestCase {
    private AndroidThaliListener thaliTestServer;
    private File filesDir;
    private int port;
    private String host = ThaliListener.DefaultThaliDeviceHubAddress;
    private char[] passPhrase = ThaliCryptoUtilities.DefaultPassPhrase;
    private AndroidEktorpCreateClientBuilder createClientBuilder;
    private ThaliTestEktorpClient.ConfigureRequestObjects configureRequestObjects;

    @Override
    public void setUp() throws InterruptedException, UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        ThaliTestUtilities.configuringLoggingApacheClient();

        thaliTestServer = new AndroidThaliListener();
        filesDir = getContext().getFilesDir();
        File keyStore = ThaliCryptoUtilities.getThaliKeyStoreFileObject(filesDir);

        // We want to start with a clean state
        if (keyStore.exists()) {
            keyStore.delete();
        }

        // We use a random port (e.g. port 0) both because it's good hygiene and because it keeps us from conflicting
        // with the 'real' Thali Device Hub if it's running.
        thaliTestServer.startServer(getContext().getFilesDir(), 0);

        port = thaliTestServer.getSocketStatus().getPort();

        createClientBuilder = new AndroidEktorpCreateClientBuilder();

        configureRequestObjects = ThaliTestEktorpClient.generateRequestObjects(host, port, passPhrase, createClientBuilder, filesDir);
    }

    @Override
    public void tearDown() {
        if (thaliTestServer != null) {
            thaliTestServer.stopServer();
        }
    }

    public class ReplicationChangeListener implements Replication.ChangeListener {
        public final Semaphore callWhenSynchDone;
        public final Replication.ReplicationStatus replicationStatus;

        public ReplicationChangeListener(Replication.ReplicationStatus replicationStatus) throws InterruptedException {
            callWhenSynchDone = new Semaphore(1);
            callWhenSynchDone.acquire();
            this.replicationStatus = replicationStatus;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            Log.d("ick", event.getSource().getStatus().toString());
            if (event.getSource().getStatus() == replicationStatus) {
                callWhenSynchDone.release();
            }
        }
    }

    public void testPullReplication() throws InvalidKeySpecException, NoSuchAlgorithmException,
            InterruptedException, MalformedURLException, CouchbaseLiteException, URISyntaxException {
        String localName = ThaliTestEktorpClient.TestDatabaseName;
        String remoteName = ThaliTestEktorpClient.ReplicationTestDatabaseName;
        final CouchDbConnector localConnector = configureRequestObjects.testDatabaseConnector;
        final CouchDbConnector remoteConnector = configureRequestObjects.replicationDatabaseConnector;
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(remoteName);

        // Set up docs in target and then pull from full 'local' to empty 'remote'
        ThaliTestEktorpClient.setUpData(configureRequestObjects.thaliCouchDbInstance, 1,
                ThaliTestEktorpClient.MaximumTestRecords, configureRequestObjects.clientPublicKey);
        PullReplicateAndTest(localName, remoteName, false);

    }

    public void testPushReplication() throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, IOException, InterruptedException, InvalidKeySpecException, URISyntaxException, CouchbaseLiteException {

        String localName = ThaliTestEktorpClient.TestDatabaseName;
        String remoteName = ThaliTestEktorpClient.ReplicationTestDatabaseName;
        final CouchDbConnector localConnector = configureRequestObjects.testDatabaseConnector;
        final CouchDbConnector remoteConnector = configureRequestObjects.replicationDatabaseConnector;
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(remoteName);

        // Set up docs and then replicate from 'local' to empty 'remote'
        ThaliTestEktorpClient.setUpData(configureRequestObjects.thaliCouchDbInstance, 1,
                ThaliTestEktorpClient.MaximumTestRecords, configureRequestObjects.clientPublicKey);
        PushReplicateAndTest(localName, remoteName, false);

        // Add and remove and alter a doc from local and re-replicate
        ThaliTestEktorpClient.GenerateDoc(localConnector);
        ThaliTestEktorpClient.DeleteDoc(localConnector);
        ThaliTestEktorpClient.AlterDoc(localConnector);
        PushReplicateAndTest(localName, remoteName, false);

        // Reverse the direction of the replication and see if anything changes
        PushReplicateAndTest(remoteName, localName, false);

        // Add and remove and alter a doc from the 'remote' and reverse the replication and see if it works
        ThaliTestEktorpClient.AlterDoc(remoteConnector);
        ThaliTestEktorpClient.DeleteDoc(remoteConnector);
        ThaliTestEktorpClient.GenerateDoc(remoteConnector);
        PushReplicateAndTest(remoteName, localName, false);

        // Add and remove and alter a doc from 'local' and then set up a continuous replication to remote
        ThaliTestEktorpClient.GenerateDoc(localConnector);
        ThaliTestEktorpClient.AlterDoc(localConnector);
        ThaliTestEktorpClient.DeleteDoc(localConnector);
        ReplicationChangeListener localToRemoteReplicationChangeListener = PushReplicateAndTest(localName, remoteName, true);

        // Add and remove docs and see if the continuous replication picks it up
        Execute execute = new Execute() {
            @Override
            public void runit() {
                ThaliTestEktorpClient.DeleteDoc(localConnector);
                ThaliTestEktorpClient.GenerateDoc(localConnector);
                ThaliTestEktorpClient.AlterDoc(localConnector);
            }
        };
        ValidateContinuousReplication(localConnector, remoteConnector, localToRemoteReplicationChangeListener, execute);

        // Set up a continuous replication from 'target' to 'source' (but leave the other replication running)
        ReplicationChangeListener remoteToLocalReplicationChangeListener = PushReplicateAndTest(remoteName, localName, true);

        // Add and remove docs from 'target' and see if the changes safely make it to 'source'
        execute = new Execute() {
            @Override
            public void runit() {
                ThaliTestEktorpClient.DeleteDoc(remoteConnector);
                ThaliTestEktorpClient.GenerateDoc(remoteConnector);
                ThaliTestEktorpClient.GenerateDoc(remoteConnector);
                ThaliTestEktorpClient.AlterDoc(remoteConnector);
                ThaliTestEktorpClient.GenerateDoc(remoteConnector);
            }
        };

        ValidateContinuousReplication(remoteConnector, localConnector, remoteToLocalReplicationChangeListener, execute);
    }

    public interface Execute {
        public void runit();
    }

    public static void ValidateContinuousReplication(CouchDbConnector sourceConnector, CouchDbConnector targetConnector,
                                                     ReplicationChangeListener replicationChangeListener, Execute execute)
            throws InterruptedException {
        // Due to https://github.com/couchbase/couchbase-lite-android-core/issues/55 we can't be sure that the semaphore
        // was cleared so we might have to clear it manually.
        if (replicationChangeListener.callWhenSynchDone.availablePermits() == 0) {
            replicationChangeListener.callWhenSynchDone.release();
        }
        replicationChangeListener.callWhenSynchDone.acquire();
        execute.runit();
        replicationChangeListener.callWhenSynchDone.tryAcquire(10, TimeUnit.SECONDS);
        ThaliTestEktorpClient.validateDatabaseEquality(sourceConnector, targetConnector);
    }

    private ReplicationChangeListener PullReplicateAndTest(String local, String remote, boolean continuous)
            throws InterruptedException, MalformedURLException, CouchbaseLiteException, URISyntaxException {
        return ReplicateAndTest(local, remote, false, continuous);
    }

    private ReplicationChangeListener PushReplicateAndTest(String source, String target, boolean continuous)
            throws InterruptedException, MalformedURLException, CouchbaseLiteException, URISyntaxException {
        return ReplicateAndTest(source, target, true, continuous);
    }

    /**
     * Executes a push replication from the source to the target and once the replication is done tests if the
     * two databases are equal.
     * @param source
     * @param target
     * @param push
     * @param continuous
     * @throws InterruptedException
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @return
     */
    private ReplicationChangeListener ReplicateAndTest(String source, String target, boolean push, boolean continuous)
            throws InterruptedException, MalformedURLException, URISyntaxException, CouchbaseLiteException {
        HttpKeyURL remoteUrl = new HttpKeyURL(configureRequestObjects.serverPublicKey, host, port, "/" + (push ? target : source), null, null);
        ThaliReplicationCommand thaliReplicationCommand =
                new ThaliReplicationCommand.Builder()
                        .source(push ? source : remoteUrl.toString())
                        .target(push ? remoteUrl.toString() : source)
                        .createTarget(true)
                        .continuous(continuous)
                        .build();
        ReplicationStatus replicationStatus = configureRequestObjects.thaliCouchDbInstance.replicate(thaliReplicationCommand);
        if (replicationStatus.isOk() == false) {
            throw new RuntimeException("Replication failed!");
        }

        Manager manager = thaliTestServer.getManager();
        Database database = manager.getDatabase(source);
        URL url = new URL(remoteUrl.createHttpsUrl());
        Replication replication = database.getActiveReplicator(url, true);
        ReplicationChangeListener replicationChangeListener =
                new ReplicationChangeListener(
                        continuous ? Replication.ReplicationStatus.REPLICATION_IDLE : Replication.ReplicationStatus.REPLICATION_STOPPED);
        replication.addChangeListener(replicationChangeListener);

        replicationChangeListener.callWhenSynchDone.tryAcquire(10, TimeUnit.SECONDS);
        CouchDbConnector sourceTestConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(source, false);
        CouchDbConnector targetTestConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(target, false);
        ThaliTestEktorpClient.validateDatabaseEquality(sourceTestConnector, targetTestConnector);
        return replicationChangeListener;
    }


    public void testClient() throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, IOException, InterruptedException, InvalidKeySpecException {
        ThaliTestEktorpClient.runRetrieveTest(
                ThaliListener.DefaultThaliDeviceHubAddress, port, ThaliCryptoUtilities.DefaultPassPhrase, new AndroidEktorpCreateClientBuilder(), getContext().getFilesDir());
    }
}
