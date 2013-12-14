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

/**
 * Created by yarong on 11/20/13.
 */
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
