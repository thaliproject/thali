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
    using System.Diagnostics.CodeAnalysis;

    using LoveSeat.Interfaces;

    public class BogusAuthorizeCouchDocument : IBaseObject
    {
        public const string RSAKeyType = "RSAKeyType";

        public BogusAuthorizeCouchDocument()
        {
        }

        public BogusAuthorizeCouchDocument(BigIntegerRSAPublicKey publicKey)
        {
            Id = GenerateRsaKeyId(publicKey);
            var modulusAndExponent = publicKey.GetModulusAndExponentAsString();
            modulus = modulusAndExponent.Item1;
            exponent = modulusAndExponent.Item2;
            keyType = RSAKeyType;
        }

        [SuppressMessage("StyleCop.CSharp.NamingRules", "SA1300:ElementMustBeginWithUpperCaseLetter", Justification = "The element's name will be used to generate the JSON name which is lowercase")]
        public string modulus { get; set; }

        [SuppressMessage("StyleCop.CSharp.NamingRules", "SA1300:ElementMustBeginWithUpperCaseLetter", Justification = "The element's name will be used to generate the JSON name which is lowercase")]
        public string exponent { get; set; }

        [SuppressMessage("StyleCop.CSharp.NamingRules", "SA1300:ElementMustBeginWithUpperCaseLetter", Justification = "The element's name will be used to generate the JSON name which is lowercase"),
        SuppressMessage("StyleCop.CSharp.NamingRules", "SA1307:AccessibleFieldsMustBeginWithUpperCaseLetter", Justification = "The element's name will be used to generate the JSON name which is lowercase")]
        public string keyType { get; set; }

        /// <summary>
        /// Enterprise in the public key database MUST have IDs of the following form of their entries won't be found
        /// when the server does a lookup
        /// </summary>
        /// <param name="publicKey"></param>
        /// <returns></returns>
        public static string GenerateRsaKeyId(BigIntegerRSAPublicKey publicKey)
        {
            var modulusAndExponent = publicKey.GetModulusAndExponentAsString();
            return RSAKeyType + ":" + modulusAndExponent.Item1 + ":" + modulusAndExponent.Item2;
        }

        public string Id { get; set; }

        public string Rev { get; set; }

        public string Type { get; private set; }
    }
}
