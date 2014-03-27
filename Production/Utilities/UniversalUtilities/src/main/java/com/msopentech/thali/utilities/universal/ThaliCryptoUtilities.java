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

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * Created by yarong on 11/12/13.
 */
public class ThaliCryptoUtilities {
    public final static String ThaliKeyAlias = "thaliKeyAlias";
    public final static String PrivateKeyHolderFormat = "PKCS12";
    public final static char[] DefaultPassPhrase = "Encrypting key files on a device with a password that is also stored on the device is security theater".toCharArray();
    public final static String KeyTypeIdentifier = "RSA";
    public final static int KeySizeInBits = 2048;
    public final static String SignerAlgorithm = "SHA256withRSA"; // TODO: Need to validate if that's a good choice
    public final static long ExpirationPeriodForCertsInDays = 365;
    public final static String X500Name = "CN=Thali";
    private static final String KeystoreFileName = "com.msopentech.thali.name.keystore";
    private static Logger logger = LoggerFactory.getLogger(ThaliCryptoUtilities.class);

    /**
     * Retrieves the Thali related keystore from the specified directory.
     * @param filesDir
     * @return
     */
    public static File getThaliKeyStoreFileObject(File filesDir) {
        return new File(filesDir, KeystoreFileName).getAbsoluteFile();
    }

    /**
     * Returns null if there are any problems with the keystore, it's cert or the keys it contains. Generally if this
     * returns null then the only thing to do is to delete the keystore (if it even exists) and start over. Note however
     * that right now we treat cert expiration as a failure condition. In the long run that doesn't make sense but
     * this is not final code and it's actually good to freak out with any keys generated with this generation of code.
     * They shouldn't last long enough to expire.
     * @param filesDir
     * @return
     */
    public static KeyStore validateThaliKeyStore(File filesDir) {
        assert filesDir != null && filesDir.exists();
        File keyStoreFile = getThaliKeyStoreFileObject(filesDir);
        if (keyStoreFile.exists() == false) {
            return null;
        }

        FileInputStream keyStoreFileStream = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(PrivateKeyHolderFormat);
            keyStoreFileStream = new FileInputStream(keyStoreFile);
            keyStore.load(keyStoreFileStream, DefaultPassPhrase);
            KeyStore.Entry thaliKeystoreEntry = keyStore.getEntry(ThaliKeyAlias, new KeyStore.PasswordProtection(DefaultPassPhrase));

            if (thaliKeystoreEntry == null) {
                logger.debug("Could not find key in keystore under alias " + ThaliKeyAlias);
                return null;
            }

            if ((thaliKeystoreEntry instanceof KeyStore.PrivateKeyEntry) == false) {
                logger.debug("Entry is not a PrivateKeyEntry");
                return null;
            }

            KeyStore.PrivateKeyEntry privateThaliKeystoreEntry = (KeyStore.PrivateKeyEntry) thaliKeystoreEntry;
            Certificate[] certificates = privateThaliKeystoreEntry.getCertificateChain();

            if (certificates == null) {
                logger.debug("No certs in cert chain.");
                return null;
            }

            if (certificates.length != 1) {
                logger.debug("More than one cert in chain, someday we will support that but not right now. Actual length was " + certificates.length );
                return null;
            }

            if ((certificates[0] instanceof X509Certificate) == false) {
                logger.debug("Cert is not a X509Cert!");
                return null;
            }

            X509Certificate x509Certificate = (X509Certificate) certificates[0];
            x509Certificate.checkValidity();

            // We don't check the cert name because we just don't care, it doesn't matter for Thali

            if ((certificates[0].getPublicKey() instanceof RSAPublicKey) == false) {
                logger.debug("Public key is not a RSA Public Key!");
                return null;
            }

            RSAPublicKey rsaPublicKey = (RSAPublicKey) certificates[0].getPublicKey();

            if (rsaPublicKey.getModulus().bitLength() < KeySizeInBits) {
                logger.debug("Public key size is less than required minimum, required size is " + KeySizeInBits + ", actual size is " + rsaPublicKey.getModulus().bitLength());
                return null;
            }

            return keyStore;
        } catch (FileNotFoundException e) {
            logger.debug("Could not get a stream from a keyStoreFile we had previously validated existed.", e);
            return null;
        } catch (KeyStoreException e) {
            logger.debug("Could not create a keystore of type " + PrivateKeyHolderFormat, e);
            return null;
        } catch (CertificateExpiredException e) {
            logger.debug("Failure on checkValidity", e);
            return null;
        } catch (CertificateNotYetValidException e) {
            logger.debug("Failure on checkValidity", e);
            return null;
        } catch (CertificateException e) {
            logger.debug("Failure on keyStore.load", e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            logger.debug("Failure on keyStore.load", e);
            return null;
        } catch (IOException e) {
            logger.debug("Failure on keyStore.load", e);
            return null;
        } catch (UnrecoverableEntryException e) {
            logger.debug("Failure on keyStore.getEntry", e);
            return null;
        } finally {
            if (keyStoreFileStream != null) {
                try {
                    keyStoreFileStream.close();
                } catch (IOException e) {
                    logger.debug("Attempt to close keyStoreFileStream failed", e);
                }
            }
        }
    }

