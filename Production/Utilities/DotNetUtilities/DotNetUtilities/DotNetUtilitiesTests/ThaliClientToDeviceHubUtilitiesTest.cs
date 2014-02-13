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
    using System.IO;

    using DotNetUtilities;

    using Microsoft.VisualStudio.TestTools.UnitTesting;

    [TestClass]
    public class ThaliClientToDeviceHubUtilitiesTest
    {
        private const string Host = "10.82.119.41";// "127.0.0.1";

        private const int Port = 9898;

        private DirectoryInfo tempDirectory;

        /// <summary>
        /// This 'test' is more bogus than usual since we aren't putting in place the right machinery to 
        /// actually launch a Thali Device Hub and then pull its key. Heck for this test I could just set up
        /// a HTTPs server and test against that. But I'm lazy. So I basically just check to see if we can
        /// get a key or not. Who knows if the key is correct, if we are reading it right, etc.
        /// I also don't test in any useful way if the client cert is actually sent or not.
        /// TODO: Fix this test so it doesn't completely suck
        /// </summary>
        [TestMethod]
        public void GetServersRootPublicKeyTest()
        {
            var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            var pkcs12Stream = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                keyPair,
                ThaliCryptoUtilities.DefaultPassPhrase);
            var cert = ThaliCryptoUtilities.GetX509Certificate(pkcs12Stream, ThaliCryptoUtilities.DefaultPassPhrase);
            var serverKey = ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(Host, Port, cert);
            Assert.IsNotNull(serverKey);
        }

        [TestMethod]
        public void ThaliWebRequestTest()
        {
            var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            var pkcs12Stream = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                keyPair,
                ThaliCryptoUtilities.DefaultPassPhrase);
            var cert = ThaliCryptoUtilities.GetX509Certificate(pkcs12Stream, ThaliCryptoUtilities.DefaultPassPhrase);
            var serverKey = ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(Host, Port, cert);
            var serverHttpKeyUri = HttpKeyUri.BuildHttpKeyUri(serverKey, Host, Port, null, null);
            var thaliWebRequest = ThaliClientToDeviceHubUtilities.CreateThaliWebRequest(serverHttpKeyUri, cert);
            thaliWebRequest.Method = "GET";
            thaliWebRequest.GetResponse().Close();
        }

        [TestMethod]
        public void ProvisionThaliClient()
        {
            var clientCert = ThaliClientToDeviceHubUtilities.ProvisionThaliClient(Host, Port, tempDirectory);
            var serverKey = ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(Host, Port, clientCert);
            var couchClient = ThaliClientToDeviceHubUtilities.GetCouchClient(serverKey, Host, Port, clientCert);
            var myPrincipalDatabase = couchClient.GetDatabase(ThaliCryptoUtilities.KeyDatabaseName);
            var keyId = BogusAuthorizeCouchDocument.GenerateRsaKeyId(new BigIntegerRSAPublicKey(clientCert));
            var clientKeyDoc = myPrincipalDatabase.GetDocument<BogusAuthorizeCouchDocument>(keyId);
        }

        [TestInitialize]
        public void Setup()
        {
            var tempDirectoryPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            tempDirectory = Directory.CreateDirectory(tempDirectoryPath);    
        }

        [TestCleanup]
        public void Teardown()
        {
            tempDirectory.Delete(true);
        }
    }
}
