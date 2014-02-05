using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DotNetUtilities
{
    using System.Diagnostics.CodeAnalysis;
    using System.Security.Cryptography;

    using MyCouch.Extensions;

    public class BogusAuthorizeCouchDocument
    {
        public const string RSAKeyType = "RSAKeyType";

        public BogusAuthorizeCouchDocument(BigIntegerRSAPublicKey publicKey)
        {
            _id = GenerateRsaKeyId(publicKey);
            modulus = publicKey.Modulus.ToString();
            exponent = publicKey.Exponent.ToString();
            keyType = RSAKeyType;
        }

        public string _id { get; set; }

        public string _rev { get; set; }

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
            return RSAKeyType + ":" + publicKey.Modulus + ":" + publicKey.Exponent;
        }
    }
}