    /**
     * Creates a new keystore file with a validate Thali public/private key pair and returns the KeyStore object.
     * @param filesDir
     * @return
     */
    public static KeyStore createNewThaliKeyInKeyStore(File filesDir) {
        File keyStoreFile = getThaliKeyStoreFileObject(filesDir);

        if (keyStoreFile.exists()) {
            throw new RuntimeException("A keystore already exists!");
        }

        KeyStore keyStore =
                ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                        ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair(), ThaliKeyAlias, ThaliCryptoUtilities.DefaultPassPhrase);

        FileOutputStream fileOutputStream = null;
        try {
            // Yes this can swallow exceptions (if you got an exception inside this try and then the finally has an exception, but given what I'm doing here I don't care.
            try {
                fileOutputStream =  new FileOutputStream(keyStoreFile);
                keyStore.store(fileOutputStream, ThaliCryptoUtilities.DefaultPassPhrase);
            } catch (Exception e) {
                logger.error("oops", e);
                throw e;
            } finally {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        } catch (Exception e) {
            logger.error("Ooops", e);
            throw new RuntimeException(e.getMessage(), e);
        }

        return keyStore;
    }


    /**
     * Generates a public/private key pair that meets Thali's security requirements
     * @return
     */
    public static KeyPair GenerateThaliAcceptablePublicPrivateKeyPair() {
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KeyTypeIdentifier);
            // TODO: http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html talks about security
            // failures in Android caused by improperly initialized RNGs. It would appear that this issue doesn't
            // apply to the latest version of Android. But obviously this is something that has to be further investigated
            // to make sure we are doing this correctly.
            keyPairGenerator.initialize(KeySizeInBits, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    /**
     * Creates a PKCS12 keystore and puts into it the submitted public/private key pair under the submitted
     * Key Alias using the submitted passphrase to 'secure' the file.
     *
     * Right now we only generate large RSA keys because I'm paranoid that the curves used in
     * Elliptic Curve crypto may have been designed by folks for whom security was not the paramount
     * concern. Once this issue is put to rest I would expect to switch to Elliptic Curve because
     * it is considered (with appropriate curves) to be more secure and is certainly faster.
     * @param keyPair
     * @param keyAlias
     * @param passphrase
     * @return
     */
    public static KeyStore CreatePKCS12KeyStoreWithPublicPrivateKeyPair(KeyPair keyPair, String keyAlias, char[] passphrase) {
        try {
            byte[] publicKeyAsByteArray = keyPair.getPublic().getEncoded();

            // Generate a cert for the public key
            Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
            Date endDate = new Date(System.currentTimeMillis() + (ExpirationPeriodForCertsInDays * 24L * 60L * 60L * 1000L));

            // Thali security is based on keys NOT on cert values. That is we are not trying to bind a name (like a DNS
            // address) to a key. The key IS the identity. But the X509 standard requires names so we stick something
            // in.
            X500Name x500Name = new X500Name(X500Name);

            SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKeyAsByteArray));

            // Note that by not specify .setProvider("BC") we are using the default provider, this is because bouncy castle as
            // previously mentioned is installed on Android but is a challenge for the applet so I'll just use the default for now.
            ContentSigner contentSigner = new JcaContentSignerBuilder(SignerAlgorithm).build(keyPair.getPrivate());

            X509v1CertificateBuilder x509v1CertificateBuilder = new X509v1CertificateBuilder(x500Name, BigInteger.ONE, startDate, endDate, x500Name, subjectPublicKeyInfo);
            X509CertificateHolder x509CertificateHolder = x509v1CertificateBuilder.build(contentSigner);
            JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
            X509Certificate x509Certificate =    jcaX509CertificateConverter.getCertificate(x509CertificateHolder);

            // Store the private key and the cert in the keystore
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[]{x509Certificate});

            KeyStore keyStore = KeyStore.getInstance(PrivateKeyHolderFormat);
            // Keystore has to be initialized before being used
            keyStore.load(null, null);

            keyStore.setEntry(keyAlias, privateKeyEntry, new KeyStore.PasswordProtection(passphrase));

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
