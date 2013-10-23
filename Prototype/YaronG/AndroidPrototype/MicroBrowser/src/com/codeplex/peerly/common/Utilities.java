package com.codeplex.peerly.common;

import org.bouncycastle.util.encoders.Base64;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: yarong
 * Date: 9/27/13
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class Utilities {
    // TODO: Near as I can tell NanoHTTPD has a single input stream for all requests so you have to just know when
    // to stop reading or you'll block until a new request comes in. So I use this function to read exactly the
    // number of bytes in the stream which only works because currently NanoHTTPD doesn't support chunked encoding
    // on requests.
    protected static String InputStreamOfCharsToString(int contentLength, InputStream inputStream) {
        String requestBody = null;
        // TODO: This is wrong on so many levels. First, we should validate that
        // we want to deal with the content we have given. Second, this
        // doesn't deal with chunked content. Third, we don't check that
        // the content is actually UTF-8. Fourth, we don't check the MIME
        // type. Fifth, we actually store everything in memory rather than
        // processing it as a stream so we can use RAM better. Etc.
        if (contentLength > 0) {
            byte[] byteArray = new byte[contentLength];
            try {
                int bytesRead = 0;
                while (bytesRead < contentLength) {
                    int currentBytesRead = inputStream.read(byteArray, bytesRead, contentLength - bytesRead);
                    if (currentBytesRead == -1) {
                        break;
                    }
                    bytesRead += currentBytesRead;
                }
                if (bytesRead != contentLength) {
                    throw new RuntimeException("Expected " + contentLength + "bytes but got " + bytesRead + "bytes.");
                }
                requestBody = new String(byteArray, "UTF-8");
            } catch (IOException ioe) {
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
    public static String InputStreamOfCharsToString(InputStream inputStream, String encoding) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        char[] charBuffer = new char[4096]; // The universe claims 4k is a good size
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, encoding);
        int charsRead;
        while ((charsRead = inputStreamReader.read(charBuffer)) != -1) {
            stringBuilder.append(charBuffer, 0, charsRead);
        }
        return stringBuilder.toString();
    }

    public static InputStream Base64ToInputStream(String base64String) {
        // I am using Bouncy Castle's Base64 because it's included in Android and I have the Bouncy Castle jar included
        // in the applet code. Android has a faster version in android.util.Base64 that is apparently taken from
        // http://migbase64.sourceforge.net/ but then what would I do for the applet? Yes, I could include the code and
        // even play games like naming it android.util.Base64 but it seems simpler for now to just use the Bouncy
        // Castle implementation.
        byte[] bytes = Base64.decode(base64String);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * TODO: This is criminally inefficient, we could instead just read in groups of 3 bytes and translate
     * them without ever having to manifest the whole byte value in memory. But oh well.
     *
     * @param byteArrayOutputStream
     * @return
     */
    public static String ByteArrayOutputStreamToBase64String(ByteArrayOutputStream byteArrayOutputStream) {
        return Base64.toBase64String(byteArrayOutputStream.toByteArray());
    }

    /**
     * This is used to null out passphrases.
     *
     * @param chars
     */
    public static void ReplaceCharsWithZeros(char[] chars) {
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = 0;
        }
    }
}
