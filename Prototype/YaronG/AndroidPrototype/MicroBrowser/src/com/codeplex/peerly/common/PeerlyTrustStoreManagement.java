package com.codeplex.peerly.common;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 10/3/13
 * Time: 11:37 AM
 * To change this template use File | Settings | File Templates.
 */


import com.codeplex.peerly.org.json.JSONArray;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * The trust store just holds X509 certs that we want to validate against (or chain up to). So X509 is our real
 * trust store format. But Java needs the certs placed in a container it understands. So we use this class to
 * create that container.
 */
public class PeerlyTrustStoreManagement {
    public final static String rootCertAlias = "root";
    private final static String javaKeyStoreFormatIdentifier = "JKS";
    private final static String androidKeyStoreFormatIdentifier = "BKS";
    private final static String certificateFactoryType = "X.509";

    /**
     * Creates a trust manager for user with SSL to validate the identity of the server one is connecting to.
     * @param x509RootCert
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static TrustManager[] GetTrustManagers(String x509RootCert) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance(certificateFactoryType);
        KeyStore keyStore = GenerateKeyStoreForTrustStore();
        // The keystore has to be initialized before it can be used which apparently can be done by calling load
        // with nulls.
        keyStore.load(null, null);

        InputStream certInputStream = Utilities.Base64ToInputStream(x509RootCert);
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

        KeyStore.TrustedCertificateEntry trustedCertificateEntry = new KeyStore.TrustedCertificateEntry(x509Certificate);
        keyStore.setEntry(rootCertAlias, trustedCertificateEntry, null);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory.getTrustManagers();
    }

    /**
     * The only keystore type that is natively supported by both Java and Android is PKCS12 which unfortunately does not
     * support entries of type KeyStore.TrustedCertificateEntry because PKC12 is about moving both public and private
     * keys. Near as I can tell the only way to store TrustedCertificateEntry is either JKS in Java or BKS in Android.
     * Now yes, we could support Bouncy Castle (the B in BKS) on Java and in fact we do include their jar. But installing
     * Bouncy Castle as a trusted provider requires a mess of permissions that I just don't want to deal with. This
     * might not prove to be a big deal in practice but for now working about the JKS/BKS mess is easy so I'm choosing
     * that way out. Keep in mind that the trust store is temporary, just used for SSL purposes. The persistance format
     * for the public keys used to put in the contents of the trust store is X.509 which is an actual standard. So we
     * really don't care what temporary store Java is using.
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
