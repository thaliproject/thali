---
title: OpenSSL and Thali
layout: page-fullwidth
permalink: "/OpenSSLThali/"
---

# Requirements
In the Thali security model each principal has its own public key that it is identified with. We then use TLS mutual auth when connecting two principals in order to mutually authenticate to each other. Each principal is expected to send a cert chain that roots up to a self signed cert.

In Thali we explicitly do not use the CA security model. In our world an identity is a public key. So when we validate a cert chain in Thali all we really want to know is if the leaf node has a path to the root cert. The public key in the root cert is then the "identity" of the principal at the other end of the line. We then know the identity of the called (their public key) and we can then use that public key with our ACL mechanism to decide what (if anything) the caller should be allowed to do. In other words TLS is for authentication, not authorization.

There is an exception to this however. If a particular identity (read: public key) is known to be compromised, then we do want to reject any connections from it. Technically we don't need to do this. The requester's identity won't be in any ACLs and all of their requests will be automatically rejected. But the earlier one can reject a bad caller the better so potentially we do want to use a revocation store. But keep in mind that what we want to revoke is not a particular cert necessarily, but a public key and any keys that chain off that public key.

When we receive a cert chain over TLS all we really care about is:

1. Each cert in the chain is "internally valid", that is, each cert:
 * Is not expired
 * Does not contain any policy, name constraint or extension restrictions that make it inappropriate for our purposes
 * Is signed with the key it is advertising
 * Is not on our internal revocation list
2. That the chain as a whole is valid, that is, each cert is properly signed by its parent cert all the way up to the root

There are a number of things we explicitly do not care about, these include:

1. Having any kind of trust store, that is, we don't ever look to find the root or intermediate certs in any trust store
2. Making any checks related to IP address or DNS addresses, the connection validates the identity of the principal, not any binding to any particular network endpoint
3. Checking the network based CRL mechanism, until we support having root keys off device we can't use this mechanism and its presence is most likely a form of de-anonimization attack.

Thali is implemented using Node.js which uses OpenSSL. So the question we are faced with is - how do we meet the above requirements in OpenSSL?

The main issue is that OpenSSL's default validator uses a trust store and for the reasons described above we don't want to use a trust store. So how do we make things work for us?

# Can't we just use the trust store anyway?
In theory we could make the trust store work. For example, imagine that we do have certs for everyone we want to talk to. In that case a TLS client could just specify the cert of the server it wanted to connect to as being the one and only CA for that connection and then let OpenSSL (and Node.js, which supports this kind of configuration) do its thing. Similarly on the server side we could specify the certs for all the principals the server knows about as the CAs for that server. This would properly validate connections using the existing Node.js/OpenSSL infrastructure. A tiny problem is that the list of principals changes over time and the way Node.js handles specifying CAs is one time at server start up. However this is an issue with Node.js, not with OpenSSL. OpenSSL supports dynamically adding new CAs to the SSL_CTX. So in theory we could extend Node.js to have a new addCACert function that could be called on a server object to allow for new principals to be added.

So with a tiny bit of work we can make Node.js and OpenSSL do what we need, more or less, as is. Sorta. There are still problems.

The problem with all of this is - how did a principal get another principal's cert? In Thali we pass around principal's identities (which is their public key) as URLs, see our [Httpkey URL Scheme](/HttpkeyURLScheme). But how do we make that work with a full cert? We would have to start base 64 encoding root certs. And what, btw, does it mean if the root cert expires? This is actually a very tricky issue. In a CA model a cert actual validates the relationship between a public key and something else, usually some kind of identity or DNS name or IP address or whatever. That isn't the model in Thali. In Thali the public key is the identity. It does not 'relate' to anything else. So when the public key is no longer usable (it was compromised, it is too short for modern attacks, etc.) then that identity no longer exists. It's gone. So the only reason we would use certs isn't because they really make sense for Thali but because it makes it easier to use OpenSSL and Node.js. So we really want to stick with public keys, not certs.

The other problem is that servers want to be able to accept authenticated connections from principals they do not know. Think of this as the equivalent of going to a website and create an account. If you come back with the same account you will get a known context. Thali wants to do the same thing. Someone could connect to my server who I don't know (and I don't need to know). If they come back again with the same identity I will return the same context for them. How do we handle that scenario if we are required to submit a list of all the CAs we know ahead of time? We don't know the principal ahead of time.

Of course we could work around the last problem with a custom validator but that is work we already describe below to give us a solution that divorces us from the Trust Store concept all together.

So for Thali the bottom line is that the trust store concept is an artifact of the CA model and Thali doesn't use the CA model so we don't need the trust store.

# A new OpenSSL flag - trust_all_certs

Our ideal solution to our problem would be a new trust_all_certs flag that says that any cert that is received in a chain is automatically trusted. The term 'trust' here has a very specific and limited definition, it means we treat all certs as being in the trust store. We don't necessarily even care if a particular public key has been revoked. The reason is that we use our own ACL layer to enforce access so if someone tries to connect with a revoked key they won't have the right ACL to do anything. It would be nice to check for revoked keys at this point but it's not a must have.

There are several potential approaches for making this new flag work.

