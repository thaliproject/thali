---
title: OpenSSL and Thali
layout: default
---

# Requirements
In the Thali security model each principal has its own public key that it is identified with. We then use TLS mutual auth when connecting two principals in order to mutually authenticate to each other. Each principal is expected to send a cert chain that roots up to a self signed cert.

Note that in our use of TLS there are no CAs or trust stores. In fact, there are no certs at all. In Thali we move around the raw public keys, not certs. So what we validate at runtime is that the root cert matches a key we recognize and chains properly to the leaf cert. 

We do have potentially revoked public keys, but not certs. This can happen in the case that a particular device and its intermediate and leaf keys are compromised but the root is not. In that case the intermediate/leaf keys would be revoked and we do need to be able to check for any certs that try to use them.

But in general when we receive a chain all we really care about is:

1. Each cert in the chain is "internally valid", that is, each cert:
 * Is not expired
 * Does not contain any policy, name constraint or extension restrictions that make it inappropriate for our purposes
 * Is signed with the key it is advertising
 * Is not on our internal revocation list
2. That the chain as a whole is valid, that is, each cert is properly signed by its parent cert all the way up to the root

There are a number of things we explicitly do not care about, these include:

1. Having any kind of trust store, that is, we don't ever look to find the root or intermediate certs in any trust store
2. Making any checks related to IP address or DNS addresses, the connection validates the identity of the principal, not any binding to any particular network endpoint
3. Checking the network based CRL mechanism, we distribute revoked keys (not certs) through our own mechanisms

# The Challenge
Thali is implemented on top of JXcore which is a variant of node.js. This means that we are using the node.js TLS libraries built on top of OpenSSL. Right off we have a problem because Node's TLS library either wants us to specify a CA (both on client and server) or we have to set rejectedUnauthorized = true. The problem is rejectedUnauthorized when it makes its way down to TLS it hits [here](https://github.com/joyent/node/blob/8e539dc26dd811c960a8943b28c4a351aa5d89ad/src/tls_wrap.cc#L700) which then hits [here](https://www.openssl.org/docs/ssl/SSL_CTX_set_verify.html) with the flag SSL_VERIFY_NONE which attempts to stop servers from soliciting client certs (bad) and on the client side allows any cert presented by the server to be accepted no matter how broken (very bad).

In trying to figure out how to fix this I took a look at OpenSSL. If I'm reading the code right then the interesting function is X509_verify_cert in the x509_vfy.c file. The problem is that this function is really focused on a bunch of things, like trust store validation, that we explicitly don't want to do.

So it's tempting them to just jump to the internal_verify() function which seems to do exactly what we want. The problem is that there are a few checks in X509_verify_cert that are not in internal_verify that I am concerned we will need. Below I try to list the functionality I think we do and do not want from X509_verify_cert:

* X509_verify_cert functionality
  * Things I think we do want
    * check_chain_extensions() - We really want a negative check here. Our certs shouldn't be using any extensions but if they are then there is probably a problem and we should reject the cert chain. The point is to prevent people from abusing certs by using them in an unintended context.
    * check_name_constraints() - Ideally we should honor name constraints like path length. I don't really see us using them much but if they are there we need to do the right thing.
    * X509_chain_check_suiteb() - Truthfully this is a nice to have. It validates that the chain follows best practices as specified by the NSA in Suite B. I haven't dug all the way to the bottom of this one to figure out if there is anything in here we might not like.
    * internal_verify() - This actually does the bulk of what we want. In an ideal world we would use it and nothing else but I'm concerned about some of the other functions I mention in this section. I think we need them.
    * X509_policy_check() - We mostly want to make sure no policies are actually used
  * Things I don't think we want
    * The entire first part of the function which is focused on figuring out if the chain is in the trust store, we don't have or want a trust store
    * check_trust() - I believe this uses the logic from the first part of the function to figure out if the chain is 'trusted' vis a vie the trust store which, as previously mentioned, we don't have.
    * check_id() - We actually need the inverse of this function. Our certs should not contain a host name, email or address. But since it's hard to create certs without them we should just ignore them and hence don't need this function.
    * check_revocation() - We don't use the X.509 CRL mechanism so this is useless to us. In fact, we should ideally reject any certs that even contain the CRL declaration.
    * v3_asid_validate_path() - This is an implementation of RFC 3779 that lets one bind identities to certs. We don't work that way. At most we should check if this extension is used and if so reject the cert.
  
  
  
