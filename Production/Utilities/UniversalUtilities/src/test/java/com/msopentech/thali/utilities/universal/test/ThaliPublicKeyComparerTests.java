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

import com.msopentech.thali.utilities.universal.ThaliCryptoUtilities;
import com.msopentech.thali.utilities.universal.ThaliPublicKeyComparer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThaliPublicKeyComparerTests {

    private KeyPair generateEllpticCurve() throws NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = new KeyPairGeneratorSpi.ECDH();
        return keyPairGenerator.generateKeyPair();

    }

    @Test
    public void testRsaPublicKeyComparer() throws Exception {
        PublicKey rsaKey1 = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair().getPublic();
        PublicKey rsaKey2 = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair().getPublic();
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
