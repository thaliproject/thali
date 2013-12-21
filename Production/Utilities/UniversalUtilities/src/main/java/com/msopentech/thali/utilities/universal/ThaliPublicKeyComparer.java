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

package com.msopentech.thali.utilities.universal;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Centralizes the logic for comparing keys and by extension also what key types we support, this lets us pass
 * PublicKey as the type in most places and only worry about binding here.
 */
public class ThaliPublicKeyComparer {
    private final PublicKey publicKey;

    private boolean supportedKeyType(PublicKey publicKey) {
        return publicKey instanceof RSAPublicKey;
    }

    public static boolean RsaPublicKeyComparer(RSAPublicKey key1, RSAPublicKey key2) {
        return key1.getPublicExponent().compareTo(key2.getPublicExponent()) == 0 && key1.getModulus().compareTo(key2.getModulus()) == 0;
    }

    public ThaliPublicKeyComparer(PublicKey publicKey) {
        if (supportedKeyType(publicKey) == false) {
            throw new RuntimeException("Unsupported key type");
        }

        this.publicKey = publicKey;
    }

    public boolean KeysEqual(PublicKey otherPublicKey) {
        if (supportedKeyType(otherPublicKey) == false) {
            return false;
        }

        return RsaPublicKeyComparer((RSAPublicKey)otherPublicKey, (RSAPublicKey)publicKey);
    }
}
