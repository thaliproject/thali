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

package com.msopentech.thali.utilities.universal.test;

import com.couchbase.lite.Context;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.replicator.Replication;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.msopentech.thali.utilities.universal.CreateClientBuilder;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliReplicationCommand;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.ReplicationStatus;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class contains all the generic test code to exercise both the Ektorp client as well as CouchBase Lite.
 * Because of inheritance issues I can't make this into a true test class and instead have to rely on the code
 * in the AndroidUtilities and JavaUtilities projects to properly bind this class. I have put in some simple
 * checks to try and detect when the child hasn't bound all the various methods.
 *
 * This test assumes that there is one instance of the thali listener that is used across all the different
 * tests. This is necessary to work around a bug in TJWS (https://github.com/couchbase/couchbase-lite-java-listener/issues/43)
 * that keeps threads (and file locks) when you think you have killed TJWS.
 */
public class ThaliTestEktorpClient {
    public static final String ReplicationTestDatabaseName = "replicationtest";

    public static final int MaximumTestRecords = 10;

    // determine whether or not we should try using Tor
    public static final boolean EnableTorTests = true;

    private final String tdhDirectHost;
    private final String tdhOnionHost;
    private final int tdhOnionPort;
    private final char[] passPhrase;
    private final CreateClientBuilder createClientBuilder;
    private int tdhDirectPort;

    private ThaliListener thaliTestServer = null;
    private ConfigureRequestObjects configureRequestObjects;


    /**
     * Creates a local instance of the Thali Listener to use for testing
     * @param tdhDirectHost
     * @param passPhrase
     * @param context
     * @param createClientBuilder
     * @param childClass This is the class object of the test environment that created this object
     */
    public ThaliTestEktorpClient(String tdhDirectHost, int tdhDirectPort,
                                 char[] passPhrase, Context context,
                                 CreateClientBuilder createClientBuilder, Class childClass,
                                 OnionProxyManager onionProxyManager)
            throws InterruptedException, UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, IOException {

        this.tdhDirectHost = tdhDirectHost;

        this.passPhrase = passPhrase;
        this.createClientBuilder = createClientBuilder;

        thaliTestServer = new ThaliListener();

        thaliTestServer.startServer(context, tdhDirectPort, onionProxyManager);

        this.tdhDirectPort = thaliTestServer.getSocketStatus().getPort();

        HttpKeyURL onionHttpKeyUrl = new HttpKeyURL(thaliTestServer.getHttpKeys().getOnionHttpKeyURL());
        tdhOnionHost = onionHttpKeyUrl.getHost();
        tdhOnionPort = onionHttpKeyUrl.getPort();

        configureRequestObjects =
                    new ConfigureRequestObjects(tdhDirectHost, this.tdhDirectPort, tdhOnionHost, tdhOnionPort, passPhrase,
                            createClientBuilder, context, null, thaliTestServer.getSocksProxy());

        checkIfChildClassExecutesAllTests(childClass);
    }

    public Manager getThaliTestServerManager() throws InterruptedException {
        return thaliTestServer.getManager();
    }

    public void setUp() throws InterruptedException, UnrecoverableEntryException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {

        // Since the databases get deleted before each test run this should be unnecessary but I'm being
        // paranoid.
        for(String databaseName : thaliTestServer.getManager().getAllDatabaseNames()) {
            Database database = thaliTestServer.getManager().getDatabaseWithoutOpening(databaseName, true);
            for(Replication replication : database.getAllReplications()) {
                replication.stop();
            }
        }
    }

    public void tearDown() {
    }

    public void testPullReplication() throws InvalidKeySpecException, NoSuchAlgorithmException,
            InterruptedException, IOException, CouchbaseLiteException, URISyntaxException, UnrecoverableEntryException,
            KeyStoreException, KeyManagementException {
        replicationTestEngine(false, false);

        if(EnableTorTests)
        {
            setUp();
            replicationTestEngine(false, true);
        }
    }

    public void testPushReplication() throws UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, IOException, InterruptedException, InvalidKeySpecException, URISyntaxException,
            CouchbaseLiteException {
        replicationTestEngine(true, false);

        if(EnableTorTests)
        {
            setUp();
            replicationTestEngine(true, true);
        }
    }

    /**
     * This code tests replicating data between two databases, one is local and the other is remote. The push
     * variable controls if replication is push based (in which case the local database will initially be filled
     * with data and the remote database will be empty) or pull base (in which case the local database will be left
     * empty initially and the remote database filled with data).
     * @param push
     * @param useTor Access 'remote' database using Tor
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws MalformedURLException
     * @throws CouchbaseLiteException
     * @throws URISyntaxException
     */
    protected void replicationTestEngine(boolean push, boolean useTor) throws InvalidKeySpecException,
            NoSuchAlgorithmException, InterruptedException, MalformedURLException, CouchbaseLiteException,
            URISyntaxException {
        String localName = ThaliTestUtilities.TestDatabaseName;
        String remoteName = ThaliTestEktorpClient.ReplicationTestDatabaseName;

        CouchDbConnector remoteConnector = useTor ? configureRequestObjects.torReplicationDatabaseConnector :
                configureRequestObjects.replicationDatabaseConnector;

        final CouchDbConnector connectorForInitiallyFullDb =
                push ? configureRequestObjects.testDatabaseConnector : remoteConnector;
        final CouchDbConnector connectorForInitiallyEmptyDb =
                push ? remoteConnector : configureRequestObjects.testDatabaseConnector;

        // With all the re-use around here some deleting is called for
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(localName);
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(remoteName);

        // Set up docs and then replicate from full to empty, in push, the remote is empty, in pull the local is empty
        ThaliTestUtilities.setUpData(configureRequestObjects.thaliCouchDbInstance, push ? localName : remoteName,
                1, ThaliTestEktorpClient.MaximumTestRecords, configureRequestObjects.clientPublicKey);
        ReplicateAndTest(localName, remoteName, push, useTor, false);

        // Add and remove and alter a doc from the full database and re-replicate
        ThaliTestUtilities.GenerateDoc(connectorForInitiallyFullDb);
        ThaliTestUtilities.DeleteDoc(connectorForInitiallyFullDb);
        ThaliTestUtilities.AlterDoc(connectorForInitiallyFullDb);
        ReplicateAndTest(localName, remoteName, push, useTor, false);

        // Reverse the direction of the replication and see if anything changes
        ReplicateAndTest(remoteName, localName, push, useTor, false);

        // Add and remove and alter a doc from the 'empty' and reverse the replication and see if it works
        ThaliTestUtilities.AlterDoc(connectorForInitiallyEmptyDb);
        ThaliTestUtilities.DeleteDoc(connectorForInitiallyEmptyDb);
        ThaliTestUtilities.GenerateDoc(connectorForInitiallyEmptyDb);
        ReplicateAndTest(remoteName, localName, push, useTor, false);

        // Add and remove and alter a doc from initially full DB and then set up a continuous replication
        ThaliTestUtilities.GenerateDoc(connectorForInitiallyFullDb);
        ThaliTestUtilities.AlterDoc(connectorForInitiallyFullDb);
        ThaliTestUtilities.DeleteDoc(connectorForInitiallyFullDb);
        ReplicationChangeListener initiallyFullToInitiallyEmptyChangeListener =
                ReplicateAndTest(localName, remoteName, push, useTor, true);

        // Add and remove docs and see if the continuous replication picks it up
        Execute execute = new Execute() {
            @Override
            public void runit() {
                ThaliTestUtilities.DeleteDoc(connectorForInitiallyFullDb);
                ThaliTestUtilities.GenerateDoc(connectorForInitiallyFullDb);
                ThaliTestUtilities.AlterDoc(connectorForInitiallyFullDb);
            }
        };
        ValidateExistingContinuousReplication(
                connectorForInitiallyFullDb, connectorForInitiallyEmptyDb, initiallyFullToInitiallyEmptyChangeListener,
                execute);

        // Set up a continuous replication in the opposite direction (but leave the other replication running)
        ReplicationChangeListener initiallyEmptyToInitiallyFullChangeListener =
                ReplicateAndTest(remoteName, localName, push, useTor, true);

        // Add and remove docs from 'target' and see if the changes safely make it to 'source'
        execute = new Execute() {
            @Override
            public void runit() {
                ThaliTestUtilities.DeleteDoc(connectorForInitiallyEmptyDb);
                ThaliTestUtilities.GenerateDoc(connectorForInitiallyEmptyDb);
                ThaliTestUtilities.GenerateDoc(connectorForInitiallyEmptyDb);
                ThaliTestUtilities.AlterDoc(connectorForInitiallyEmptyDb);
                ThaliTestUtilities.GenerateDoc(connectorForInitiallyEmptyDb);
            }
        };

        ValidateExistingContinuousReplication(
                connectorForInitiallyEmptyDb, connectorForInitiallyFullDb, initiallyEmptyToInitiallyFullChangeListener,
                execute);
    }

    /**
     * Runs a test where we set a user key in one database and then post to another.
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public void testRetrieve() throws IOException, KeyManagementException, NoSuchAlgorithmException,
            UnrecoverableEntryException, KeyStoreException, InvalidKeySpecException, InterruptedException {
        Collection<CouchDbDocument> testDocuments = ThaliTestUtilities.setUpData(
                configureRequestObjects.thaliCouchDbInstance, ThaliTestUtilities.TestDatabaseName , 1,
                MaximumTestRecords, configureRequestObjects.clientPublicKey);

        ThaliTestUtilities.validateDatabaseState(configureRequestObjects.testDatabaseConnector, testDocuments);
        runBadKeyTest(tdhDirectHost, tdhDirectPort, createClientBuilder, configureRequestObjects.serverPublicKey,
               	configureRequestObjects.clientKeyStore, passPhrase, null);

	    if(EnableTorTests)
	    {
            ThaliTestUtilities.validateDatabaseState(configureRequestObjects.torTestDatabaseConnector, testDocuments);
            runBadKeyTest(tdhOnionHost, tdhOnionPort, createClientBuilder, configureRequestObjects.serverPublicKey,
            	configureRequestObjects.clientKeyStore, passPhrase, thaliTestServer.getSocksProxy());
	    }
    }

    protected void checkIfChildClassExecutesAllTests(Class childClass) {
        if (childClass == this.getClass()) {
            throw new RuntimeException("No cheating! You have to put the real child class!");
        }

        HashSet<String> thisTestMethods = getListOfMethodsThatStartWith(this.getClass(), "test");
        HashSet<String> childTestMethods = getListOfMethodsThatStartWith(childClass, "test");

        if (thisTestMethods.size() != childTestMethods.size()) {
            throw new RuntimeException("The number of test methods in this class and in its parent class don't match! Something was missed or added!");
        }

        for(String thisMethodName : thisTestMethods) {
            if (childTestMethods.contains(thisMethodName) == false) {
                throw new RuntimeException("The test method " + thisMethodName + " was not implemented in the child test class");
            }
        }

        // This only verifies the exitence of setUp and tearDown but it doesn't actually prove that they call
        // the matching methods here.
        HashSet<String> setUpMethods = getListOfMethodsThatStartWith(childClass, "setUp");
        HashSet<String> tearDownmethods = getListOfMethodsThatStartWith(childClass, "tearDown");

        if (setUpMethods.contains("setUp") == false || tearDownmethods.contains("tearDown") == false) {
            throw new RuntimeException("Either setUp or tearDown is missing!");
        }

    }

    private HashSet<String> getListOfMethodsThatStartWith(Class classToExamine, String startsWith) {
        HashSet<String> testMethods = new HashSet<String>();
        for( Method method : classToExamine.getDeclaredMethods()) {
            if (method.getName().startsWith(startsWith)) {
                // TODO: Put in a check for the number of arguments being used, for all our current cases it's 0.
                testMethods.add(method.getName());
            }
        }
        return testMethods;
    }

    /**
     * Lets us listen in on the replication changes, we use this to know when the replicator has entered
     * certain states.
     */
    protected class ReplicationChangeListener implements Replication.ChangeListener {
        public final Semaphore callWhenSynchDone;
        public final Replication.ReplicationStatus replicationStatus;

        public ReplicationChangeListener(Replication.ReplicationStatus replicationStatus) throws InterruptedException {
            callWhenSynchDone = new Semaphore(1);
            callWhenSynchDone.acquire();
            this.replicationStatus = replicationStatus;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            if (event.getSource().getStatus() == replicationStatus) {
                callWhenSynchDone.release();
            }
        }
    }

    protected interface Execute {
        public void runit();
    }

    protected static void ValidateExistingContinuousReplication(CouchDbConnector sourceConnector,
                                                                CouchDbConnector targetConnector,
                                                                ReplicationChangeListener replicationChangeListener,
                                                                Execute execute)
            throws InterruptedException {

        if (replicationChangeListener != null) {
            // Due to https://github.com/couchbase/couchbase-lite-android-core/issues/55 we can't be sure that the semaphore
            // was cleared so we might have to clear it manually.
            if (replicationChangeListener.callWhenSynchDone.availablePermits() == 0) {
                replicationChangeListener.callWhenSynchDone.release();
            }
            replicationChangeListener.callWhenSynchDone.acquire();
        }
        execute.runit();
        ValidateReplicationCompletion(true, replicationChangeListener, sourceConnector, targetConnector);
    }

    /**
     * Either pushes from source to target or pulls from target to source depending on the value of push. Note that
     * the replication always goes from the local database.
     * @param source
     * @param target
     * @param push
     * @param continuous
     * @throws InterruptedException
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @return
     */
    protected ReplicationChangeListener ReplicateAndTest(String source, String target, boolean push, boolean useTor,
                                                         boolean continuous)
            throws InterruptedException, MalformedURLException, URISyntaxException, CouchbaseLiteException {
        HttpKeyURL targetUrl =
                new HttpKeyURL(configureRequestObjects.serverPublicKey, useTor ? tdhOnionHost : tdhDirectHost,
                        useTor ? tdhOnionPort : tdhDirectPort, target, null, null);
        ThaliReplicationCommand thaliReplicationCommand =
                new ThaliReplicationCommand.Builder()
                        .source(push ? source : targetUrl.toString())
                        .target(push ? targetUrl.toString() : source)
                        .createTarget(true)
                        .continuous(continuous)
                        .build();
        ReplicationStatus replicationStatus =
                configureRequestObjects.thaliCouchDbInstance.replicate(thaliReplicationCommand);
        if (replicationStatus.isOk() == false) {
            throw new RuntimeException("Replication failed!");
        }

        ReplicationChangeListener replicationChangeListener = null;

        if (thaliTestServer != null) {
            Manager manager = thaliTestServer.getManager();
            Database database = manager.getDatabase(source);
            URL targetHttpsUrl = new URL(targetUrl.createHttpsUrl());
            Replication replication = database.getActiveReplicator(targetHttpsUrl, push);
            replicationChangeListener =
                    new ReplicationChangeListener(
                            continuous ? Replication.ReplicationStatus.REPLICATION_IDLE :
                                    Replication.ReplicationStatus.REPLICATION_STOPPED);
            replication.addChangeListener(replicationChangeListener);
        }

        CouchDbConnector sourceTestConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(source, false);
        CouchDbConnector targetTestConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(target, false);
        return ValidateReplicationCompletion(continuous, replicationChangeListener, sourceTestConnector, targetTestConnector);
    }

    private static ReplicationChangeListener ValidateReplicationCompletion(
            boolean continuous, ReplicationChangeListener replicationChangeListener,
            CouchDbConnector sourceTestConnector, CouchDbConnector targetTestConnector) throws InterruptedException {
        // The notifications in CouchBase, especially for pull requests, are a bit half hazard. This is
        // an occupational hazard with pull requests since the long poll mechanism used makes it a guess
        // as to what things like 'idle' even mean. So for continuous requests we just try a couple of times
        // to see if the replication has finished.
        int maxRepeatCount = continuous ? 10 : 5;
        Exception lastException = null;
        boolean doWeThinkWeAreDone = replicationChangeListener == null;
        for(int repeatCount = 0; repeatCount < maxRepeatCount; ++repeatCount) {
            if (doWeThinkWeAreDone) {
                Thread.sleep(10*1000);
            }

            // TODO: doWeThinkWeAreDone is an overly complex optimization that in some theoretical cases might
            // make us run faster. We should probably remove it.
            doWeThinkWeAreDone =
                    replicationChangeListener == null ||
                            replicationChangeListener.callWhenSynchDone.tryAcquire(10, TimeUnit.SECONDS);

            try {
                ThaliTestUtilities.validateDatabaseEquality(sourceTestConnector, targetTestConnector);
                return replicationChangeListener;
            } catch (Exception e) {
                lastException = e;
            }
        }

        throw new RuntimeException("Compare failed.", lastException);
    }


    /**
     * Try to connect to a DB with a client key we know is not authorized
     * @param host
     * @param port
     * @param createClientBuilder
     * @param actualServerPublicKey
     * @param actualClientKeyStore
     * @param clientPassPhrase
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    protected static void runBadKeyTest(String host, int port, CreateClientBuilder createClientBuilder,
                                     PublicKey actualServerPublicKey, KeyStore actualClientKeyStore,
                                     char[] clientPassPhrase, Proxy proxy) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        KeyPair wrongKeys = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();

        HttpClient httpClientWithWrongServerKeyAndRightClientKey =
                createClientBuilder.CreateEktorpClient(host, port, wrongKeys.getPublic(), actualClientKeyStore,
                        clientPassPhrase, proxy);

        CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClientWithWrongServerKeyAndRightClientKey);

        try {
            CouchDbConnector couchDbConnector =
                    couchDbInstance.createConnector(ThaliTestUtilities.TestDatabaseName, true);
            throw new RuntimeException();
        } catch (Exception e) {
            ThaliTestUtilities.assertFail(e.getCause() instanceof SSLException);
        }

        KeyStore wrongClientKeyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(wrongKeys, "foo",
                        ThaliCryptoUtilities.DefaultPassPhrase);

        HttpClient httpClientWithRightServerKeyAndWrongClientKey =
                createClientBuilder.CreateEktorpClient(host, port, actualServerPublicKey, wrongClientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase, proxy);

        couchDbInstance = new StdCouchDbInstance(httpClientWithRightServerKeyAndWrongClientKey);
        try {
            CouchDbConnector couchDbConnector =
                    couchDbInstance.createConnector(ThaliTestUtilities.TestDatabaseName, true);
            throw new RuntimeException();
        } catch (DbAccessException e) {
        }
    }
}
