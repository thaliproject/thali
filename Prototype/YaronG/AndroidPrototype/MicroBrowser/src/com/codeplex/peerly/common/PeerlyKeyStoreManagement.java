package com.codeplex.peerly.common;

import com.codeplex.peerly.org.json.JSONArray;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * For now in Peerly we assume that the trust store is kept as synchronized file in pouchDB. Since we don't have
 * a binary interface to Javascript yet (and we aren't yet using a native version of Couch) we persist the keystore
 * as a base 64 blob. We use the PKCS12 format for the trust store because it is a standard and should be supported
 * on all platforms. Of course ideally one would never, ever, EVER put a trust store on the wire but for low security
 * situations it's necessary in order to make sure that the loss of one device doesn't lose the core keys.
 * http://stackoverflow.com/questions/6340918/trust-store-vs-key-store-creating-with-keytool/6341566#6341566 good
 * explanation of the difference between a key store and a trust store.
 */
public abstract class PeerlyKeyStoreManagement {
    private final static String keyStoreFormatIdentifier = "PKCS12";
    private final static long expirationPeriodForCertsInDays = 365;
    private final static String keyTypeIdentifier = "RSA";
    private final static int keySizeInBits = 512;
    private final static String signerAlgorithm = "SHA256withRSA"; // TODO: Need to validate if that's a good choice
    private final KeyStore keyStore;

    /**
     * Just to keep things simpler we pass in the keystore to be saved rather than making the internal class keystore
     * protected.
     * @param keyStore
     */
    public abstract void SaveKeyStore(KeyStore keyStore);

    public PeerlyKeyStoreManagement(String base64EncodedKeyStore, char[] passphrase) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        InputStream keyStoreInputStream = Utilities.Base64ToInputStream(base64EncodedKeyStore);
        this.keyStore = KeyStore.getInstance(keyStoreFormatIdentifier);
        this.keyStore.load(keyStoreInputStream, passphrase);
    }

    /**
     * Creates an empty keystore secured with the supplied passphrase and then base64'd
     * @param passphrase
     */
    public static String CreateBase64KeyStore(char[] passphrase) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreFormatIdentifier);
        // The keystore has to be initialized before it can be used which apparently can be done by calling load
        // with nulls.
        keyStore.load(null, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        keyStore.store(byteArrayOutputStream, passphrase);
        return Utilities.ByteArrayOutputStreamToBase64String(byteArrayOutputStream);
    }

    /**
     * Creates a new public/private key pair and stores it under the supplied alias protected by the supplied passphrase.
     * The updated keystore is then saved.
     * @param keyAlias
     * @param passphrase
     */
    public void AddNewPublicPrivateKeyPairToKeyStore(String keyAlias, char[] passphrase) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, KeyStoreException {
        // Right now I'm focusing on large RSA keys because that seems more likely to be secure given concerns
        // about potential manipulation of curve arguments.

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
        Date endDate = new Date(System.currentTimeMillis() + (expirationPeriodForCertsInDays * 24L * 60L * 60L * 1000L));

        // Peerly security is based on keys NOT on cert values. That is we are not trying to bind a name (like a DNS
        // address) to a key. The key IS the identity. But the X509 standard requires names so we stick something
        // in.
        X500Name x500Name = new X500Name("CN=Peerly");

        SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKeyAsByteArray));

        // Note that by not specify .setProvider("BC") we are using the default provider, this is because bouncy castle as
        // previously mentioned is installed on Android but is a challenge for the applet so I'll just use the default for now.
        ContentSigner contentSigner = new JcaContentSignerBuilder(signerAlgorithm).build(keyPair.getPrivate());

        X509v1CertificateBuilder x509v1CertificateBuilder = new X509v1CertificateBuilder(x500Name, BigInteger.ONE, startDate, endDate, x500Name, subjectPublicKeyInfo);
        X509CertificateHolder x509CertificateHolder = x509v1CertificateBuilder.build(contentSigner);
        JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
        X509Certificate x509Certificate = jcaX509CertificateConverter.getCertificate(x509CertificateHolder);

        // Store the private key and the cert in the keystore
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), new Certificate[] { x509Certificate });
        keyStore.setEntry(keyAlias, privateKeyEntry, new KeyStore.PasswordProtection(passphrase));

        SaveKeyStore(keyStore);

    }

    public KeyManager[] GetKeyManagers() throws NoSuchAlgorithmException {
        return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).getKeyManagers();
    }

    /**
     * Returns the root X.509 cert for the given alias
     * @param alias
     * @return
     * @throws KeyStoreException
     * @throws CertificateEncodingException
     */
    public String GetBase64RootCert(String alias) throws KeyStoreException, CertificateEncodingException {
        Certificate certificates[] = keyStore.getCertificateChain(alias);
        return Base64.toBase64String(certificates[certificates.length - 1].getEncoded());
    }
}
