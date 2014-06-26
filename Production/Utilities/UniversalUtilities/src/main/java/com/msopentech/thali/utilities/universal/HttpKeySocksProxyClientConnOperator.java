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

/*
This code began life as a copy and paste from SocksProxyClientConnOperator in NetCipher which we then modified
to meet our needs. That original code was licensed as:

This file contains the license for Orlib, a free software project to
provide anonymity on the Internet from a Google Android smartphone.

For more information about Orlib, see https://guardianproject.info/

If you got this file as a part of a larger bundle, there may be other
license terms that you should be aware of.
===============================================================================
Orlib is distributed under this license (aka the 3-clause BSD license)

Copyright (c) 2009-2010, Nathan Freitas, The Guardian Project

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.

    * Neither the names of the copyright owners nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*****
Orlib contains a binary distribution of the JSocks library:
http://code.google.com/p/jsocks-mirror/
which is licensed under the GNU Lesser General Public License:
http://www.gnu.org/licenses/lgpl.html

*****

 */

package com.msopentech.thali.utilities.universal;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.impl.conn.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;

import java.io.*;
import java.net.*;

/**
 * This is all but copied and pasted from SocksProxyClientConnOperator in libnetcipher from the Guardian Project.
 * It's job is to put in a socket underneath the HttpKeySSLSocketFactory to properly handle talking to
 * Tor's SOCKS 4a proxy.
 */
public class HttpKeySocksProxyClientConnOperator extends DefaultClientConnectionOperator {

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
    private static final int READ_TIMEOUT_MILLISECONDS = 60000;

    protected final Proxy proxy;

    public HttpKeySocksProxyClientConnOperator(SchemeRegistry registry, Proxy proxy) {
        super(registry);

        if (proxy == null || proxy.type() != Proxy.Type.SOCKS) {
            throw new IllegalArgumentException("We only support SOCKS proxies");
        }

        this.proxy = proxy;
    }

    // Derived from the original DefaultClientConnectionOperator.java in Apache HttpClient 4.2
    @Override
    public void openConnection(
            final OperatedClientConnection conn,
            final HttpHost target,
            final InetAddress local,
            final HttpContext context,
            final HttpParams params) throws IOException {
        Socket socket = null;
        Socket sslSocket = null;
        try {
            if (conn == null || target == null || params == null) {
                throw new IllegalArgumentException("Required argument may not be null");
            }
            if (conn.isOpen()) {
                throw new IllegalStateException("Connection must not be open");
            }

            // The original NetCipher code uses a SchemeSocketFactory class that isn't supported by the version
            // of Apache that ships standard with Android. It also doesn't support the layered socket factory
            // interface either. We work around this later on but for now we just get our HttpKeySSLSocketFactory
            Scheme scheme = schemeRegistry.getScheme(target.getSchemeName());
            HttpKeySSLSocketFactory httpKeySSLSocketFactory = (HttpKeySSLSocketFactory) scheme.getSocketFactory();

            int port = scheme.resolvePort(target.getPort());
            String host = target.getHostName();

            // Perform explicit SOCKS4a connection request. SOCKS4a supports remote host name resolution
            // (i.e., Tor resolves the hostname, which may be an onion address).
            // The Android (Apache Harmony) Socket class appears to support only SOCKS4 and throws an
            // exception on an address created using INetAddress.createUnresolved() -- so the typical
            // technique for using Java SOCKS4a/5 doesn't appear to work on Android:
            // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/net/PlainSocketImpl.java
            // See also: http://www.mit.edu/~foley/TinFoil/src/tinfoil/TorLib.java, for a similar implementation

            // From http://en.wikipedia.org/wiki/SOCKS#SOCKS4a:
            //
            // field 1: SOCKS version number, 1 byte, must be 0x04 for this version
            // field 2: command code, 1 byte:
            //     0x01 = establish a TCP/IP stream connection
            //     0x02 = establish a TCP/IP port binding
            // field 3: network byte order port number, 2 bytes
            // field 4: deliberate invalid IP address, 4 bytes, first three must be 0x00 and the last one must not be 0x00
            // field 5: the user ID string, variable length, terminated with a null (0x00)
            // field 6: the domain name of the host we want to contact, variable length, terminated with a null (0x00)

            socket = new Socket();
            conn.opening(socket, target);
            socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
            socket.connect(proxy.address(), CONNECT_TIMEOUT_MILLISECONDS);

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write((byte)0x04);
            outputStream.write((byte)0x01);
            outputStream.writeShort((short)port);
            outputStream.writeInt(0x01);
            outputStream.write((byte)0x00);
            outputStream.write(host.getBytes());
            outputStream.write((byte)0x00);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            if (inputStream.readByte() != (byte)0x00 || inputStream.readByte() != (byte)0x5a) {
                throw new IOException("SOCKS4a connect failed");
            }
            inputStream.readShort();
            inputStream.readInt();

            // In the NetCipher code we cast to SchemeLayeredSocketFactory and call createLayeredSocket which amongst
            // other things takes 'params' as an argument. But none of this is supported in Android. When I looked in
            // Java at what createLayeredSocket was actually doing it was just calling createSocket with exactly the
            // arguments used below (it ignored params completely). So we should be good.
            sslSocket = ((HttpKeySSLSocketFactory)httpKeySSLSocketFactory).createSocket(socket, host, port, true);
            conn.opening(sslSocket, target);
            sslSocket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
            prepareSocket(sslSocket, context, params);
            conn.openCompleted(httpKeySSLSocketFactory.isSecure(sslSocket), params);
            // TODO: clarify which connection throws java.net.SocketTimeoutException?
        } catch (IOException e) {
            try {
                if (sslSocket != null) {
                    sslSocket.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {}
            throw e;
        }
    }

    @Override
    public void updateSecureConnection(
            final OperatedClientConnection conn,
            final HttpHost target,
            final HttpContext context,
            final HttpParams params) throws IOException {
        throw new RuntimeException("operation not supported");
    }

    @Override
    protected InetAddress[] resolveHostname(final String host) throws UnknownHostException {
        throw new RuntimeException("operation not supported");
    }

}
