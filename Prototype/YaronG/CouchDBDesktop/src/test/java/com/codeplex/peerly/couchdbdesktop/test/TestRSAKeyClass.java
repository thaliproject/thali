package com.codeplex.peerly.couchdbdesktop.test;

import org.ektorp.support.CouchDbDocument;

/**
 * Used to store the key of the client that should have access to the database. Unfortunately the Jackson
 * parser as configured in Couch doesn't tell the difference between an integer and a BigInteger so when
 * passing a really big integer we get an out of bounds exception. So we encode things as strings instead.
 */
public class TestRSAKeyClass extends CouchDbDocument {
    private String modulus;
    private String exponent;

    public TestRSAKeyClass() {
        this(null, null);
    }

    public TestRSAKeyClass(String modulus, String exponent) {
        super();
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public String getModulus() { return modulus; }
    public void setModulus(String modulus) { this.modulus = modulus; }

    public String getExponent() { return exponent; }
    public void setExponent(String exponent) { this.exponent = exponent; }

    @Override
    public boolean equals(Object object) {
        if ((object instanceof  TestRSAKeyClass) == false) {
            return false;
        }
        TestRSAKeyClass compareTo = (TestRSAKeyClass) object;
        return (this.getId().equals(compareTo.getId()) &&
                this.getRevision().equals(compareTo.getRevision()) &&
                this.getModulus().equals(compareTo.getModulus()) &&
                this.getExponent().equals(compareTo.getExponent()));
    }
}
