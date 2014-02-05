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

/* Note: This code is copied verbatum from MyCouch (which has a MIT license) and we have just made small changes to
 * support specifying a HttpMessageHandler. Our hope is that the MyCouch folks will take this file as a pull request
 * and we can then get rid of it.
 */

namespace DotNetUtilities
{
    using System;
    using System.Linq;
    using System.Net.Http;
    using System.Net.Http.Headers;
    using System.Threading;
    using System.Threading.Tasks;

    using EnsureThat;

    using MyCouch;
    using MyCouch.Extensions;
    using MyCouch.Net;

    /// <summary>
    /// An implementation of the IConnection class from MyCouch designed to support Thali's communication needs
    /// </summary>
    public class BasicHttpClientConnection : IConnection
    {
        protected HttpClient HttpClient { get; private set; }

        protected bool IsDisposed { get; private set; }

        public Uri Address
        {
            get { return HttpClient.BaseAddress; }
        }

        public BasicHttpClientConnection(Uri dbUri, HttpMessageHandler handler = null, bool disposeHandler = false)
        {
            Ensure.That(dbUri, "dbUri").IsNotNull();

            HttpClient = CreateHttpClient(dbUri, handler, disposeHandler);
        }

        public virtual void Dispose()
        {
            Dispose(true);
            GC.SuppressFinalize(this);
        }

        protected virtual void Dispose(bool disposing)
        {
            ThrowIfDisposed();

            IsDisposed = true;

            if (!disposing)
                return;

            HttpClient.CancelPendingRequests();
            HttpClient.Dispose();
            HttpClient = null;
        }

        protected virtual void ThrowIfDisposed()
        {
            if (IsDisposed)
                throw new ObjectDisposedException(GetType().Name);
        }

        private HttpClient CreateHttpClient(Uri dbUri, HttpMessageHandler handler, bool disposeHandler)
        {
            var client = handler != null ? new HttpClient(handler, disposeHandler) : new HttpClient();
            client.BaseAddress = new Uri(this.BuildCleanUrl(dbUri));
            client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue(HttpContentTypes.Json));

            if (!string.IsNullOrWhiteSpace(dbUri.UserInfo))
            {
                var parts = dbUri.UserInfo
                    .Split(new[] { ":" }, StringSplitOptions.RemoveEmptyEntries)
                    .Select(p => Uri.UnescapeDataString(p))
                    .ToArray();

                client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", string.Join(":", parts).AsBase64Encoded());
            }

            return client;
        }

        private string BuildCleanUrl(Uri uri)
        {
            EnsureValidUri(uri);

            var url = string.Format("{0}://{1}{2}", uri.Scheme, uri.Authority, uri.LocalPath);
            while (url.EndsWith("/"))
                url = url.Substring(0, url.Length - 1);

            return url;
        }

        private void EnsureValidUri(Uri uri)
        {
            Ensure.That(uri, "uri").IsNotNull();
            Ensure.That(uri.LocalPath, "uri.LocalPath")
                  .IsNotNullOrEmpty()
                  .WithExtraMessageOf(() => ExceptionStrings.BasicHttpClientConnectionUriIsMissingDb);
        }

        public virtual async Task<HttpResponseMessage> SendAsync(HttpRequest httpRequest)
        {
            ThrowIfDisposed();

            return await HttpClient.SendAsync(OnBeforeSend(httpRequest)).ForAwait();
        }

        public virtual async Task<HttpResponseMessage> SendAsync(HttpRequest httpRequest, CancellationToken cancellationToken)
        {
            ThrowIfDisposed();

            return await HttpClient.SendAsync(OnBeforeSend(httpRequest), cancellationToken).ForAwait();
        }

        public virtual async Task<HttpResponseMessage> SendAsync(HttpRequest httpRequest, HttpCompletionOption completionOption)
        {
            ThrowIfDisposed();

            return await HttpClient.SendAsync(OnBeforeSend(httpRequest), completionOption).ForAwait();
        }

        public virtual async Task<HttpResponseMessage> SendAsync(HttpRequest httpRequest, HttpCompletionOption completionOption, CancellationToken cancellationToken)
        {
            ThrowIfDisposed();

            return await HttpClient.SendAsync(OnBeforeSend(httpRequest), completionOption, cancellationToken).ForAwait();
        }

        protected virtual HttpRequest OnBeforeSend(HttpRequest httpRequest)
        {
            ThrowIfDisposed();

            return httpRequest.RemoveRequestType();
        }
    }
}
