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

namespace ChromeNativeMessagingHost
{
    using System;
    using System.Collections.Concurrent;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.Net;
    using System.Runtime.InteropServices;
    using System.Security.Cryptography.X509Certificates;
    using System.Text;

    using DotNetUtilities;

    using Org.BouncyCastle.Math;

    public static class XmlHttpRequestProxy
    {
        // These two values are for bogus HTTP methods that the client can send the proxy in order to
        // get it to handle either provisioning the local client to the destination
        public const string ProvisionClientToHub = "ThaliProvisionLocalClientToHub";

        // or (see above) to provision the local hub to a remote hub
        public const string ProvisionLocalHubToRemoteHub = "ThaliProvisionRemote";

        /// <summary>
        /// We support allowing the client to specify 0 for the modulus and exponent in a HttpKeyUrl, in that case
        /// we will (insecurely as hell) look up the server's key ourselves and then cache it here because otherwise
        /// every single request will end up being a double request.
        /// TODO: This thing could theoretically grow without limit and anyway it's insecure as heck, we need to fix it. Oh
        /// and if the server should change its key for any number of good reasons this will blow up. Bad. Bad. Bad.
        /// </summary>
        private static readonly ConcurrentDictionary<Tuple<string, int>, BigIntegerRSAPublicKey> HttpKeyStore = 
            new ConcurrentDictionary<Tuple<string, int>, BigIntegerRSAPublicKey>();

        public static void MainLoop(bool synchronous)
        {
            Stream outStream = null;

            // TODO: If one of the console methods throws an exception then will it be caught by the outer catch? And if
            // there is an exception inside the inner try then don't I have to assume that the using will close the streams
            // before the outter catch can send a message? I'm not sure about the answers and don't have time to explore
            // at the moment so I'm using a double wrapping of try's to take care of it.
            try
            {
                using (var inStream = Console.OpenStandardInput())
                using (outStream = Console.OpenStandardOutput())
                {
                    try
                    {
                        ServicePointManager.DefaultConnectionLimit = 100;

                        var appdataDirectory = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                        var homeDirectory = string.Concat(appdataDirectory, @"\Google\Chrome\User Data\Thali");
                        if (!Directory.Exists(homeDirectory))
                        {
                            Directory.CreateDirectory(homeDirectory);
                        }

                        DirectoryInfo workingDirectory = new DirectoryInfo(homeDirectory);
                        
                        var clientCert = ThaliClientToDeviceHubUtilities.GetLocalClientCertificate(workingDirectory);
                        if (synchronous)
                        {
                            ChromeNativeHostUtilities.SynchronousRequestResponseMessageEngine<XmlHttpRequest>(
                                inStream,
                                outStream,
                                inMessage => ProcessMessage(inMessage, clientCert),
                                ProcessHostError);
                        }
                        else
                        {
                            ChromeNativeHostUtilities.AsynchronousRequestResponseMessageEngine<XmlHttpRequest>(
                                inStream,
                                outStream,
                                inMessage => ProcessMessage(inMessage, clientCert),
                                ProcessHostError);
                        }
                    }
                    catch (Exception e)
                    {
                        if (outStream != null)
                        {
                            ChromeNativeHostUtilities.ParallelSendMessage(ProcessHostError("oops! " + e.Message, null), outStream);
                        }                        
                    }
                }
            }
            catch (Exception e)
            {
                if (outStream != null)
                {
                    ChromeNativeHostUtilities.SendMessage(ProcessHostError("oops! " + e.Message, null), outStream);   
                }
            }
        }

        public static object ProcessMessage(XmlHttpRequest xmlHttpRequest, X509Certificate2 clientCert)
        {
            Debug.Assert(xmlHttpRequest != null && clientCert != null);
            if (xmlHttpRequest.type != XmlHttpRequest.typeValue)
            {
                return
                    ProcessHostError(
                        "The type of the incoming request was " + xmlHttpRequest.type + " and not "
                        + XmlHttpRequest.typeValue + " as required.",
                        xmlHttpRequest);
            }

