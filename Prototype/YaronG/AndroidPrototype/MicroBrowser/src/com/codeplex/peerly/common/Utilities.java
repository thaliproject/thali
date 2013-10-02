package com.codeplex.peerly.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 9/27/13
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utilities {
    // TODO: This function needs to go away. It is completely dependent on knowing the content length which,
    // since the NanoHTTPD server doesn't support chunking, is kind of o.k. But HTTP clients are allowed
    // to send chunked messages so we have a real problem here. But for now if we don't read exactly the number
    // of bytes we are supposed to then we'll never know where the request actually ends since they don't
    // structure the streams to stop when the request body is done.
    protected static String StringifyInputStream(int contentLength, InputStream inputStream) {
        String requestBody = null;
        // TODO: This is wrong on so many levels. First, we should validate that
        // we want to deal with the content we have given. Second, this
        // doesn't deal with chunked content. Third, we don't check that
        // the content is actually UTF-8. Fourth, we don't check the MIME
        // type. Fifth, we actually store everything in memory rather than
        // processing it as a stream so we can use RAM better. Etc.
        if (contentLength > 0)
        {
            byte[] byteArray = new byte[contentLength];
            try
            {
                int bytesRead = 0;
                while(bytesRead < contentLength)
                {
                    int currentBytesRead = inputStream.read(byteArray, bytesRead, contentLength - bytesRead);
                    if (currentBytesRead == -1)
                    {
                        break;
                    }
                    bytesRead += currentBytesRead;
                }
                if (bytesRead != contentLength)
                {
                    throw new RuntimeException("Expected " + contentLength + "bytes but got " + bytesRead + "bytes.");
                }
                requestBody =  new String(byteArray, "UTF-8");
            } catch (IOException ioe)
            {
                throw new RuntimeException(ioe.toString());
            }
        }
        return requestBody;
    }

    // TODO: This is wrong on so many levels. First, we should validate that
    // we want to deal with the content we have given. Second, this
    // doesn't deal with chunked content. Third, we don't check that
    // the content is actually UTF-8. Fourth, we don't check the MIME
    // type. Fifth, we actually store everything in memory rather than
    // processing it as a stream so we can use RAM better. Etc.
    // Thanks to http://stackoverflow.com/questions/13602465/convert-byte-array-or-strinbuilder-to-utf-8
    public static String StringifyByteStream(InputStream inputStream, String encoding) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        char[] charBuffer = new char[4096]; // The universe claims 4k is a good size
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, encoding);
        int charsRead;
        while ((charsRead = inputStreamReader.read(charBuffer)) != -1) {
            stringBuilder.append(charBuffer, 0, charsRead);
        }
        return stringBuilder.toString();
    }
}
