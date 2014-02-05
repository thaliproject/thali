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

namespace DotNetUtilities
{
    using System;
    using System.Diagnostics;
    using System.Net;
    using System.Net.Security;
    using System.Security.Cryptography;
    using System.Security.Cryptography.X509Certificates;

    using Org.BouncyCastle.Asn1.X509;
    using Org.BouncyCastle.Crypto;
    using Org.BouncyCastle.Crypto.Generators;
    using Org.BouncyCastle.Crypto.Parameters;
    using Org.BouncyCastle.Crypto.Prng;
    using Org.BouncyCastle.Math;
    using Org.BouncyCastle.Security;
    using Org.BouncyCastle.X509;

    public static class ThaliCryptoUtilities
    {
        public const int KeySizeInBits = 2048; // TODO: Default Thali RSA Key Size, yes it should be 4096

        public const int ExpirationPeriodForCertsInDays = 365;

        public const string SignerAlgorithm = "SHA256withRSA"; // TODO: Need to change this to something more secure

        public const string DefaultPassPhrase = "Encrypting key files on a device with a password that is also stored on the device is security theater";

        public const string Pkcs12FileName = "thali.pk";

        public const string KeyDatabaseName = "thaliprincipaldatabase";

        public static readonly X509Name X500Name = new X509Name("CN=Thali");
        
        // TODO: We are not using key containers properly, this code is not secure.
        public static AsymmetricCipherKeyPair GenerateThaliAcceptablePublicPrivateKeyPair()
        {
            var keyGenerationParameters = new KeyGenerationParameters(new SecureRandom(new CryptoApiRandomGenerator()), KeySizeInBits);
            var rsaKeyPairGenerator = new RsaKeyPairGenerator();
            rsaKeyPairGenerator.Init(keyGenerationParameters);
            var keyPair = rsaKeyPairGenerator.GenerateKeyPair();
            return keyPair;
        }

        public static byte[] CreatePKCS12KeyStoreWithPublicPrivateKeyPair(AsymmetricCipherKeyPair keyPair, string passphrase)
        {
            var keyContainerName = new Guid().ToString();
            var x509V3CertificateGenerator = new X509V3CertificateGenerator();

            // The fields that Thali cares about
            x509V3CertificateGenerator.SetSignatureAlgorithm(SignerAlgorithm);
            x509V3CertificateGenerator.SetPublicKey(keyPair.Public);

            // To avoid getting an InvalidOperationExceptoin when calling generate below we have to 
            // specify a certain number of mandatory fields as explained in http://blog.differentpla.net/post/53
            // We don't actually care about these fields but Bouncy Castle does
            var serialNumber = BigInteger.ProbablePrime(120, new Random());
            x509V3CertificateGenerator.SetSerialNumber(serialNumber);
            x509V3CertificateGenerator.SetSubjectDN(X500Name);
            x509V3CertificateGenerator.SetIssuerDN(X500Name);
            x509V3CertificateGenerator.SetNotBefore(DateTime.Now.Subtract(new TimeSpan(24, 0, 0)));
            x509V3CertificateGenerator.SetNotAfter(DateTime.Now.AddDays(ExpirationPeriodForCertsInDays));

            var bouncyCastleX509Cert = x509V3CertificateGenerator.Generate(keyPair.Private);
            try
            {
                var msX509Cert = new X509Certificate2(DotNetUtilities.ToX509Certificate(bouncyCastleX509Cert))
                {
                    PrivateKey = ToRSA((RsaPrivateCrtKeyParameters)keyPair.Private, keyContainerName)
                };
                var pkcs12Store = msX509Cert.Export(X509ContentType.Pkcs12, passphrase);
                return pkcs12Store;
            }
            finally
            {
                var cspParameters = new CspParameters { KeyContainerName = keyContainerName };
                var rsaCryptoServiceProvider = new RSACryptoServiceProvider(cspParameters) { PersistKeyInCsp = false };
                rsaCryptoServiceProvider.Clear();
            }
        }

        /// <summary>
        /// Taken directly from Bouncy Castle's ToRSA function in DotNetUtilities I had to modify it based on
        /// http://stackoverflow.com/questions/16419911/cannot-export-generated-certificate-with-private-key-to-byte-array-in-net-4-0-4
        /// and more cleanly http://msdn.microsoft.com/en-us/library/system.security.cryptography.cspparameters.keycontainername%28v=vs.110%29.aspx
        /// which both say that if you don't put in some kind of key store then the private key will be marked as ephemeral and so won't
        /// be persisted on an export.
        /// </summary>
        /// <param name="privKey"></param>
        /// <param name="keyContainerName"></param>
        /// <returns></returns>
        public static RSA ToRSA(RsaPrivateCrtKeyParameters privKey, string keyContainerName)
        {
            Debug.Assert(privKey != null && string.IsNullOrWhiteSpace(keyContainerName) == false);
            RSAParameters parameters = DotNetUtilities.ToRSAParameters(privKey);

            var cspParameters = new CspParameters { KeyContainerName = keyContainerName };
            RSACryptoServiceProvider cryptoServiceProvider = new RSACryptoServiceProvider(cspParameters);
            cryptoServiceProvider.ImportParameters(parameters);
            return cryptoServiceProvider;
        }

        public static X509Certificate2 GetX509Certificate(byte[] pkcs12Contents, string password)
        {
            var cert = new X509Certificate2();
            cert.Import(pkcs12Contents, password, X509KeyStorageFlags.Exportable);
            return cert;
        }
    }
}
