package com.msopentech.thali.utilities.universal.test;

import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliPublicKeyComparer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by yarong on 11/20/13.
 */
public class ThaliPublicKeyComparerTests {

    private KeyPair generateEllpticCurve() throws NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = new KeyPairGeneratorSpi.ECDH();
        return keyPairGenerator.generateKeyPair();

    }

    @Test
    public void testRsaPublicKeyComparer() throws Exception {
        PublicKey rsaKey1 = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair().getPublic();
        PublicKey rsaKey2 = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair().getPublic();
        PublicKey ecdhKey1 = generateEllpticCurve().getPublic();

        try {
            new ThaliPublicKeyComparer(ecdhKey1);
            assertFalse(true);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }

        ThaliPublicKeyComparer thaliPublicKeyComparer = new ThaliPublicKeyComparer(rsaKey1);

        assertFalse(thaliPublicKeyComparer.KeysEqual(rsaKey2));
        assertFalse(thaliPublicKeyComparer.KeysEqual(ecdhKey1));
        assertTrue(thaliPublicKeyComparer.KeysEqual(rsaKey1));
    }

}
