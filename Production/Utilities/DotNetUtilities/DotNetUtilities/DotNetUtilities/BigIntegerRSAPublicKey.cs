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

namespace DotNetUtilities
{
    using System.Diagnostics;
    using System.Security.Cryptography;
    using System.Security.Cryptography.X509Certificates;

    using Org.BouncyCastle.Crypto.Parameters;
    using Org.BouncyCastle.Math;

    public struct BigIntegerRSAPublicKey
    {
        public readonly BigInteger Modulus;
        public readonly BigInteger Exponent;

        public BigIntegerRSAPublicKey(string modulus, string exponent)
        {
            Debug.Assert(string.IsNullOrWhiteSpace(modulus) == false && string.IsNullOrWhiteSpace(exponent) == false);
            this.Modulus = new BigInteger(modulus);
            this.Exponent = new BigInteger(exponent);
        }

        public BigIntegerRSAPublicKey(RSAParameters rsaParameters)
        {
            this.Modulus = new BigInteger(1, rsaParameters.Modulus);
            this.Exponent = new BigInteger(1, rsaParameters.Exponent);
        }

        public BigIntegerRSAPublicKey(RsaKeyParameters rsaKeyParameters)
        {
            Debug.Assert(rsaKeyParameters != null);
            this.Modulus = rsaKeyParameters.Modulus;
            this.Exponent = rsaKeyParameters.Exponent;
        }

        public BigIntegerRSAPublicKey(X509Certificate2 certificate2)
            : this(((RSACryptoServiceProvider)certificate2.PublicKey.Key).ExportParameters(false))
        {
        }

        public bool Equals(BigIntegerRSAPublicKey value)
        {
            return this.Modulus.Equals(value.Modulus) && this.Exponent.Equals(value.Exponent);
        }
    }
}
