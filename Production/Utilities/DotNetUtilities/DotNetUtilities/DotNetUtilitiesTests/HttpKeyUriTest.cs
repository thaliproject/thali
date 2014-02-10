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
    using System;

    using DotNetUtilities;

    using Microsoft.VisualStudio.TestTools.UnitTesting;

    using Org.BouncyCastle.Crypto.Parameters;

    [TestClass]
    public class HttpKeyUriTest
    {
        [TestMethod]
        public void HttpKeyUriTests()
        {
            var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
            var serverPublicKey = new BigIntegerRSAPublicKey((RsaKeyParameters)keyPair.Public);
            const string Host = "foo.com";
            const int Port = 413;
            string path = "/ick";
            string query = "?ark";
            string fragment = "#bark";
            string extraValue = query + fragment;

            var httpKeyURL = HttpKeyUri.BuildHttpKeyUri(serverPublicKey, Host, Port, path, extraValue);

            // We want one we do manually just to make sure everything is o.k.
            Assert.IsTrue(Host.Equals(httpKeyURL.Host, StringComparison.Ordinal));
            Assert.IsTrue(Port == httpKeyURL.Port);
            Assert.IsTrue(serverPublicKey.Modulus.Equals(httpKeyURL.ServerPublicKey.Modulus));
            Assert.IsTrue(serverPublicKey.Exponent.Equals(httpKeyURL.ServerPublicKey.Exponent));
            Assert.IsTrue(query.Equals(httpKeyURL.Query, StringComparison.Ordinal));
            Assert.IsTrue(fragment.Equals(httpKeyURL.Fragment, StringComparison.Ordinal));
            Assert.IsTrue(httpKeyURL.PathWithoutPublicKey.Equals(path, StringComparison.Ordinal));

            string expectedURL = HttpKeyUri.HttpKeySchemeName + "://" + Host + ":" + Port + "/" +
                    HttpKeyUri.RsaKeyType + ":" + serverPublicKey.Exponent + "." + serverPublicKey.Modulus +
                    path + query + fragment;

            Assert.IsTrue(expectedURL.Equals(httpKeyURL.ToString(), StringComparison.Ordinal));

            string expectedHttpsURL = "https://" + Host + ":" + Port + path + query + fragment;
            Assert.IsTrue(expectedHttpsURL.Equals(httpKeyURL.CreateHttpsUrl(), StringComparison.Ordinal));

            // ReSharper disable once EqualExpressionComparison
            Assert.IsTrue(httpKeyURL.Equals(httpKeyURL));

            HttpKeyUri secondHttpKeyURL = HttpKeyUri.BuildHttpKeyUri(expectedURL);

            Assert.IsTrue(httpKeyURL.Equals(secondHttpKeyURL));

            HttpKeyUri thirdHttpKeyURL = HttpKeyUri.BuildHttpKeyUri(serverPublicKey, Host, Port, null, null);

            string expectedThirdURL = HttpKeyUri.HttpKeySchemeName + "://" + Host + ":" + Port + "/" +
                    HttpKeyUri.RsaKeyType + ":" + serverPublicKey.Exponent + "." + serverPublicKey.Modulus;

            Assert.IsTrue(expectedThirdURL.Equals(thirdHttpKeyURL.ToString(), StringComparison.Ordinal));
            Assert.IsTrue(HttpKeyUri.BuildHttpKeyUri(expectedThirdURL).Equals(thirdHttpKeyURL));

            path = "/ick  ?";
            query = "??????    ";
            fragment = "###???///???";
            HttpKeyUri escapedChars = HttpKeyUri.BuildHttpKeyUri(serverPublicKey, Host, Port, path, query + fragment);

            expectedHttpsURL = "https://" + Host + ":" + Port + "/ick%20%20%3F" + "??????%20%20%20%20" + "#%23%23???///???";
            Assert.IsTrue(expectedHttpsURL.Equals(escapedChars.CreateHttpsUrl(), StringComparison.Ordinal));

            path = "/ick/bick/bark/ark/mark/hark";
            httpKeyURL = HttpKeyUri.BuildHttpKeyUri(serverPublicKey, Host, Port, path, extraValue);
            Assert.IsTrue(httpKeyURL.PathWithoutPublicKey.Equals(path, StringComparison.Ordinal));

            httpKeyURL = HttpKeyUri.BuildHttpKeyUri(httpKeyURL.ToString());
            Assert.IsTrue(httpKeyURL.PathWithoutPublicKey.Equals(path, StringComparison.Ordinal));
        }
    }
}