## Override get_issuer
In looking at the OpenSSL code there seems like a fairly straight forward way to make this work. In x509_vfy.c there is a function called X509_verify_cert which is where the default validator (I believe) lives. This takes as an argument a X509_STORE_CTX. x509_STORE_CTX is actually a typedef in ossl_typ.h for the X509_store_ctx_st structure. This structure contains a pointer to a function called get_issuer which is used by the validator to see if a specific cert is in the trust store. So in theory if we can replace that function with our own, our version of the function could (in essence) always return 'true' to any request to validate a cert.

Near as I can tell X509_STORE_CTX is actually taken from the root SSL_CTX object which has a member cert_store which is a X509_STORE which is an alias for x509_store_st. It is this object that is used to create a X509_STORE_CTX object using the x509_STORE_CTX_init function. So in theory the trick is to grab cert_store from SSL_CTX and set it the way we want. Then any connections created from that SSL_CTX will use our "trust all" get_issuer function.

### Making this work in Node.js
node_crypto.cc has a method SecureContext::Init which calls SSL_CTX_new which sets sc->ctx_ which is the SSL_CTX object. Just below that sc->ca_store is set to NULL. That ca_store argument is the X509_STORE object we are looking for. So in theory if we can wrap that with an IF that looks for a special flag we specify then we could set the ca_store to X509_STORE_new() (which just initializes everything) and then override the get_issuer function to point to our 'accept all' function.

The only problem is that if no CA is specified in the tls.createServer or equivalent command then crypto.js will call addrootCerts which will override our X509_STORE setting. That logic seems to live in crypto.js in the createCredentials method. This is called in tls.js by SecurePair. So we need to make sure we pass in an argument in options to SecurePair so that if we are using 'trust all' we don't call crypto.createCredentials(). In general the real work here is making sure we validate the options that are submitted to the TLS object (and secure context in general) to make sure we don't have contradictory options. Someone, for example, shouldn't be specifying a custom list of CAs and using our flag. If they do we should return an error.

## Using a custom validator
The other option for making this all work is to replace the default validator all together with our own custom validator. This will, by definition, do whatever we want it to do. Most likely we would start with the default validator and just excise the trust store checks. Below I look at the various components of the default validator and identify what we do and don't want to keep.

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

To make this work we have to call a function called SSL_CTX_set_cert_verify_callback which takes our old friend SSL_CTX and lets us set our custom validator.

### What about SSL_CTX_set_verify/SSL_set_verify?
There is a similar sounding function called SSL_CTX_set_verify/SSL_set_verfy that would seem like a potential candidate. But in practice it is not. The verify function isn't called until after the validator is run and if we are using the default validator then it will fail due to trust store issues.

### Making this work in Node.js
This should be as easy as slipping an argument down into SecureContext::Init which would then call SSL_CTX_set_cert_cerify_callback. Node.js doesn't (and shouldn't) ever mess with the validator so once we set it on the parent context it should show up everywhere we need it automatically.

## Trade offs between the approaches
I suspect the most robust solution is the custom validator. The reason being that regardless of what methods node.js calls on the SSL_CTX the custom validator always gets the final say. So we don't have to worry about some obscure function somewhere over writing the cert_store object and making a mess of things.

The reason why I don't like the custom validator is because TLS validation code is easy to screw up. Writing one's own validator seems like a recipe for disaster. When various bugs are found in the default validator are we going to find out about them and fix them in our custom version? It's just a mess.

A potential solution is a hybrid approach where we create our own custom validator but all it does is make sure that the X509_STORE_ctx has the right trust store validator before then calling the default validator. This gives us the benefit of not worrying about anything messing with our settings at the Node.js layer while also allowing us to rely on the default validator.

My guess is that we should use a custom validator whose only purpose is to validate the store is set correctly before calling the default validate and we simultaneously should check the options to make sure that no one submitted any options that could cause "odd" behavior. Think of it as defense in depth.

## Could we get OpenSSL to just implement our flag?

The CA model is known to have serious issues and so Thali doesn't use it. We can't be the only ones! But anyone who wants to use TLS with OpenSSL outside of the CA model really needs an easy way to disable trust store checking. So one wonders if we might not be able to convince the OpenSSL folks to add a 'disable trust store checks' flag?

# Disabling CRL/OCSP Checks
Because we don't use cert revocation (we revoke public keys instead) both CRL and OCSP really aren't useful for us. Especially since our default scenarios work completely offline and with devices that don't have stable IP addresses or even DNS addresses. So any cert that comes our way with a CRL or OCSP extension is most likely some kind of attack to cause us to leak location or accept a badly formatted file. So we really need to figure out how to turn both of these off.

Turning off CRL checking is pretty easy in OpenSSL, we just need to not pass in the X509_V_FLAG_CRL_CHECK. Node.js will only set this flag if the AddCRL function is called. So um... don't do that.

As for OCSP. I need to do more homework. I looked around and I can see the OCSP code but I can't find anywhere in x509_vfy.c where it is called. So I'm not completely sure how to make sure it doesn't get used. But we'll figure it out.

OCSP stapling is also useless since there is no CA.
