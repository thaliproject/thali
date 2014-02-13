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
    using System.Diagnostics;
    using System.IO;

    using DotNetUtilities;

    using Newtonsoft.Json;

    /// <summary>
    /// TODO: In general this entire project is really sad. Buffers are created like crazy where streams could be
    /// used massively more efficiently. Request/responses are done serially where they could trivially be done
    /// in parallel. But we'll fix that, if it's worth fixing, once we get the end to end flow working.
    /// </summary>
    public class Program
    {
        public static void Main(string[] args)
        {
            bool synchronous;
            if (args.Length == 0)
            {
                synchronous = false;
            }
            else
            {
                // I think Google Native Host sends random stuff on the command line so if there is an argument and I
                // can't convert it to a bool then I assume it's them and use parallel.
                try
                {
                    synchronous = Convert.ToBoolean(args[0]);
                }
                catch (FormatException)
                {
                    synchronous = false;
                }
            }

            XmlHttpRequestProxy.MainLoop(synchronous);
            //foo();
            //bar();
        }

        public static void bar()
        {
            var tempDirectoryPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            var tempDirectory = Directory.CreateDirectory(tempDirectoryPath);

            var clientCert = ThaliClientToDeviceHubUtilities.GetLocalClientCertificate(tempDirectory);

            var testGetJsonString = GenerateXmlHttpRequestJsonObjectForNonExistentDatabase();

            var xmlHttpRequest = JsonConvert.DeserializeObject<XmlHttpRequest>(testGetJsonString);
            var xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            //Assert.AreEqual(xmlHttpResponse.status, 404);


            xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
            xmlHttpResponse = (XmlHttpResponse)XmlHttpRequestProxy.ProcessMessage(xmlHttpRequest, clientCert);
        }

        public static void foo()
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
                                              RedirectStandardOutput = true
                                          }
                                  };
                loopProcess.Start();

                using (var inStream = loopProcess.StandardInput)
                using (var outStream = loopProcess.StandardOutput)
                {
                    var testGetJsonString = GenerateXmlHttpRequestJsonObjectForNonExistentDatabase();
                    var xmlHttpRequest = JsonConvert.DeserializeObject<XmlHttpRequest>(testGetJsonString);
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    var stopWatch = new Stopwatch();
                    stopWatch.Start();

                    var xmlHttpResponse =
                        ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();
                    //Assert.AreEqual(404, xmlHttpResponse.status);  



                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    stopWatch.Restart();

                    ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();

                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    stopWatch.Restart();

                    ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    stopWatch.Restart();

                    ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    stopWatch.Restart();

                    ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();
                    ChromeNativeHostUtilities.SendMessage(xmlHttpRequest, inStream.BaseStream);
                    inStream.BaseStream.Flush();

                    stopWatch.Restart();

                    ChromeNativeHostUtilities.ReadNextMessage<XmlHttpResponse>(outStream.BaseStream);
                    stopWatch.Stop();
                    Debug.WriteLine("Time for message on wire to thali device hub: " + stopWatch.ElapsedMilliseconds);
                    Debug.Flush();

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

        private static string GenerateXmlHttpRequestJsonObjectForNonExistentDatabase()
        {
            var testHttpsString = "https://" + Host + ":" + Port + "/rsapublickey:0.0/" + TestDatabaseName + "/";
            return @"{""type"":""REQUEST_XMLHTTP"",""transactionId"":""thaliXMLHTTPRequestManager1"",""method"":""GET"",""url"":""" +
                testHttpsString +
@""",""headers"":{""Accept"":""application/json"",""Content-Type"":""application/json""},""requestText"":""""}";
        }

        private const string Host = "10.82.119.41";// "127.0.0.1";

        private const int Port = 9898;

        private const string TestDatabaseName = "test";
    }
}
