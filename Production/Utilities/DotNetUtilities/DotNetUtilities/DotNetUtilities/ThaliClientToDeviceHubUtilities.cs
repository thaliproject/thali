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
    using System.IO;
    using System.Linq;
    using System.Net;
    using System.Net.Security;
    using System.Security.Cryptography;
    using System.Security.Cryptography.X509Certificates;

    using LoveSeat;
    using LoveSeat.Interfaces;

    public static class ThaliClientToDeviceHubUtilities
    {
        private const int MinimumTCPPortValue = 0;
        private const int MaximumTCPPortValue = 65535;

        private const string HttpsScheme = "https";

        /// <summary>
        /// Connects to a server and returns its root public key value.
        /// </summary>
        /// <param name="host">Server's host, an IP or DNS address</param>
        /// <param name="port">Server's port</param>
        /// <param name="clientCertificate">A, possibly null, cert to use to authenticate the client</param>
        /// <returns>Returns the server key if it can be retrieved, otherwise throw an exception</returns>
        public static BigIntegerRSAPublicKey GetServersRootPublicKey(string host, int port, X509Certificate2 clientCertificate)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(host) == false && port >= MinimumTCPPortValue && port <= MaximumTCPPortValue);
            var serverUri = new UriBuilder(HttpsScheme, host, port).Uri;
            var httpWebRequest = WebRequest.CreateHttp(serverUri);
            BigIntegerRSAPublicKey serverRsaPublicKey = null;
            httpWebRequest.ServerCertificateValidationCallback = (sender, certificate, chain, sslPolicyErrors) =>
                {
                    serverRsaPublicKey = ThaliServerCertificateValidationCallback(certificate, chain, sslPolicyErrors);
                    return true;
                };
            if (clientCertificate != null)
            {
                httpWebRequest.ClientCertificates.Add(clientCertificate);    
            }

            httpWebRequest.Method = "GET";
            httpWebRequest.GetResponse().Close();
            return serverRsaPublicKey;
        }

        public static HttpWebRequest CreateThaliWebRequest(HttpKeyUri httpKeyUri, X509Certificate2 clientCertificate)
        {
            Debug.Assert(httpKeyUri != null && clientCertificate != null);
            var expectedServerRsaKey = httpKeyUri.ServerPublicKey;
            var httpWebRequest = WebRequest.CreateHttp(httpKeyUri.CreateHttpsUrl());
            httpWebRequest.ServerCertificateValidationCallback =
                ServerCertificateValidationCallbackGenerator(expectedServerRsaKey);
            httpWebRequest.ClientCertificates.Add(clientCertificate);

            return httpWebRequest;
        }

        /// <summary>
        /// By default Thali Apps (at least those using this utility) keep their PKCS12 file in a directory, this function
        /// takes that directory and either reads the file into a cert or it creates the public/private key pair, writes the
        /// file and then reads in the cert.
        /// </summary>
        /// <param name="directoryInfo"></param>
        /// <returns></returns>
        public static X509Certificate2 GetLocalClientCertificate(DirectoryInfo directoryInfo)
        {
            Debug.Assert(directoryInfo != null && directoryInfo.Exists);
            var pkcFileInfo = new FileInfo(Path.Combine(directoryInfo.FullName, ThaliCryptoUtilities.Pkcs12FileName));
            byte[] pkcs12Stream = null;
            if (pkcFileInfo.Exists == false)
            {
                var keyPair = ThaliCryptoUtilities.GenerateThaliAcceptablePublicPrivateKeyPair();
                pkcs12Stream = ThaliCryptoUtilities.CreatePKCS12KeyStoreWithPublicPrivateKeyPair(
                    keyPair,
                    ThaliCryptoUtilities.DefaultPassPhrase);
                File.WriteAllBytes(pkcFileInfo.FullName, pkcs12Stream);
            }

            if (pkcs12Stream == null)
            {
                pkcs12Stream = File.ReadAllBytes(pkcFileInfo.FullName);
            }

            var cert = ThaliCryptoUtilities.GetX509Certificate(pkcs12Stream, ThaliCryptoUtilities.DefaultPassPhrase);
            return cert;
        }

        public static CouchClient GetCouchClient(BigIntegerRSAPublicKey serverPublicKey, string host, int port, X509Certificate2 clientCert)
        {
            var configWebRequest = new ThaliConfigWebRequest(serverPublicKey, clientCert);
            return new CouchClient(
                host, port, null, null, true, AuthenticationType.Cookie, configWebRequest);
        }

        /// <summary>
        /// Finds and or creates the local client PKCS12 public/private key store in the specified directory and then
        /// provisions the key in the identified server's principal database. Also returns the client's key, this is 
        /// useful for calls like setting up WebRequest.
        /// </summary>
        /// <param name="host"></param>
        /// <param name="port"></param>
        /// <param name="directoryInfo"></param>
        /// <returns></returns>
        public static X509Certificate2 ProvisionThaliClient(string host, int port, DirectoryInfo directoryInfo)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(host) == false && port > 0 && directoryInfo != null && directoryInfo.Exists);
            var clientCert = GetLocalClientCertificate(directoryInfo);
            var serverKey = GetServersRootPublicKey(host, port, clientCert);
            ProvisionThaliClient(serverKey, host, port, clientCert);
            return clientCert;
        }

        public static void ProvisionThaliClient(BigIntegerRSAPublicKey serverPublicKey, string host, int port, X509Certificate2 clientCert)
        {
            var clientPublicKey = new BigIntegerRSAPublicKey(clientCert);

            var aclDatabaseEntity = new BogusAuthorizeCouchDocument(clientPublicKey);

            var couchClient = GetCouchClient(serverPublicKey, host, port, clientCert);
            var principalDatabase = couchClient.GetDatabase(ThaliCryptoUtilities.KeyDatabaseName);
            var getResult =
                principalDatabase.GetDocument(
                    BogusAuthorizeCouchDocument.GenerateRsaKeyId(clientPublicKey));
            if (getResult == null)
            {
                var createResult = principalDatabase.CreateDocument(aclDatabaseEntity);

                if (createResult.StatusCode < 200 || createResult.StatusCode > 299)
                {
                    throw new ApplicationException("Could not successfully put client's credentials in ACL Store: " + createResult.StatusCode);
                }    
            }
        }

        private static BigIntegerRSAPublicKey ThaliServerCertificateValidationCallback(
            X509Certificate certificate,
            X509Chain chain,
            SslPolicyErrors sslPolicyErrors)
        {
            if (certificate == null || (sslPolicyErrors & SslPolicyErrors.RemoteCertificateNotAvailable) != 0)
            {
                throw new ApplicationException();
            }

            if ((sslPolicyErrors & SslPolicyErrors.RemoteCertificateChainErrors) != 0)
            {
                const X509ChainStatusFlags AcceptableCertChainErrors = X509ChainStatusFlags.UntrustedRoot
                                                                       | X509ChainStatusFlags.RevocationStatusUnknown
                                                                       | X509ChainStatusFlags.OfflineRevocation 
                                                                       | X509ChainStatusFlags.NoError;
                if (
                    chain.ChainStatus.Any(
                        chainStatus => (chainStatus.Status | AcceptableCertChainErrors) != AcceptableCertChainErrors))
                {
                    throw new ApplicationException();
                }
            }

            // TODO: Actually prove that the last entry in the chain is the root
            var x509Certv2 = chain.ChainElements[chain.ChainElements.Count - 1].Certificate;
            var serverPublicKeyParameters =
                        ((RSACryptoServiceProvider)x509Certv2.PublicKey.Key).ExportParameters(false);
            var serverPresentedRsaKey = new BigIntegerRSAPublicKey(serverPublicKeyParameters);
            return serverPresentedRsaKey;
        }

        /// <summary>
        /// Generates a server certificate validator for web requests involving Thali servers
        /// </summary>
        /// <param name="expectedServerRsaKey"></param>
        /// <returns></returns>
        private static RemoteCertificateValidationCallback ServerCertificateValidationCallbackGenerator(
            BigIntegerRSAPublicKey expectedServerRsaKey)
        {
            return (sender, certificate, chain, sslPolicyErrors) =>
            {
                var serverPresentedRsaKey = ThaliServerCertificateValidationCallback(
                                      certificate,
                                      chain,
                                      sslPolicyErrors);

                return serverPresentedRsaKey.Equals(expectedServerRsaKey);
            };
        }

        private class ThaliConfigWebRequest : IConfigWebRequest
        {
            private readonly X509Certificate2 clientCert;

            private readonly BigIntegerRSAPublicKey serverKey;

            public ThaliConfigWebRequest(BigIntegerRSAPublicKey serverKey, X509Certificate2 clientCert)
            {
                Debug.Assert(clientCert != null);
                this.serverKey = serverKey;
                this.clientCert = clientCert;
            }

            public void ConfigWebRequest(HttpWebRequest webRequest)
            {
                // There is a bug in TJWS used by the Java Thali Device Hub that doesn't properly support expect: 100-continues
                webRequest.ServicePoint.Expect100Continue = false;

                webRequest.ServerCertificateValidationCallback =
                    ServerCertificateValidationCallbackGenerator(serverKey);
                webRequest.ClientCertificates.Add(clientCert);
            }
        }
    }
}
