---
title: OpenSSL and Thali
layout: default
---

# Requirements
In the Thali security model each principal has its own public key that it is identified with. We then use TLS mutual auth when connecting two principals in order to mutually authenticate to each other. Each principal is expected to send a cert chain that roots up to a self signed cert.

Note that in our use of TLS there are no CAs or trust stores. In fact, there are no certs at all. In Thali we move around the raw public keys, not certs. So what we validate at runtime is that the root cert matches a key we recognize and chains properly to the leaf cert. 

We do revoke public keys, but not certs. This can happen in the case that a particular device and its intermediate and leaf keys are compromised but the root is not. In that case the intermediate/leaf keys would be revoked and we do need to be able to check for any certs that try to use them.

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

# Changing the OpenSSL default validator
Ideally what we would do is add an extra flag to the OpenSSL default validator. There already is a X509_V_FLAG_CRL_CHECK that if it isn't set will disable CRL checking. We can also not set X509_V_FLAG_TRUSTED_FIRST which if turned off disables a tiny bit of the trust store logic. So really we just need to disable [this section](https://github.com/openssl/openssl/blob/e23a3fc8e38a889035bf0964c70c7699f4a38e5c/crypto/x509/x509_vfy.c#L259). And either mark all the certs as trusted or also disable [this section](https://github.com/openssl/openssl/blob/e23a3fc8e38a889035bf0964c70c7699f4a38e5c/crypto/x509/x509_vfy.c#L288) and [this section](https://github.com/openssl/openssl/blob/e23a3fc8e38a889035bf0964c70c7699f4a38e5c/crypto/x509/x509_vfy.c#L415).

I wonder what's the probability of us getting the OpenSSL folks to accept a new flag with these changes and then getting JXcore to take the new release of OpenSSL and then extending _tls_wrap.js and tls_wrap.cc to be able to pass the new flag in?

# Using SSL_CTX_set_cert_verify_callback
Another option would be to use the SSL_CTX_set_cert_verify_callback function to submit our own custom validator. This works fine on the OpenSSL side but it's a challenge on the JXcore side. The problem is that we have to somehow submit a native C validator as part of a JXcore extension and then somehow reference that validator from inside of Node.js and have some kind of pointer to it passed via the Node.js TLS library down through _tls_wrap.js to tls_wrap.cc. The changes to Node.js would be really minor. Essentially adding a new native function in tls_wrap.cc to expose the SSL_CTX_set_verify_callback interface.

The hard part is - how do we provide our native C validator? I could imagine us doing something like providing a JXcore extension that exposes some standard C layer factor interface that we could then point to in Node.js and JXcore would then be able to call the factory down in the C code and provide the instance (which would return a verify callback) to OpenSSL.

# Driving validation from Node.js
Another option is to try and drive things from the Node.js layer. But it's a bit tricky. What we would have to do is the following:
1. Add a method to tls_wrap.cc that exposes the cert chain as an array of PEM (base 64 strings) values. Alternatively we could enhance the existing getPeerCertificate call to include the PEM values in the existing JSON.
2. Write our own JXcore extension that performs our own stand alone validation (outside the context of the handshake) and pass it the PEM values.
3. Write an event listener that will sit on the secure/secureEvent events and make two native calls, one to get the PEM encoded cert chain 
4. 
STOPPED HERE

# Getting OpenSSL to do what we want
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

# Making this work in JXCore
Now in theory the solution is fairly straight forward, at least if we were only using OpenSSL. We could call SSL_CTX_set_cert_verify_callback and specify our own custom validator. This would replace X509_verify_cert completely and instead we could use some hacked up logic of our own that works as I describe above. Heck, for now, we could just call internal_verify() and call it a day and add the rest later.

But, alas, it's really not that simple.

The problem is that we aren't talking to OpenSSL directly. We are talking to Node.js who is talking to its HTTPS library who is talking to its TLS library who is talking to OpenSSL. So how are we supposed to build our custom validator and how do we get it to OpenSSL?

## Get JXcore to let us use our own validator directly with OpenSSL (or not)

Keep in mind that we need to re-use code already in OpenSSL (I'm too much of a wimp to trust things like [pkijs](https://www.npmjs.com/package/pkijs) quite yet) so somewhere there is C code. Now the good news is that JXCore supports calling out to the native platform. The bad news is that this isn't good enough. We don't just need the custom validator available in C, we have to get it into a physical position so that when Node calls TLS who calls OpenSSL we can pass in that validator. This means solving two different problems.

The first problem is how to make our C code visable to the instance of OpenSSL being used by Node.js? That is a linking problem. Although presumably with a little function passing at the C level using a function declaration provided in JXcore we could solve that one.

The second problem is how do we change Node's TLS library to allow us to use our customer C validator for OpenSSL? There are two approaches here. One approach is we somehow figure out how to get our custom validator submitted to SSL_CTX_set_cert_verify_callback when Node.js sets up the SSL connection in tls_wrap.cc and call it a day. That would be ideal.

But I'm not sure if the JXcore folks would be willing to go for all the magic implied here. In other words we would need some way to create a native extension that has a validator and tell JXcore about it via the native interface and then have JXcore marshal an instance of it and pass it to the OpenSSL context and call SSL_CTX_set_cert_verify_callback with it. This is all doable, I just don't know if they will be willing to do it.

## Get JXcore to expose a few OpenSSL APIs

But the other way, the one Node uses itself, is that it tends to accept a lot and then depend on the user, in Node (e.g. in Javascript) listening to the 'secure' and/or 'secureConnect' event. This is a choke point where in theory the Javascript code can look around the SSL context and decide if it likes it or not. This leads to a possible solution to our validation problem but it's performance implications and complexity don't make me happy. The solution would be:

1. Extend the _tls_wrap.js and tls_wrap.cc libraries to expose a variant of getPeerCertificate that returns an array of PEM encoded certs representing the X.509 certs that were presented by the other principal.
  * Implementing this will require calling OpenSSL's SSL_get_peer_certificate function followed by a call to one of the PEM_write_* methods to write the X509 in memory cert out to PEM (which is base 64 string) and then returning that back to Javascript as an array
2. Write native C code that does all the validation described above and give it an interface that expects to receive an array of PEM encoded certs representing the cert chain which will then be translated to X509 objects which will be shoved into a STACK_OF(X509) and then used to create a X509_STORE_CTX which can then be used to call the functions above. The final result being a boolean, passed back to Javascript, that specifies if it all worked or not.
3. Listen to the secure and/or secureConnect event and when it happens call out to "enhanced" getPeerCertificate function to get the list of PEM values for the chain and then make a second native call to call our custom validator.

The good news is that all of the above should actually work. The bad news is that it's nightmarish for our perf! Those two native calls? They are synchronous! This means that JXCore is shut down while those calls are off doing expensive crypto operations! And near as I can tell those functions MUST be synchronous because our only chance to kill the connection if it is bad is in the listener to the secure/secureConnect event. Once we exit, that's it, our chance to stop bad things is lost.

An obvious alternative is to create a variant of the secure/secureConnection event that is willing to block a connection until it gets an explicit callback. In other words the event itself will come with a context object that will have a method that specifies if the connection should be allowed. This would at least allow us to do all the expensive crypto work on its own thread and not block node while we wait for validation to complete.
