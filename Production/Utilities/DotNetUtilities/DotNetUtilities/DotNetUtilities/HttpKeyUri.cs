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

    public class HttpKeyUri : Uri
    {
        public const string HttpKeySchemeName = "httpkey";

        public const string RsaKeyType = "rsapublickey";

        public readonly BigIntegerRSAPublicKey ServerPublicKey;

        protected HttpKeyUri(string httpKeyUrlString)
            : base(httpKeyUrlString)
        {
            if (HttpKeySchemeName.Equals(this.Scheme, StringComparison.Ordinal) == false)
            {
                throw new ArgumentException("Scheme must be" + HttpKeySchemeName + " and not " + this.Scheme);
            }

            var publicKeyAndPath = ExtractKeyAndPath(this.AbsolutePath);
            this.ServerPublicKey = GenerateServerPublicKey(publicKeyAndPath.Item1);
            this.PathWithoutPublicKey = publicKeyAndPath.Item2;
        }

        /// <summary>
        /// Gets the path for httpkey without the public key information.
        /// The underlying Uri object thinks the AbsolutePath includes the public key information. I couldn't find a good way to change that
        /// behavior and using 'new' to override it just at this level in the inheritance hierarchy seemed like begging for trouble.
        /// So instead I introduced this method to get the path without the public key.
        /// </summary>
        public string PathWithoutPublicKey { get; private set; }

        public static HttpKeyUri BuildHttpKeyUri(string httpKeyUri)
        {
            return new HttpKeyUri(httpKeyUri);
        }

        /// <summary>
        /// Creates a HttpKeyUri
        /// </summary>
        /// <param name="serverPublicKey"></param>
        /// <param name="host"></param>
        /// <param name="port"></param>
        /// <param name="absolutePath">Expected to start with a /</param>
        /// <param name="extraValue">Queries should start with ? and fragments with #</param>
        /// <returns></returns>
        public static HttpKeyUri BuildHttpKeyUri(BigIntegerRSAPublicKey serverPublicKey, string host, int port, string absolutePath, string extraValue)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(host) == false && (string.IsNullOrEmpty(absolutePath) || absolutePath[0] == '/'));
            var modulusAndExponent = serverPublicKey.GetModulusAndExponentAsString();
            string httpKeyAbsolutePath = "/" + RsaKeyType + ":" + modulusAndExponent.Item2 + "." + modulusAndExponent.Item1 + absolutePath;
            return new HttpKeyUri(new UriBuilder(HttpKeySchemeName, host, port, httpKeyAbsolutePath, extraValue).ToString());
        }

        public string CreateHttpsUrl()
        {
            return new UriBuilder("https", this.Host, this.Port, this.PathWithoutPublicKey, Query + Fragment).ToString();
        }

        /// <summary>
        /// A HttpKeyURL contains in its 'path' both the public key for the server as well as a normal HTTP path. This function
        /// just separates the public key part from the rest of the path
        /// </summary>
        /// <param name="absolutePath">The path segment of a httpkey URL</param>
        /// <returns>The first string is the public key string for the server and the second is the rest of the path</returns>
        protected static Tuple<string, string> ExtractKeyAndPath(string absolutePath)
        {
            // The first character of the path should be a / so we can skip that
            Debug.Assert(absolutePath[0] == '/');
            var preprocessedPath = absolutePath.Substring(1);
            int locationOfPathStart = preprocessedPath.IndexOf('/');
            var publicKeyServerString = locationOfPathStart == -1 ? preprocessedPath : preprocessedPath.Substring(0, locationOfPathStart);
            var restOfPath = preprocessedPath.Substring(publicKeyServerString.Length);
            return new Tuple<string, string>(publicKeyServerString, restOfPath);
        }

        /// <summary>
        /// Takes the part of a httpkey url that encodes the servers key and turns it into a RSAParameters structure.
        /// </summary>
        /// <param name="rsaKeyValue"></param>
        /// <returns></returns>
        protected static BigIntegerRSAPublicKey GenerateServerPublicKey(string rsaKeyValue)
        {
            if (string.IsNullOrWhiteSpace(rsaKeyValue)
                || rsaKeyValue.StartsWith(RsaKeyType, StringComparison.Ordinal) == false)
            {
                throw new ArgumentException();
            }

            var splitString = rsaKeyValue.Substring(RsaKeyType.Length + 1).Split('.');
            if (splitString.Length != 2)
            {
                throw new ArgumentException("rsaKeyValue must have a single dot, instead it had: " + rsaKeyValue);
            }

            return new BigIntegerRSAPublicKey(splitString[1], splitString[0]);
        }
    }
}
