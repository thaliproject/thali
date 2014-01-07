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

package com.msopentech.thali.CouchDBListener;

import org.ektorp.support.CouchDbDocument;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 * Created by yarong on 12/25/13.
 */
public class BogusAuthorizeCouchDocument extends CouchDbDocument {
    public static final java.lang.String RSAKeyType = "RSAKeyType";

    private String modulus = null;
    private String exponent = null;
    private String keyType = RSAKeyType;

    public BogusAuthorizeCouchDocument() {
        this(null);
    }

    public BogusAuthorizeCouchDocument(PublicKey publicKey) {
        if (publicKey == null) {
            return;
        }

        if (publicKey instanceof RSAPublicKey == false) {
            throw new RuntimeException("Unsupported key type");
        }

        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        this.modulus = rsaPublicKey.getModulus().toString();
        this.exponent = rsaPublicKey.getPublicExponent().toString();
        this.setId(generateRsaKeyId(rsaPublicKey));
    }

    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }

    public String getModulus() { return modulus; }
    public void setModulus(String modulus) { this.modulus = modulus; }

    public String getExponent() { return exponent; }
    public void setExponent(String exponent) { this.exponent = exponent; }

    /**
     * Creates a PublicKey class from the arguments of the body.
     * @return
     */
    public PublicKey generatePublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (RSAKeyType.equals(this.keyType) == false) {
            throw new RuntimeException("Unsupported key type");
        }

        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(rsaPublicKeySpec);
    }

    @Override
    public boolean equals(Object object) {
        if ((object instanceof BogusAuthorizeCouchDocument) == false) {
            return false;
        }
        BogusAuthorizeCouchDocument compareTo = (BogusAuthorizeCouchDocument) object;
        return (this.getId().equals(compareTo.getId()) &&
                this.getRevision().equals(compareTo.getRevision()) &&
                this.getKeyType().equals(compareTo.getKeyType()) &&
                this.getModulus().equals(compareTo.getModulus()) &&
                this.getExponent().equals(compareTo.getExponent()));
    }

    public static String generateRsaKeyId(java.security.interfaces.RSAPublicKey rsaPublicKey) {
        return RSAKeyType + ":" + rsaPublicKey.getModulus().toString() + ":" + rsaPublicKey.getPublicExponent().toString();
    }
}
