package com.codeplex.peerly.common;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

// http://stackoverflow.com/questions/6340918/trust-store-vs-key-store-creating-with-keytool/6341566#6341566 good explanation
// of the difference between a key store and a trust store.
public class JsonKeyStoreManagement {
    // BKS is also supported by the Desktop and Android but only if Bouncy Castle is a registered security provider
    // which apparently requires a mountain of permissions on the desktop so I'm sticking with PKC12 which can only
    // be a keystore (e.g. it only stores public and private keys not just public keys so it can't be a trust store).
    private final static String keyStoreFormatIdentifier = "PKCS12"; // Supported by both Android and the JVM
    private final static int expirationPeriodForCertsInDays = 365;
    private final static String keyTypeIdentifier = "RSA";
    private final static int keySizeInBits = 4096;
    private final static String signerAlgorithm = "SHA256withRSA"; // TODO: Need to validate if that's a good choice
    private final KeyStore keyStore;

    public static void main(String [] args) {
        char[] passphrase = "Abcdef@#$@#4JK2k34j2lk;j4;l23kj423lkjfl;".toCharArray();
        String base64EncodedKeyStore = JsonKeyStoreManagement.CreateBase64KeyStore(passphrase);
        JsonKeyStoreManagement jsonKeyManagement = new JsonKeyStoreManagement(base64EncodedKeyStore, passphrase);
        jsonKeyManagement.AddNewPublicPrivateKeyPairToKeyStore("foo", passphrase);
    }

    public JsonKeyStoreManagement(String base64EncodedKeyStore, char[] passphrase) {
        try {
            InputStream keyStoreInputStream = Utilities.Base64ToInputStream(base64EncodedKeyStore);
            this.keyStore = KeyStore.getInstance(keyStoreFormatIdentifier);
            this.keyStore.load(keyStoreInputStream, passphrase);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.toString());
        } catch (CertificateException e) {
            throw new RuntimeException(e.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString());
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }


    /**
     * Creates an empty keystore secured with the supplied passphrase and then base64'd
     * @param passphrase - The passphrase will be cleared out by the function
     */
    public static String CreateBase64KeyStore(char[] passphrase) {
        try {
            KeyStore keyStore = KeyStore.getInstance(keyStoreFormatIdentifier);
            // The keystore has to be initialized before it can be used which apparently can be done by calling load
            // with nulls.
            keyStore.load(null, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            keyStore.store(byteArrayOutputStream, passphrase);
            return Utilities.ByteArrayOutputStreamToBase64String(byteArrayOutputStream);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.toString());
        } catch (CertificateException e) {
            throw new RuntimeException(e.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString());
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        } finally {
            Utilities.ReplaceCharsWithZeros(passphrase);
        }
    }

    /**
     * Creates a new public/private key pair and stores it under the supplied alias protected by the supplied passphrase.
     * @param keyAlias
     * @param passphrase
     */
    public void AddNewPublicPrivateKeyPairToKeyStore(String keyAlias, char[] passphrase) {
        // Right now I'm focusing on large RSA keys because that seems more likely to be secure given concerns
        // about potential manipulation of curve arguments.
        try {
            // Create the public/private key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyTypeIdentifier);
            // TODO: http://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html talks about security
            // failures in Android caused by improperly initialized RNGs. It would appear that this issue doesn't
            // apply to the latest version of Android. But obviously this is something that has to be further investigated
            // to make sure we are doing this correctly.
            keyPairGenerator.initialize(keySizeInBits, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            byte[] publicKeyAsByteArray = keyPair.getPublic().getEncoded();

            // Generate a cert for the public key
            Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
            Date endDate = new Date(System.currentTimeMillis() + expirationPeriodForCertsInDays * 24 * 60 * 60 * 1000);

            // Peerly security is based on keys NOT on cert values. That is we are not trying to bind a name (like a DNS
            // address) to a key. The key IS the identity. But the X509 standard requires names so we stick something
            // in.
            X500Name x500Name = new X500Name("CN=Peerly");

            SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKeyAsByteArray));

            ContentSigner contentSigner = new JcaContentSignerBuilder(signerAlgorithm).setProvider("BC").build(keyPair.getPrivate());

            X509v1CertificateBuilder x509v1CertificateBuilder = new X509v1CertificateBuilder(x500Name, BigInteger.ONE, startDate, endDate, x500Name, subjectPublicKeyInfo);
            X509CertificateHolder x509CertificateHolder = x509v1CertificateBuilder.build(contentSigner);
            JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
            X509Certificate x509Certificate = jcaX509CertificateConverter.getCertificate(x509CertificateHolder);

            // Store the private key and the cert in the keystore
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { x509Certificate });
            keyStore.setEntry(keyAlias, privateKeyEntry, new KeyStore.PasswordProtection(passphrase));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString());
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e.toString());
        } catch (CertificateException e) {
            throw new RuntimeException(e.toString());
        } catch (KeyStoreException e) {
            throw new RuntimeException(e.toString());
        }
    }
}
