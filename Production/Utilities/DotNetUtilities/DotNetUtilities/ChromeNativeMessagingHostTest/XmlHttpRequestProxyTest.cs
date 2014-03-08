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

namespace ChromeNativeMessagingHostTest
{
    using System.Diagnostics;
    using System.IO;

    using ChromeNativeMessagingHost;

    using DotNetUtilities;

    using Microsoft.VisualStudio.TestTools.UnitTesting;

    using Newtonsoft.Json;

    [TestClass]
    public class XmlHttpRequestProxyTest
    {
        private const string Host = "127.0.0.1";

        private const int Port = 9898;

        private const string TestDatabaseName = "test";

        private DirectoryInfo tempDirectory;

        private DirectoryInfo tempDirectoryForSetup;

        [TestMethod]
        public void ProcessMessageTest()
        {
            var clientCert = ThaliClientToDeviceHubUtilities.GetLocalClientCertificate(tempDirectory);

            var testGetJsonString = GenerateXmlHttpRequestJsonObjectForNonExistentDatabase();

            var xmlHttpRequest = JsonConvert.DeserializeObject<XmlHttpRequest>(testGetJsonString);
            var xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            Assert.AreEqual(xmlHttpResponse.status, 404);
        }

        [TestMethod]
        public void MainLoopTest()
        {
            this.MainLoopTestBody(true);    
            this.MainLoopTestBody(false);
        }


        public void MainLoopTestBody(bool synchronous)
        {
            Process loopProcess = null;
            try
            {
                loopProcess = new Process
                                  {
                                      StartInfo =
                                          {
                                              FileName = "ChromeNativeMessagingHost.exe",
                                              UseShellExecute = false,
                                              RedirectStandardInput = true,
                                              RedirectStandardOutput = true,
                                              Arguments = synchronous.ToString()
                                          }
                                  };
                loopProcess.Start();

                using (var inStream = loopProcess.StandardInput)
                using (var outStream = loopProcess.StandardOutput)
                {
                    var testGetJsonString = GenerateXmlHttpRequestJsonObjectForNonExistentDatabase();
                    var xmlHttpRequest = JsonConvert.DeserializeObject<XmlHttpRequest>(testGetJsonString);
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();
                    var xmlHttpResponse =
                        ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    Assert.AreEqual(404, xmlHttpResponse.status);
                    xmlHttpResponse = ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    Assert.AreEqual(404, xmlHttpResponse.status);
                    xmlHttpResponse = ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    Assert.AreEqual(404, xmlHttpResponse.status);  
                }
            }
            finally
            {
                if (loopProcess != null)
                {
                    loopProcess.Kill();
                }
            }
        }

        [TestInitialize]
        public void Setup()
        {
            var tempDirectoryForSetupPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            tempDirectoryForSetup = Directory.CreateDirectory(tempDirectoryForSetupPath);
            var clientCert = ThaliClientToDeviceHubUtilities.GetLocalClientCertificate(tempDirectoryForSetup);
            var serverPublicKey = ThaliClientToDeviceHubUtilities.GetServersRootPublicKey(Host, Port, clientCert);
            ThaliClientToDeviceHubUtilities.ProvisionThaliClient(serverPublicKey, Host, Port, clientCert);
            var couchClient = ThaliClientToDeviceHubUtilities.GetCouchClient(serverPublicKey, Host, Port, clientCert);

            var response = couchClient.DeleteDatabase(TestDatabaseName);

            var tempDirectoryPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            tempDirectory = Directory.CreateDirectory(tempDirectoryPath);
        }

        [TestCleanup]
        public void Teardown()
        {
            tempDirectory.Delete(true);
            tempDirectoryForSetup.Delete(true);
        }

        private static string GenerateXmlHttpRequestJsonObjectForNonExistentDatabase()
        {
            var testHttpsString = "https://" + Host + ":" + Port + "/rsapublickey:0.0/" + TestDatabaseName + "/";
            return @"{""type"":""REQUEST_XMLHTTP"",""transactionId"":""thaliXMLHTTPRequestManager1"",""method"":""GET"",""url"":""" +
                testHttpsString +
@""",""headers"":{""Accept"":""application/json"",""Content-Type"":""application/json""},""requestText"":""""}";
        }
    }
}
