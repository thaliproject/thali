package com.codeplex.thali.utilities.universal;

import org.ektorp.support.CouchDbDocument;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 * Used to store the key of the client that should have access to the database. Unfortunately the Jackson
 * parser as configured in Couch doesn't tell the difference between an integer and a BigInteger so when
 * passing a really big integer we get an out of bounds exception. So we encode things as strings instead.
 */
public class CouchDBDocumentKeyClassForTests extends CouchDbDocument {
    public static final String RSAKeyType = "RSAKeyType";

    private String modulus = null;
    private String exponent = null;
    private String keyType = RSAKeyType;

    public CouchDBDocumentKeyClassForTests() {
        this(null);
    }

    public CouchDBDocumentKeyClassForTests(PublicKey publicKey) {
        if (publicKey == null) {
            return;
        }

        if (publicKey instanceof RSAPublicKey == false) {
            throw new RuntimeException("Unsupported key type");
        }

        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        this.modulus = rsaPublicKey.getModulus().toString();
        this.exponent = rsaPublicKey.getPublicExponent().toString();
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
        if ((object instanceof CouchDBDocumentKeyClassForTests) == false) {
            return false;
        }
        CouchDBDocumentKeyClassForTests compareTo = (CouchDBDocumentKeyClassForTests) object;
        return (this.getId().equals(compareTo.getId()) &&
                this.getRevision().equals(compareTo.getRevision()) &&
                this.getKeyType().equals(compareTo.getKeyType()) &&
                this.getModulus().equals(compareTo.getModulus()) &&
                this.getExponent().equals(compareTo.getExponent()));
    }
}
