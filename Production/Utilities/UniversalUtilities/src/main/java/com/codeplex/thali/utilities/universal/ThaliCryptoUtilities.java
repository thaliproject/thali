package com.codeplex.thali.utilities.universal;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Created by yarong on 11/12/13.
 */
public class ThaliCryptoUtilities {
    public final static String PrivateKeyHolderFormat = "PKCS12";
    public final static char[] DefaultPassPhrase = "Encrypting key files on a device with a password that is also stored on the device is security theater".toCharArray();
    public final static String KeyTypeIdentifier = "RSA";
    public final static int KeySizeInBits = 2048;
    public final static String SignerAlgorithm = "SHA256withRSA"; // TODO: Need to validate if that's a good choice
    public final static long ExpirationPeriodForCertsInDays = 365;

    /**
     * Generates a public/private key pair that meets Peerly's security requirements
     * @return
     */
    public static KeyPair GeneratePeerlyAcceptablePublicPrivateKeyPair() {
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
     * it is considere (with appropriate curves) to be more secure and is certainly faster.
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

            // Peerly security is based on keys NOT on cert values. That is we are not trying to bind a name (like a DNS
            // address) to a key. The key IS the identity. But the X509 standard requires names so we stick something
            // in.
            X500Name x500Name = new X500Name("CN=Peerly");

            SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKeyAsByteArray));

            // Note that by not specify .setProvider("BC") we are using the default provider, this is because bouncy castle as
            // previously mentioned is installed on Android but is a challenge for the applet so I'll just use the default for now.
            ContentSigner contentSigner = new JcaContentSignerBuilder(SignerAlgorithm).build(keyPair.getPrivate());

            X509v1CertificateBuilder x509v1CertificateBuilder = new X509v1CertificateBuilder(x500Name, BigInteger.ONE, startDate, endDate, x500Name, subjectPublicKeyInfo);
            X509CertificateHolder x509CertificateHolder = x509v1CertificateBuilder.build(contentSigner);
            JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
            X509Certificate x509Certificate = jcaX509CertificateConverter.getCertificate(x509CertificateHolder);

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
