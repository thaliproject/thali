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
import com.msopentech.thali.CouchDBListener.GroupReplicator;
import com.msopentech.thali.utilities.universal.HttpKeyURL;
import com.msopentech.thali.utilities.universal.ThaliGroupUrl;
import com.msopentech.thali.utilities.universal.test.ThaliTestUtilities;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.support.CouchDbDocument;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class GroupReplicatorTests extends CommonListenerTestBasis {
    public static final String repManagerName = "repmanager";
    public static final String repManager2Name = "repmanager2";

    @Override
    public void tearDown() {
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(GroupReplicator.addressBookDatabaseName);
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(repManagerName);
        configureRequestObjects.thaliCouchDbInstance.deleteDatabase(repManager2Name);
        secondConfigureRequestObjects.thaliCouchDbInstance.deleteDatabase(repManager2Name);
        super.tearDown();
    }

    // Todo: Once we actually put in real tests for the group replication manager (right now they are in Javascript
    // in the HTML5 Framework) we can get rid of this silly test.
    public void testDidNotBreakReplicationManager() throws InvalidKeySpecException, NoSuchAlgorithmException,
            InterruptedException, URISyntaxException, CouchbaseLiteException, MalformedURLException {
        HttpKeyURL repManager2HttpKeyUrl = new HttpKeyURL(thaliListener.getServerPublicKey(), "127.0.0.1",
                thaliListener.getSocketStatus().getPort(), repManager2Name, null, null);
        try {
            // Create repmanager and put some docs inside it
            List<CouchDbDocument> generatedDocs =
                    ThaliTestUtilities.setUpData(configureRequestObjects.thaliCouchDbInstance, repManagerName, 1, 10);
            // Send in replication request for repmanager2
            // confirm that repmanager2 matches repmanager
            ThaliTestUtilities.ReplicateAndTest(repManagerName, repManager2Name,
                    repManager2HttpKeyUrl.toString(), true, false, true, configureRequestObjects.thaliCouchDbInstance);
            // Put a second doc inside of repmanager
            ThaliTestUtilities.GenerateDoc(
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false), generatedDocs);
            // confirm that repmanager2 matches repmanager
            CouchDbConnector sourceTestConnector =
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false);
            CouchDbConnector targetTestConnector =
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false);
            ThaliTestUtilities.ValidateReplicationCompletion(sourceTestConnector, targetTestConnector);
            // Delete replication request
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, repManager2HttpKeyUrl.toString(), true,
                    false, true, true, configureRequestObjects.thaliCouchDbInstance);
            // Put a third doc inside of repmanager
            ThaliTestUtilities.GenerateDoc(
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false), generatedDocs);
            // Check that repmanager2 DID NOT get the replication
            Thread.sleep(10*1000, 0);
            assertEquals(
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false).getAllDocIds()
                            .size(),
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false).getAllDocIds()
                            .size() + 1);
        } finally {
            // Delete replication request
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, repManager2HttpKeyUrl.toString(), true,
                    false, true, true, configureRequestObjects.thaliCouchDbInstance);
        }
    }

    public class addressBookEntry extends CouchDbDocument {
        public String name;
        public String httpKeyUrl;

        public addressBookEntry() {

        }

        public addressBookEntry(String name, String httpKeyUrl) {
            this.name = name;
            this.httpKeyUrl = httpKeyUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHttpKeyUrl() {
            return httpKeyUrl;
        }

        public void setHttpKeyUrl(String httpKeyUrl) {
            this.httpKeyUrl = httpKeyUrl;
        }
    }

    // TODO: Eventually we have to pull group all since it's nuts but for now we will use it for our September demo
    public void testGroupReplicatorAll() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException,
            InterruptedException {
        ThaliGroupUrl allGroup = new ThaliGroupUrl(thaliListener.getServerPublicKey(), "all", repManager2Name);
        try {
            // Create database repmanager and put some docs inside it
            List<CouchDbDocument> generatedDocs =
                    ThaliTestUtilities.setUpData(configureRequestObjects.thaliCouchDbInstance, repManagerName, 1, 10);
            // Send a group replication request for repmanager to repmanager2
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, allGroup.toString(), true, false, true,
                    false, configureRequestObjects.thaliCouchDbInstance);
            // Wait 10 seconds and make sure nothing is in repManager2Name
            Thread.sleep(10*1000,0);
            assertFalse(configureRequestObjects.thaliCouchDbInstance.checkIfDbExists(repManager2Name));
            // Delete replication request
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, allGroup.toString(), true, false, true,
                    true, configureRequestObjects.thaliCouchDbInstance);
            // Create addressbook database but don't put anything in it.
            // There is some bizarre behavior where sometimes Ektorp decides after checking that addressbook
            // doesn't exist that it actually does exist! I have no idea what's going on and I'm sure I'm going
            // to pay for not figuring it out. The next release of Ektorp fixes this problem all together by
            // changing how it handles existing database. But that's another story.
            // TODO: We have to figure out what's up with the addressbook already exists error, it looks like the
            // problem is us and not Ektorp. I know we could write the below as a simple create a database and
            // then create connector but I am keeping it this way so we will eventually figure out what the heck
            // is going on!
            CouchDbConnector addressBookCouchDbConnector;
            try {
                addressBookCouchDbConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(
                        GroupReplicator.addressBookDatabaseName, true);
            } catch(DbAccessException e) {
                addressBookCouchDbConnector = configureRequestObjects.thaliCouchDbInstance.createConnector(
                        GroupReplicator.addressBookDatabaseName, false);
            }
            // Send a group replication request for repmanager to repmanager2 & validate replication
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, allGroup.toString(), true, false, true,
                    false, configureRequestObjects.thaliCouchDbInstance);
            // Wait a little bit and check that repmanager2 hasn't changed
            Thread.sleep(10*1000,0);
            assertFalse(configureRequestObjects.thaliCouchDbInstance.checkIfDbExists(repManager2Name));
            // Add thaliListener localhost as member
            addressBookCouchDbConnector.create(
                    new addressBookEntry("localHostThaliListener", thaliListener.getHttpKeys().getLocalMachineIPHttpKeyURL()));
            // Replication request should still be active so see the repmanager is copied to repmanager2
            CouchDbConnector repManagerCouchDbConnector =
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false);
            CouchDbConnector repManager2CouchDbConnector =
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false);
            ThaliTestUtilities.ValidateReplicationCompletion(repManagerCouchDbConnector, repManager2CouchDbConnector);
            // Put a second record into addressbook database point at the second thali listener
            addressBookCouchDbConnector.create(
                    new addressBookEntry("torSecondThaliListener", secondThaliListener.getHttpKeys().getOnionHttpKeyURL()));
            // Put in a third record with a blank httpkeyurl to simulate what the address book app can do while it
            // waits for an onion address to resolve
            addressBookCouchDbConnector.create(new addressBookEntry("blankEntry", ""));
            // Check that repmanager and repmanager2 on secondThaliListener are the same
            CouchDbConnector repManager2OnSecondThaliListenerConnector =
                    secondConfigureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false);
            ThaliTestUtilities.ValidateReplicationCompletion(
                    repManagerCouchDbConnector, repManager2OnSecondThaliListenerConnector);
            // Insert another record into repmanager
            ThaliTestUtilities.GenerateDoc(repManagerCouchDbConnector, generatedDocs);
            // Confirm that repmanager2 are the same on both listeners
            ThaliTestUtilities.ValidateReplicationCompletion(repManagerCouchDbConnector, repManager2CouchDbConnector);
            ThaliTestUtilities.ValidateReplicationCompletion(
                    repManagerCouchDbConnector, repManager2OnSecondThaliListenerConnector);
            // Delete the replication request
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, allGroup.toString(), true, false, true,
                    true, configureRequestObjects.thaliCouchDbInstance);
            // Give the delete a little time to work its way through the system
            Thread.sleep(10*1000, 0);
            // Add another doc to repmanager
            ThaliTestUtilities.GenerateDoc(repManagerCouchDbConnector, generatedDocs);
            // Check that there is no replication
            Thread.sleep(10*1000, 0);
            assertEquals(
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false).getAllDocIds()
                            .size(),
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false).getAllDocIds()
                            .size() + 1);
            assertEquals(
                    configureRequestObjects.thaliCouchDbInstance.createConnector(repManagerName, false).getAllDocIds()
                            .size(),
                    secondConfigureRequestObjects.thaliCouchDbInstance.createConnector(repManager2Name, false).getAllDocIds()
                            .size() + 1);

        } finally {
            // Delete the replication request
            ThaliTestUtilities.buildAndExecuteReplicationRequest(repManagerName, allGroup.toString(), true, false, true,
                    true, configureRequestObjects.thaliCouchDbInstance);
        }

    }
}
