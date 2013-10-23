package com.codeplex.peerly.common;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All we really care about when a client connects is if their cert is 'proper'. Can they prove they own it and if it's
 * chained does the chain work properly? We will map their identity to something useful later on. Therefore this
 * Trust Manager will accept any 'valid' cert.
 */
public class DelayedTrustManager implements X509TrustManager {
    public final static String rootCertAlias = "root";
    private final static String javaKeyStoreFormatIdentifier = "JKS";
    private final static String androidKeyStoreFormatIdentifier = "BKS";

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        try {
            // TODO: The built in validation check will check if the presented cert is expired, but why would we care?
            // Put another way what's more likely that the certificate is really expired (but the key validated!!!) or
            // there is a clock skew problem?
            GetTrustManager(x509Certificates).checkClientTrusted(x509Certificates, s);
            // TODO: Test if Android supports Java 7 multi-catch and if switching Intellij to Java 7 will bring us
            // any other grief with Android.
        } catch (KeyStoreException e) {
            Logger.getLogger(DelayedTrustManager.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e.toString());
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(DelayedTrustManager.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e.toString());
        } catch (IOException e) {
            Logger.getLogger(DelayedTrustManager.class.getName()).log(Level.SEVERE, null, e);
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        throw new RuntimeException("Not implemented");
    }

    private static X509TrustManager GetTrustManager(X509Certificate[] x509Certificates) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = GenerateKeyStoreForTrustStore();
        // The keystore has to be initialized before it can be used which apparently can be done by calling load
        // with nulls.
        keyStore.load(null, null);

        for (int i = 0; i < x509Certificates.length; ++i) {
            KeyStore.TrustedCertificateEntry trustedCertificateEntry = new KeyStore.TrustedCertificateEntry(x509Certificates[i]);
            keyStore.setEntry(rootCertAlias, trustedCertificateEntry, null);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
    }

    /**
     * The only keystore type that is natively supported by both Java and Android is PKCS12 which unfortunately does not
     * support entries of type KeyStore.TrustedCertificateEntry because PKC12 is about moving both public and private
     * keys. Near as I can tell the only way to store TrustedCertificateEntry is either JKS in Java or BKS in Android.
     * Now yes, we could support Bouncy Castle (the B in BKS) on Java and in fact we do include their jar. But installing
     * Bouncy Castle as a trusted provider requires a mess of permissions that I just don't want to deal with. This
     * might not prove to be a big deal in practice but for now working about the JKS/BKS mess is easy so I'm choosing
     * that way out. Keep in mind that the trust store is temporary, just used for SSL purposes. The persistence format
     * for the public keys used to put in the contents of the trust store is X.509 which is an actual standard. So we
     * really don't care what temporary store Java is using.
     *
     * @return
     */
    private static KeyStore GenerateKeyStoreForTrustStore() throws KeyStoreException {
        KeyStore keyStore;
        try {
            return KeyStore.getInstance(javaKeyStoreFormatIdentifier);
        } catch (KeyStoreException e) {
            return KeyStore.getInstance(androidKeyStoreFormatIdentifier);
        }

    }
}
