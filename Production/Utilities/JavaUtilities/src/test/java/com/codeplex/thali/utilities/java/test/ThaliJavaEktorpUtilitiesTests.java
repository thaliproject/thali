package com.codeplex.thali.utilities.java.test;

import com.codeplex.thali.utilities.java.ThaliJavaEktorpUtilities;
import com.codeplex.thali.utilities.universal.ThaliCryptoUtilities;
import com.codeplex.thali.utilities.universal.ThaliTestEktorpClient;
import com.codeplex.thali.utilities.universal.ThaliTestUtilities;
import org.ektorp.http.HttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.*;

/**
 * Created by yarong on 11/20/13.
 */
public class ThaliJavaEktorpUtilitiesTests {
    private final boolean debugApache = true;
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
        createRetrieveTest(AndroidHost, AndroidPort);
    }

    /**
     * Runs the standard Ektorp client exercises against the server at the specified host and port
     * @param host
     * @param port
     * @throws IOException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     */
    public void createRetrieveTest(String host, int port)
            throws IOException, KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyStoreException {
        KeyPair clientKeys = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair();
        KeyStore clientKeyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(clientKeys,"foo",
                        ThaliCryptoUtilities.DefaultPassPhrase);

        org.apache.http.client.HttpClient httpClientNoServerValidation =
                ThaliJavaEktorpUtilities.getEktorpHttpKeyClientBuilder(host, port, null, clientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase).configureClient();

        PublicKey serverPublicKey =
                ThaliTestEktorpClient.getServersRootPublicKey(host, port, clientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase, httpClientNoServerValidation);

        HttpClient httpClientWithServerValidation =
                ThaliJavaEktorpUtilities.getEktorpHttpKeyClientBuilder(host, port, serverPublicKey, clientKeyStore,
                        ThaliCryptoUtilities.DefaultPassPhrase).build();

        ThaliTestEktorpClient.runRetrieveTest(httpClientWithServerValidation, clientKeys.getPublic());
    }
}
