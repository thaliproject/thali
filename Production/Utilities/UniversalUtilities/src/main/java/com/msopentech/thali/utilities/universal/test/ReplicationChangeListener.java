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

import com.couchbase.lite.replicator.Replication;

import java.util.concurrent.Semaphore;

/**
 * Lets us listen in on the replication changes, we use this to know when the replicator has entered
 * certain states.
 */
class ReplicationChangeListener implements Replication.ChangeListener {
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
