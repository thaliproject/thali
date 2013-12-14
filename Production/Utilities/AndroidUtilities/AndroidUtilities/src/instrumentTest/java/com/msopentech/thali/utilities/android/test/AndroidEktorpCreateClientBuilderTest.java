package com.msopentech.thali.utilities.android.test;

import android.test.AndroidTestCase;
import com.msopentech.thali.utilities.android.AndroidEktorpCreateClientBuilder;
import com.msopentech.thali.utilities.universal.ThaliTestEktorpClient;
import com.msopentech.thali.utilities.universal.ThaliTestUtilities;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * Created by yarong on 11/26/13.
 */
public class AndroidEktorpCreateClientBuilderTest extends AndroidTestCase {
    private final String MachineHost = "127.0.0.1";

    public void testClient() throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, IOException, InterruptedException {
        ThaliTestUtilities.configuringLoggingApacheClient();

        ThaliTestServer thaliTestServer = new ThaliTestServer();
        thaliTestServer.startServer(getContext().getFilesDir());

        int port = thaliTestServer.getSocketStatus().getPort();
        ThaliTestEktorpClient.runRetrieveTest(
                MachineHost, port, new AndroidEktorpCreateClientBuilder(), thaliTestServer.getServerPublicKey());
    }
}
