using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Math;

using System;
using System.Diagnostics;

namespace DotNetUtilities
{
    public struct BigIntegerRSAPublicKey
    {
        public readonly BigInteger modulus;
        public readonly BigInteger exponent;

        public BigIntegerRSAPublicKey(String modulus, String exponent)
        {
            Debug.Assert(String.IsNullOrWhiteSpace(modulus) == false && String.IsNullOrWhiteSpace(exponent) == false);
            this.modulus = new BigInteger(modulus);
            this.exponent = new BigInteger(exponent);
        }

        public BigIntegerRSAPublicKey(byte[] modulus, byte[] exponent)
        {
            Debug.Assert(modulus != null && exponent != null);
            this.modulus = new BigInteger(modulus);
            this.exponent = new BigInteger(exponent);
        }

        public BigIntegerRSAPublicKey(RsaKeyParameters rsaKeyParameters)
        {
            Debug.Assert(rsaKeyParameters != null);
            this.modulus = rsaKeyParameters.Modulus;
            this.exponent = rsaKeyParameters.Exponent;
        }

        public Boolean Equals(BigIntegerRSAPublicKey value)
        {
            return this.modulus.Equals(value.modulus) && this.exponent.Equals(value.exponent);
        }
    }
}
