package com.codeplex.thali.utilities.test;

import com.codeplex.thali.utilities.ThaliCryptoUtilities;
import junit.framework.TestCase;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

/**
 * Created by yarong on 11/13/13.
 */
public class ThaliCryptoUtilitiesTest extends TestCase {
    public void testRsaPublicKeyComparer() throws Exception {
        RSAPublicKey key1 = (RSAPublicKey) ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair().getPublic();
        RSAPublicKey key2 = (RSAPublicKey) ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair().getPublic();

        assertFalse(ThaliCryptoUtilities.RsaPublicKeyComparer(key1, key2));
        assertFalse(ThaliCryptoUtilities.RsaPublicKeyComparer(key2, key1));
        assertTrue(ThaliCryptoUtilities.RsaPublicKeyComparer(key1, key1));
        assertTrue(ThaliCryptoUtilities.RsaPublicKeyComparer(key2, key2));
    }

    public void testGeneratePeerlyAcceptablePublicPrivateKeyPair() throws Exception {
        // These tests are just to make sure we are passing in the right parameters
        KeyPair keyPair = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair();
        assertTrue(keyPair != null && keyPair.getPrivate() != null & keyPair.getPublic() != null);

        assertEquals(keyPair.getPublic().getAlgorithm(), ThaliCryptoUtilities.KeyTypeIdentifier);
        assertTrue(keyPair.getPublic() instanceof RSAPublicKey);

        assertEquals(((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength(), ThaliCryptoUtilities.KeySizeInBits);
    }

    public void testCreatePKCS12KeyStoreWithPublicPrivateKeyPair() throws Exception {
        KeyPair keyPair = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair();
        String keyAlias = "foo";
        KeyStore keyStore = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(keyPair, keyAlias, ThaliCryptoUtilities.DefaultPassPhrase);
        List<String> aliases = Collections.list(keyStore.aliases());
        assertTrue(aliases.size() == 1);
        assertEquals(aliases.get(0), keyAlias);
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(ThaliCryptoUtilities.DefaultPassPhrase);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, protectionParameter);
        privateKeyEntry.getCertificate().verify(keyPair.getPublic());
        assertEquals(privateKeyEntry.getCertificateChain().length, 1);
    }
}
