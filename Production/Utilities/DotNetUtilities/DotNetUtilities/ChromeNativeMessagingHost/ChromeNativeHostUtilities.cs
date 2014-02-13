namespace ChromeNativeMessagingHost
{
    using System;
    using System.Diagnostics;
    using System.IO;
    using System.Linq;
    using System.Text;
    using System.Threading;

    using Newtonsoft.Json;

    public static class ChromeNativeHostUtilities
    {
        private const int NativeMessagingHeaderLengthInBytes = 4;

        /// <summary>
        /// TODO: This should obviously be parallel given the scenarios we normally use it in.
        /// </summary>
        /// <typeparam name="InMessageType">The object to which the incoming message will be serialized from JSON</typeparam>
        /// <param name="inStream"></param>
        /// <param name="outStream"></param>
        /// <param name="serialProcessorFunc">Is given an input object of type InMessageType and produces an output object to be serialized into JSON and returned</param>
        /// <param name="createResponseForHostError">Is given an error message and is to produce an output object to be serialized into JSON and returned</param>
        public static void SynchronousRequestResponseMessageEngine<InMessageType>(
            Stream inStream,
            Stream outStream,
            Func<InMessageType, object> serialProcessorFunc,
            Func<string, InMessageType, object> createResponseForHostError) where InMessageType : class
        {
            InMessageType inMessage = null;
            try
            {
                while (true)
                {
                    inMessage = ReadNextMessage<InMessageType>(inStream);

                    if (inMessage == null)
                    {
                        return;
                    }

                    var outMessage = serialProcessorFunc(inMessage);

                    SendMessage(outMessage, outStream);
                }
            }
            catch (Exception e)
            {
                SendMessage(createResponseForHostError("Oops! " + e.Message, inMessage), outStream);
            }
        }

        public static void AsynchronousRequestResponseMessageEngine<InMessageType>(
            Stream inStream,
            Stream outStream,
            Func<InMessageType, object> parallelProcessorFunc,
            Func<string, InMessageType, object> createResponseForHostError) where InMessageType : class
        {
            try
            {
                while (true)
                {
                    var inMessageAsBytes = ReadNextMessageBytes(inStream);

                    if (inMessageAsBytes == null)
                    {
                        return;
                    }

                    var thread = new Thread(
                        () =>
                            {
                                var inMessage = ReadNextMessage<InMessageType>(inMessageAsBytes);

                                var outMessage = parallelProcessorFunc(inMessage);

                                ParallelSendMessage(outMessage, outStream);
                            });

                    thread.Start();
                }
            }
            catch (Exception e)
            {
                // TODO: We should do some handling here, but whatever
                throw e; 
            }
        }

        public static void ParallelSendMessage(object message, Stream outStream)
        {
            Debug.Assert(message != null && outStream != null);
            var jsonAsString = JsonConvert.SerializeObject(message);
            var lengthAsBytes = BitConverter.GetBytes(jsonAsString.Length);
            Debug.Assert(lengthAsBytes.Count() == 4);
            var jsonAsUTF8Binary = Encoding.UTF8.GetBytes(jsonAsString);
            lock (outStream)
            {
                outStream.Write(lengthAsBytes, 0, lengthAsBytes.Count());
                outStream.Write(jsonAsUTF8Binary, 0, jsonAsUTF8Binary.Count());                
            }
        }

        public static void SendMessage(object message, Stream outStream)
        {
            Debug.Assert(message != null && outStream != null);
            var jsonAsString = JsonConvert.SerializeObject(message);
            var lengthAsBytes = BitConverter.GetBytes(jsonAsString.Length);
            Debug.Assert(lengthAsBytes.Count() == 4);
            outStream.Write(lengthAsBytes, 0, lengthAsBytes.Count());
            var jsonAsUTF8Binary = Encoding.UTF8.GetBytes(jsonAsString);
            outStream.Write(jsonAsUTF8Binary, 0, jsonAsUTF8Binary.Count());
        }

        public static byte[] ReadNextMessageBytes(Stream inStream)
        {
            var lengthBuffer = new byte[NativeMessagingHeaderLengthInBytes];

            var bytesRead = inStream.Read(lengthBuffer, 0, NativeMessagingHeaderLengthInBytes);

            if (bytesRead == 0)
            {
                return null;
            }

            if (bytesRead != NativeMessagingHeaderLengthInBytes)
            {
                throw new ApplicationException("Got only " + bytesRead + "bytes in header instead of " + NativeMessagingHeaderLengthInBytes);
            }

            var jsonByteCount = BitConverter.ToInt32(lengthBuffer, 0);
            return ReadNumberOfBytes(inStream, jsonByteCount);
        }

        public static T ReadNextMessage<T>(Stream inStream) where T : class
        {
            return ReadNextMessage<T>(ReadNextMessageBytes(inStream));
        }

        public static T ReadNextMessage<T>(byte[] jsonAsBytes) where T : class
        {
            var jsonAsString = Encoding.UTF8.GetString(jsonAsBytes);
            return JsonConvert.DeserializeObject<T>(jsonAsString);            
        }

        public static byte[] ReadNumberOfBytes(Stream inStream, int bytesToReturn)
        {
            var byteArray = new byte[bytesToReturn];
            var totalByteCount = 0;
            while (totalByteCount != bytesToReturn)
            {
                // TODO: In theory this blocks forever, we should do an async read and put in a timer
                var currentByteCount = inStream.Read(byteArray, totalByteCount, bytesToReturn - totalByteCount);
                if (currentByteCount == 0)
                {
                    throw new ArgumentException("We were told to expect " + bytesToReturn + "bytes but we only got " + totalByteCount + " bytes before hitting the end of stream.");
                }
                totalByteCount += currentByteCount;
            }
            Debug.Assert(totalByteCount == bytesToReturn);
            return byteArray;
        }
    }
}
