---
layout: post
title: How do we exchange identities in Thali without making our users hate us?
categories:
    - security
---
In Thali, identities are public keys. But typing in a 4 Kb RSA key or even a 512 bit EC key isn’t exactly easy. So how do users securely exchange their keys? Our original approach used QRCodes. But lining up phones, scanning values, etc. is all a serious pain. So if ultimate security isn’t a requirement our backup plan is to use a variant of Bluetooth’s secure simple pairing with numeric comparison which itself is just an implementation of a coin-flip or commitment protocol. The main downside of this approach is that it provides a 1:1,000,000 chance of an attack succeeding.

For the rest of the details see my [blog article](http://www.goland.org/coinflippingforthali/)