            try
            {
                switch (xmlHttpRequest.method)
                {
                    case ProvisionClientToHub:
                        return ExecuteProvisionLocalClientToLocalHub(xmlHttpRequest, clientCert);
                    case ProvisionLocalHubToRemoteHub:
                        return ExecuteProvisionLocalHubToRemoteHub(xmlHttpRequest, clientCert);
                    default:
                        var stopwatch = new Stopwatch();
                        stopwatch.Start();
                        var result = ProxyRequest(xmlHttpRequest, clientCert);
                        stopwatch.Stop();            
                        Debug.WriteLine("Method: " + xmlHttpRequest.method + ", elapsed Time: " + stopwatch.ElapsedMilliseconds + ", id: " + xmlHttpRequest.transactionId);
                        return result;
                }
            }
            catch (Exception e)
            {
                return ProcessHostError("Received exception: " + e.Message, xmlHttpRequest);
            }
        }

        public static object ProcessHostError(string errorMessage, XmlHttpRequest xmlHttpRequest)
        {
            var xmlHttpResponse = new XmlHttpResponse
                                      {
                                          transactionId =
                                              xmlHttpRequest != null
                                                  ? xmlHttpRequest.transactionId
                                                  : null,
                                          status = 502,
                                          responseText = errorMessage
                                      };
            xmlHttpResponse.headers.Add("content-type", "text/plain");
            return xmlHttpResponse;
        }

        private static XmlHttpResponse ExecuteProvisionLocalHubToRemoteHub(
            XmlHttpRequest xmlHttpRequest,
            X509Certificate2 clientCert)
        {
            var remoteHubHttpKeyUri = DiscoverRootCertIfNeeded(HttpKeyUri.BuildHttpKeyUri(xmlHttpRequest.url), clientCert);

            var localHubHttpKeyUri = HttpKeyUri.BuildHttpKeyUri(xmlHttpRequest.requestText);

            ThaliClientToDeviceHubUtilities.ProvisionKeyInPrincipalDatabase(remoteHubHttpKeyUri.ServerPublicKey, remoteHubHttpKeyUri.Host, remoteHubHttpKeyUri.Port, localHubHttpKeyUri.ServerPublicKey, clientCert);

            return new XmlHttpResponse
            {
                status = 200,
                transactionId = xmlHttpRequest.transactionId,
                responseText = remoteHubHttpKeyUri.AbsoluteUri
            };
        }

        private static XmlHttpResponse ExecuteProvisionLocalClientToLocalHub(
            XmlHttpRequest xmlHttpRequest,
            X509Certificate2 clientCert)
        {
            var hubHttpKeyUri = DiscoverRootCertIfNeeded(HttpKeyUri.BuildHttpKeyUri(xmlHttpRequest.url), clientCert);

            ThaliClientToDeviceHubUtilities.ProvisionThaliClient(
                hubHttpKeyUri.ServerPublicKey,
                hubHttpKeyUri.Host,
                hubHttpKeyUri.Port,
                clientCert);

            return new XmlHttpResponse { status = 200, transactionId = xmlHttpRequest.transactionId, responseText = hubHttpKeyUri.AbsoluteUri };
        }

        private static XmlHttpResponse ProxyRequest(
            XmlHttpRequest xmlHttpRequest,
            X509Certificate2 clientCert)
        {
            var httpKeyUri = HttpKeyUri.BuildHttpKeyUri(xmlHttpRequest.url);

            HttpWebRequest webRequest = ThaliClientToDeviceHubUtilities.CreateThaliWebRequest(httpKeyUri, clientCert);

            webRequest.Method = xmlHttpRequest.method;

            // There are multiple headers that cannot be set directly via webRequest.Headers. I only catch
            // two below that seem of some reasonable use.
            foreach (var headerNameValue in xmlHttpRequest.headers)
            {
                if (headerNameValue.Key.Equals("Accept", StringComparison.OrdinalIgnoreCase))
                {
                    webRequest.Accept = headerNameValue.Value;
                }
                else if (headerNameValue.Key.Equals("Content-Type", StringComparison.OrdinalIgnoreCase))
                {
                    webRequest.ContentType = headerNameValue.Value;
                }
                else
                {
                    webRequest.Headers.Add(headerNameValue.Key, headerNameValue.Value);
                }
            }

            if (string.IsNullOrWhiteSpace(xmlHttpRequest.requestText))
            {
                return ProcessResponse(xmlHttpRequest.transactionId, webRequest);
            }

            var bodyAsBytes = Encoding.UTF8.GetBytes(xmlHttpRequest.requestText);
            webRequest.GetRequestStream().Write(bodyAsBytes, 0, bodyAsBytes.Count());

            var response = ProcessResponse(xmlHttpRequest.transactionId, webRequest);

            return response;
        }

