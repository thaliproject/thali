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

package com.msopentech.thali.utilities.test;

import com.couchbase.lite.CouchbaseLiteException;
import com.msopentech.thali.CouchDBListener.HttpKeyTypes;
import com.msopentech.thali.CouchDBListener.ReplicationManager;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.local.utilities.UtilitiesTestCase;
import com.msopentech.thali.relay.RelayWebServer;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.test.ConfigureRequestObjects;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;
import org.apache.commons.lang3.time.StopWatch;
import org.ektorp.CouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;

/**
 * These tests are to help us measure our network perf. Right now it can take anywhere from 15 to 30 seconds to replicate
 * a simple text record. We need to prove what the problem is, not just guess. We use these tests for two different
 * purposes and as result they can be configured in two different ways.
 *
 * We use them for automated testing with a set of check values. In that case noTorHttpListenerKey will be set to null
 * in UtilitiesTestCase and cause us to create a listener just for this test
 *
 *
 */
public class NetworkPerfTests extends UtilitiesTestCase {
    public static final Logger LOGGER = LoggerFactory.getLogger(NetworkPerfTests.class);
    public static final String proxyHost = "127.0.0.1";
    public static Proxy torSocksProxy;
    public static ConfigureRequestObjects configureRequestObjects;
    public static RelayWebServer relayWebServer;
    public static final String relayHost = "127.0.0.1";
    public static final int relayPort = 23428;
    public static String localName;
    public static String remoteName;
    public static String targetNoTorReplicationUrl;
    public static String targetTorReplicationUrl;
    public static ThaliListener perfThaliListener;

    /**
     * This is a poor man's substitute for Junit 4's parameterized tests (I think, I haven't use them). But since
     * Android doesn't really support Junit 4 (at least not integrated with IntelliJ, there is a project to run
     * Junit4 on Android) I'll just use this hack.
     */
    protected abstract class MinMedianMax {
        public MinMedianMax(String description) throws Throwable {
            this(description, 10);
        }

        public MinMedianMax(String description, int numberOfRepeats) throws Throwable {
            assertTrue(numberOfRepeats > 0);
            long testTimes[] = new long[numberOfRepeats];
            for(int i = 0; i < numberOfRepeats; ++i) {
                try {
                    setUp();
                    testTimes[i] = runOneTest(description);
                } finally {
                    tearDown();
                }
            }
            logTestResults(description, testTimes);
        }

        public void setUp() throws Throwable {
        }

        public void tearDown() throws Throwable {
        }

        public abstract void singleTestInstance() throws Throwable;

        private long runOneTest(String description) throws Throwable {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                singleTestInstance();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            stopWatch.stop();
            LOGGER.info("Test run for " + description + " took " + stopWatch.getTime());
            return stopWatch.getTime();
        }

        private void logTestResults(String description, long testTimes[]) {
            Arrays.sort(testTimes);
            long median = testTimes.length % 2 == 0 ?
                    (long)(((double)testTimes[testTimes.length / 2] + (double)testTimes[testTimes.length/2 - 1])/2) :
                    testTimes[testTimes.length/2];
            LOGGER.info(String.format("%s - %s/%s/%s ms", description, testTimes[0], median,
                    testTimes[testTimes.length - 1]));
        }
    }

