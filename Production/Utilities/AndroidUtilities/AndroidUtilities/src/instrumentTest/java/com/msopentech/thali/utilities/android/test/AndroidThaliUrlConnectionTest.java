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
import com.msopentech.thali.CouchDBListener.AndroidThaliListener;
import com.msopentech.thali.CouchDBListener.ThaliListener;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliTestUrlConnection;
import com.msopentech.thali.utilities.universal.ThaliTestUtilities;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

/**
 * Created by yarong on 1/7/14.
 */
public class AndroidThaliUrlConnectionTest extends AndroidTestCase {
    public void testThaliUrlConnection() throws InterruptedException, UnrecoverableEntryException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException {
        ThaliTestUtilities.configuringLoggingApacheClient();

        AndroidThaliListener thaliTestServer = new AndroidThaliListener();
        File filesDir = getContext().getFilesDir();
        File keyStore = ThaliCryptoUtilities.getThaliKeyStoreFileObject(filesDir);

        // We want to start with a clean state
        if (keyStore.exists()) {
            keyStore.delete();
        }

        // We use a random port (e.g. port 0) both because it's good hygiene and because it keeps us from conflicting
        // with the 'real' Thali Device Hub if it's running.
        thaliTestServer.startServer(getContext().getFilesDir(), 0);

        int port = thaliTestServer.getSocketStatus().getPort();

        ThaliTestUrlConnection.TestThaliUrlConnection(ThaliListener.DefaultThaliDeviceHubAddress, port, ThaliCryptoUtilities.DefaultPassPhrase, new AndroidEktorpCreateClientBuilder(), getContext().getFilesDir());
    }

}
