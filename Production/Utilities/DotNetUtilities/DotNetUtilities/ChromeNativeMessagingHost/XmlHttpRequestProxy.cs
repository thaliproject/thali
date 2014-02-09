using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ChromeNativeMessagingHost
{
    using System.Diagnostics;
    using System.IO;
    using System.Net;
    using System.Security.Cryptography.X509Certificates;

    using DotNetUtilities;

    using Org.BouncyCastle.Math;

    public static class XmlHttpRequestProxy
    {
        private static X509Certificate2 clientCert;

        /// <summary>
        /// We support allowing the client to specify 0 for the modulus and exponent in a HttpKeyUrl, in that case
        /// we will (insecurely as hell) look up the server's key ourselves and then cache it here because otherwise
        /// every single request will end up being a double request.
        /// TODO: This thing could theoretically grow without limit and anyway it's insecure as heck, we need to fix it. Oh
        /// and if the server should change its key for any number of good reasons this will blow up. Bad. Bad. Bad.
        /// </summary>
        private static readonly Dictionary<Tuple<string, int>, HttpKeyUri> HttpKeyStore = 
            new Dictionary<Tuple<string, int>, HttpKeyUri>();

        /// <summary>
        /// When we provision the client's identity at a Thali Device Hub we record the address and the associated
        /// server's key in this list.
        /// TODO: This assumes there is just one client identity, which in a real Thali system wouldn't be true. Fix.
        /// </summary>
        private static readonly Dictionary<Tuple<string, int>, BigIntegerRSAPublicKey> ProvisionedList =
            new Dictionary<Tuple<string, int>, BigIntegerRSAPublicKey>();

        public static void MainLoop()
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
                        var workingDirectory = new DirectoryInfo(Environment.CurrentDirectory);
                        clientCert = ThaliClientToDeviceHubUtilities.GetLocalClientCertificate(workingDirectory);
                        ChromeNativeHostUtilities.SynchronousRequestResponseMessageEngine<XmlHttpRequest>(
                            inStream,
                            outStream,
                            ProcessMessage,
                            ProcessHostError);
                    }
                    catch (Exception e)
                    {
                        if (outStream != null)
                        {
                            ChromeNativeHostUtilities.SendMessage(ProcessHostError("oops! " + e.Message, null), outStream);
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

        public static object ProcessMessage(XmlHttpRequest xmlHttpRequest)
        {
            Debug.Assert(xmlHttpRequest != null);
            if (xmlHttpRequest.type != XmlHttpRequest.typeValue)
            {
                throw new ApplicationException("The type of the incoming request was " + xmlHttpRequest.type + 
                    " and not " + XmlHttpRequest.typeValue + " as required.");
            }

            // Pouch gets unhappy if you give it a httpkey URL, so instead we give it a https
            lskjfas;
            lkjf;
            lkasdjf;
            lkasdjf;
            lkasdjf;
            lksajdf;lkjsad
            var httpKeyUri = HttpKeyUri.BuildHttpKeyUri(xmlHttpRequest.url);

            httpKeyUri = DiscoverRootCertIfNeeded(httpKeyUri);

            var hostPortTuple = new Tuple<string, int>(httpKeyUri.Host, httpKeyUri.Port);

            if (ProvisionedList.ContainsKey(hostPortTuple) == false ||
                httpKeyUri.ServerPublicKey.Equals(ProvisionedList[hostPortTuple]) == false)
            {
                ThaliClientToDeviceHubUtilities.ProvisionThaliClient(httpKeyUri, clientCert);
                ProvisionedList[hostPortTuple] = httpKeyUri.ServerPublicKey;
            }

            var webRequest = ThaliClientToDeviceHubUtilities.CreateThaliWebRequest(httpKeyUri, clientCert);
            webRequest.Method = xmlHttpRequest.method;

            foreach (var headerNameValue in xmlHttpRequest.headers)
            {
                webRequest.Headers.Add(headerNameValue.Key, headerNameValue.Value);    
            }

            if (string.IsNullOrWhiteSpace(xmlHttpRequest.requestText) == false)
            {
                var bodyAsBytes = Encoding.UTF8.GetBytes(xmlHttpRequest.requestText);
                webRequest.GetRequestStream().Write(bodyAsBytes, 0, bodyAsBytes.Count());
            }

            var xmlHttpResponse = ProcessResponse(xmlHttpRequest.transactionId, webRequest);

            return xmlHttpResponse;
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
                                          headers = null,
                                          responseText = errorMessage
                                      };
            return xmlHttpResponse;
        }

        private static HttpKeyUri DiscoverRootCertIfNeeded(HttpKeyUri httpKeyUri)
        {
            if (httpKeyUri.ServerPublicKey.Exponent.Equals(BigInteger.Zero)
                && httpKeyUri.ServerPublicKey.Modulus.Equals(BigInteger.Zero))
            {
                var host = httpKeyUri.Host;
                var port = httpKeyUri.Port;
                var hostPortTuple = new Tuple<string, int>(host, port);

                if (HttpKeyStore.ContainsKey(hostPortTuple))
                {
                    httpKeyUri = HttpKeyStore[hostPortTuple];
                }
                else
                {
                    var serversRootPublicKey = ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(host, port, clientCert);
                    httpKeyUri = HttpKeyUri.BuildHttpKeyUri(
                        serversRootPublicKey,
                        host,
                        port,
                        httpKeyUri.AbsolutePath,
                        httpKeyUri.Query);
                }
            }
            return httpKeyUri;
        }

        private static XmlHttpResponse ProcessResponse(string transactionId, HttpWebRequest webRequest)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(transactionId) == false && webRequest != null);
            HttpWebResponse webResponse;

            try
            {
                webResponse = (HttpWebResponse)webRequest.GetResponse();
            }
            catch (WebException webException)
            {
                webResponse = (HttpWebResponse)webException.Response;
            }

            var xmlHttpResponse = new XmlHttpResponse
                                      {
                                          transactionId = transactionId,
                                          status = (int)webResponse.StatusCode
                                      };

            foreach (var headerName in webResponse.Headers.AllKeys)
            {
                if (xmlHttpResponse.headers.ContainsKey(headerName))
                {
                    xmlHttpResponse.headers[headerName] = xmlHttpResponse.headers[headerName] + ","
                                                          + webResponse.Headers[headerName];
                }
                else
                {
                    xmlHttpResponse.headers[headerName] = webResponse.Headers[headerName];
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