    @Override
    public void setUp() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, IOException, InterruptedException {
        if (perfListenerHttpKeyTypes == null) {
            perfThaliListener = getStartedListener("automaticPerfListener");
            perfListenerHttpKeyTypes = perfThaliListener.getHttpKeys();
        }
        noTorHttpListenerKey = new HttpKeyURL(perfListenerHttpKeyTypes.getLocalMachineIPHttpKeyURL());
        torHttpListenerKey = new HttpKeyURL(perfListenerHttpKeyTypes.getOnionHttpKeyURL());
        proxyPort = Integer.parseInt(perfListenerHttpKeyTypes.getSocksOnionProxyPort());

        torSocksProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));

        configureRequestObjects = new ConfigureRequestObjects(noTorHttpListenerKey.getHost(),
                noTorHttpListenerKey.getPort(),
                torHttpListenerKey.getHost(), torHttpListenerKey.getPort(), ThaliCryptoUtilities.DefaultPassPhrase,
                getCreateClientBuilder(), getNewRandomCouchBaseContext(), null, torSocksProxy);

        HttpKeyTypes httpKeyTypes = new HttpKeyTypes(noTorHttpListenerKey, torHttpListenerKey, proxyPort);
        relayWebServer = new RelayWebServer(getCreateClientBuilder(),
                getNewRandomCouchBaseContext().getFilesDir(), httpKeyTypes, relayHost, relayPort);
        relayWebServer.start();

        CouchDbConnector replicationDbConnector =
                configureRequestObjects.thaliCouchDbInstance.createConnector(ReplicationManager.replicationDatabaseName,
                        false);
        for(String docId : replicationDbConnector.getAllDocIds()) {
            String currentRevision = replicationDbConnector.getCurrentRevision(docId);
            replicationDbConnector.delete(docId, currentRevision);
        }

        localName = configureRequestObjects.testDatabaseConnector.getDatabaseName();
        remoteName = configureRequestObjects.replicationDatabaseConnector.getDatabaseName();
        targetNoTorReplicationUrl = noTorHttpListenerKey + remoteName;
        targetTorReplicationUrl = torHttpListenerKey + remoteName;
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(localName);
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(remoteName);
        configureRequestObjects.testDatabaseConnector.createDatabaseIfNotExists();
        configureRequestObjects.replicationDatabaseConnector.createDatabaseIfNotExists();
    }

    @Override
    public void tearDown() {
        relayWebServer.stop();
    }

    public void testClientToTDHPerf() throws Throwable {
        final String baseNoTorTdhUrl = String.format("https://%s:%s/", noTorHttpListenerKey.getHost(),
                noTorHttpListenerKey.getPort());

        new MinMedianMax("Elapsed time for GET test to TDH, no TOR") {
            @Override
            public void singleTestInstance() throws Throwable {
                assertEquals(
                        configureRequestObjects.thaliCouchDbInstance.getConnection().get(baseNoTorTdhUrl).getCode(),
                        200);
            }
        };

        final String baseTorTdhUrl = String.format("https://%s:%s/", torHttpListenerKey.getHost(),
                torHttpListenerKey.getPort());

        new MinMedianMax("Elapsed time for GET test to TDH, with TOR") {
            @Override
            public void singleTestInstance() throws Throwable {
                assertEquals(
                        configureRequestObjects.torThaliCouchDbInstance.getConnection().get(baseTorTdhUrl).getCode(),
                        200);
            }
        };
    }

    public void testClientToRelayWebServer() throws Throwable {
        final String baseRelayUrl = String.format("http://%s:%s/", relayHost, relayPort);

        new MinMedianMax("Elapsed time for GET test from Client to Relay") {
            @Override
            public void singleTestInstance() throws Throwable {
                RestTestMethods.testGet(baseRelayUrl + "_relayutility/localhttpkeys", null, 200, null);
            }
        };

        new MinMedianMax("Elapsed time for GET test from Client to Relay to TDH, No TOR") {
            @Override
            public void singleTestInstance() throws Throwable {
                RestTestMethods.testGet(baseRelayUrl, null, 200, null);
            }
        };

        new MinMedianMax("Elapsed time for GET test from Client to Relay to TDH, with TOR") {
            @Override
            public void singleTestInstance() throws Throwable {
                RestTestMethods.testGet(
                        baseRelayUrl + "_relayutility/translateonion?" + torHttpListenerKey.getHost() +
                                ":9898", null, 200, null);
            }
        };
    }

    public void testDirectReplication() throws Throwable {
        for(final boolean push : new boolean[] {true, false}) {
            for(final String targetUrl : new String[] {targetNoTorReplicationUrl, targetTorReplicationUrl}) {
                for(final boolean continuous : new boolean[] {false, true}) {
                    for(final int docCount : new int[] { 1 }) {
                        new MinMedianMax("Elapsed time for " + (continuous ? "continuous" : "one-time" ) +
                                " replication, " +
                                (push ? "push" : "pull") + " replication, doc count = " + docCount + ", " +
                                (targetUrl.equals(targetTorReplicationUrl) ? "with" : "no") + " tor") {
                            @Override
                            public void setUp() throws Throwable {
                                configureRequestObjects.thaliCouchDbInstance.deleteDatabase(localName);
                                configureRequestObjects.thaliCouchDbInstance.deleteDatabase(remoteName);
                                configureRequestObjects.testDatabaseConnector.createDatabaseIfNotExists();
                                configureRequestObjects.replicationDatabaseConnector.createDatabaseIfNotExists();
                                ThaliTestUtilities.setUpData(configureRequestObjects.thaliCouchDbInstance,
                                        push ? localName : remoteName, docCount, docCount);
                            }

                            @Override
                            public void singleTestInstance() throws Throwable {
                                ThaliTestUtilities.ReplicateAndTest(localName, remoteName, targetUrl, push, continuous,
                                        false, configureRequestObjects.thaliCouchDbInstance);

                                if (continuous) {
                                    ThaliTestUtilities.GenerateDoc(push ? configureRequestObjects.testDatabaseConnector
                                            : configureRequestObjects.replicationDatabaseConnector);

                                    ThaliTestUtilities.ValidateReplicationCompletion(
                                            configureRequestObjects.testDatabaseConnector,
                                            configureRequestObjects.replicationDatabaseConnector);
                                }
                            }
                        };

                    }

                }
            }
        }
    }
}
