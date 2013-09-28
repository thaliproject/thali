package com.codeplex.peerly.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 9/27/13
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utilities {
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
}
