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
    /// <summary>
    /// TODO: In general this entire project is really sad. Buffers are created like crazy where streams could be
    /// used massively more efficiently. Request/responses are done serially where they could trivially be done
    /// in parallel. But we'll fix that, if it's worth fixing, once we get the end to end flow working.
    /// </summary>
    public class Program
    {
        public static void Main(string[] args)
        {
            XmlHttpRequestProxy.MainLoop();
        }
    }
}
