package com.msopentech.thali.utilities.universal.test;

import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ThaliCryptoUtilitiesTest {
    @Test
    public void testGeneratePeerlyAcceptablePublicPrivateKeyPair() throws Exception {
        // These tests are just to make sure we are passing in the right parameters
        KeyPair keyPair = ThaliCryptoUtilities.GeneratePeerlyAcceptablePublicPrivateKeyPair();
        assertTrue(keyPair != null && keyPair.getPrivate() != null & keyPair.getPublic() != null);

        assertEquals(keyPair.getPublic().getAlgorithm(), ThaliCryptoUtilities.KeyTypeIdentifier);
        assertTrue(keyPair.getPublic() instanceof RSAPublicKey);

        assertEquals(((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength(), ThaliCryptoUtilities.KeySizeInBits);
    }

    @Test
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