        /// <summary>
        /// TODO: This whole method is just wrong, what happens if the server at the address changes its key?!?!?!
        /// TODO: Once we have a real discovery framework this whole 0.0 mechanism needs to go away.
        /// </summary>
        /// <param name="httpKeyUri"></param>
        /// <param name="clientCert"></param>
        /// <returns></returns>
        private static HttpKeyUri DiscoverRootCertIfNeeded(HttpKeyUri httpKeyUri, X509Certificate2 clientCert)
        {
            if (httpKeyUri.ServerPublicKey.Exponent.Equals(BigInteger.Zero)
                && httpKeyUri.ServerPublicKey.Modulus.Equals(BigInteger.Zero))
            {
                var host = httpKeyUri.Host;
                var port = httpKeyUri.Port;
                var hostPortTuple = new Tuple<string, int>(host, port);

                var serverPublicKey = HttpKeyStore.GetOrAdd(
                    hostPortTuple,
                    keyTuple => ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(host, port, clientCert));

                var serverHttpKey = HttpKeyUri.BuildHttpKeyUri(
                    serverPublicKey,
                    host,
                    port,
                    httpKeyUri.PathWithoutPublicKey,
                    httpKeyUri.Query);
                return serverHttpKey;
            }

            return httpKeyUri;
        }

        private static XmlHttpResponse ProcessResponse(string transactionId, HttpWebRequest webRequest)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(transactionId) == false && webRequest != null);
            HttpWebResponse webResponse;

            var stopWatch = new Stopwatch();
            stopWatch.Start();

            try
            {
                webResponse = (HttpWebResponse)webRequest.GetResponse();
            }
            catch (WebException webException)
            {
                webResponse = (HttpWebResponse)webException.Response;
            }

            stopWatch.Stop();
            Debug.WriteLine("webRequest only - elapsed Time: " + stopWatch.ElapsedMilliseconds + ", id: " + transactionId);

            var xmlHttpResponse = new XmlHttpResponse
                                      {
                                          transactionId = transactionId,
                                          status = (int)webResponse.StatusCode
                                      };

            foreach (var headerName in webResponse.Headers.AllKeys)
            {
                var lowerHeaderName = headerName.ToLowerInvariant();
                if (xmlHttpResponse.headers.ContainsKey(lowerHeaderName))
                {
                    xmlHttpResponse.headers[lowerHeaderName] = xmlHttpResponse.headers[lowerHeaderName] + ","
                                                          + webResponse.Headers[headerName];
                }
                else
                {
                    xmlHttpResponse.headers[lowerHeaderName] = webResponse.Headers[headerName];
                }
            }

            xmlHttpResponse.responseText = string.Empty;
            var readBuffer = new byte[102400];
            using (var memoryStream = new MemoryStream())
            {
                var responseStream = webResponse.GetResponseStream();

                if (responseStream != null)
                {
                    int bytesRead = responseStream.Read(readBuffer, 0, readBuffer.Count());

                    while (bytesRead > 0)
                    {
                        memoryStream.Write(readBuffer, 0, bytesRead);
                        bytesRead = responseStream.Read(readBuffer, 0, readBuffer.Count());
                    }

                    if (memoryStream.Length > 0)
                    {
                        xmlHttpResponse.responseText = Encoding.UTF8.GetString(memoryStream.ToArray());
                    }
                }
            }

            return xmlHttpResponse;
        }
    }
}
