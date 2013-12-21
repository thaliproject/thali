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


package com.msopentech.thali.utilities.java.test;

import com.msopentech.thali.utilities.java.JavaEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliTestEktorpClient;
import com.msopentech.thali.utilities.universal.ThaliTestUtilities;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class JavaEktorpCreateClientBuilderTest {
    private final boolean debugApache = false;
    private final String MachineHost = "127.0.0.1";
    private final String AndroidHost = MachineHost;
    private final int AndroidPort = 9898;

    @Before
    public void setup() {
        if (debugApache) {
            ThaliTestUtilities.configuringLoggingApacheClient();
        }
    }

    @Test
    public void testClient()
            throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
            IOException {
        ThaliTestEktorpClient.runRetrieveTest(AndroidHost, AndroidPort, new JavaEktorpCreateClientBuilder(), null);
    }
}
