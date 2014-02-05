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

namespace DotNetUtilitiesTests
{
    using System.Security.Cryptography;

    using DotNetUtilities;

    using Microsoft.VisualStudio.TestTools.UnitTesting;

    using Org.BouncyCastle.Crypto.Parameters;
    using Org.BouncyCastle.Math;

    [TestClass]
    public class ThaliCryptoUtilitiesTest
    {
        [TestMethod]
        public void TestGenerateThaliAcceptablePublicPrivateKeyPair()
        {
            var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            Assert.IsTrue(keyPair != null && keyPair.Private != null && keyPair.Public != null);

            var testPublic = (RsaKeyParameters)keyPair.Public;
            
            Assert.AreEqual(testPublic.Modulus.BitLength, ThaliCryptoUtilities.KeySizeInBits);
        }

        [TestMethod]
        public void TestKeyStoreMethods()
        {
            var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            var keyStoreBinary = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(keyPair, ThaliCryptoUtilities.DefaultPassPhrase);
            var x509cert = ThaliCryptoUtilities.GetX509Certificate(keyStoreBinary, ThaliCryptoUtilities.DefaultPassPhrase);
            var retrievedKeyParams = ((RSACryptoServiceProvider)x509cert.PrivateKey).ExportParameters(true);

            var originalBigIntegerRsaPublicKey = new BigIntegerRSAPublicKey((RsaKeyParameters)keyPair.Public);
            var retrievedBigIntegerRsaPublicKey = new BigIntegerRSAPublicKey(retrievedKeyParams);
            Assert.IsTrue(originalBigIntegerRsaPublicKey.Equals(retrievedBigIntegerRsaPublicKey));

            var originalKeyParams = (RsaPrivateCrtKeyParameters)keyPair.Private;
            Assert.IsTrue(
                originalKeyParams.DP.Equals(new BigInteger(1, retrievedKeyParams.DP))
                && originalKeyParams.DQ.Equals(new BigInteger(1, retrievedKeyParams.DQ))
                && originalKeyParams.P.Equals(new BigInteger(1, retrievedKeyParams.P))
                && originalKeyParams.Q.Equals(new BigInteger(1, retrievedKeyParams.Q)));
        }
    }
}
