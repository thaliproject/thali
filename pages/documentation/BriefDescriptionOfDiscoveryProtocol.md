---
title: A brief description of Thali's discovery protocol
layout: page-fullwidth
permalink: "/BriefDescriptionOfDiscoveryProtocol/"
header:
   image_fullwidth: "header.png"
categories:
    - documentation
---

# Introduction
One of [Project Thali's](http://www.thaliproject.org) capabilities is to let mobile devices find each other using P2P technologies like BLE, Bluetooth and various flavors of WiFi. Thali is a sync based system so the way discovery works is that when a device's local DB changes state (due to a user enter information or due to synching with some other device) we have a small list of peers, each identified by a public key, who we potentially need to notify about that update. So we will then advertise the peers we are looking for over local radio. If they are in range then they can connect and synchronize the new data.

Our security requirements for discovery are written out [here](http://www.goland.org/localdiscoverybillofrights/) but can be summarized as during discovery do not expose:

* The device's identity
* The identity of peers we are looking for

The list of peers to notify of changes has to be small because we are dealing primarily with battery powered device. They just don't have the power to advertise and sync with large numbers of peers. But the list can be dynamic. For example, we might get a record that a certain peer is permitted to see and another is not. In that case we would notify one peer and not the other. So while the number of peers we will advertise for at any time is limited the set of potential peers we could be advertising for is larger (but still not huge).

# Discovery beacons
The way we handle discovery is by advertising a binary string. The full spec is available [here](http://thaliproject.org/PresenceProtocolForOpportunisticSynching/) but a summary is:

```
DiscoveryAnnouncementWithoutVersionId = PreAmble 1*Beacon
PreAmble = PubKe Expiration
Expiration = 8-OCTET
PubKe = 88-OCTET
Beacon = 48-OCTET
```

PubKe MUST encode an Elliptic Curve Diffie-Hellman (ECDH) key using the secp256k1 curve. We use the secp256k1 curve, even though that curve is known to be slightly less secure than secp256r1, because the process for generating the curve is more open and thus less susceptible to subversion.

Expiration MUST encode a 64 bit integer, in network byte order, specifying the number of milliseconds since the epoch (1/1/1970 at 00:00:00 GMT). The value in the Expiration MUST encode the point in time after which the sender does not want the discovery announcement to be honored. When generating the expiration value we MUST add a small random offset to the current system time in order to prevent attacks based on observing the device's actual time. Note that it isn't clear what the attack would be here but it's easy to do.

Implementers MUST NOT honor discovery announcements with expirations that are too far into the future. It is up to each implementation to decide what constitutes "too far" but generally anything greater than 24 hours SHOULD be rejected.

Beacons are generated as given in the following pseudo-code:

```
function generateBeacons(setOfReceivingDevicesPublicKeys, Kx, IV, Ke, Expiration) {
  beacons = []
  UnencryptedKeyId = SHA256(Kx.public().encode()).first(16)

  for(PubKy : setOfReceivingDevicesPublicKeys) {
    Sey = ECDH(Ke.private(), PubKy)
    HKey = HKDF(SHA256, Sey, Expiration, 16)
    BeaconFlag = AESEncrypt(GCM, HKey, 0, 128, UnencryptedKeyId)
  
    Sxy = ECDH(Kx.private(), PubKy)
    HKxy = HKDF(SHA256, Sxy, Expiration, 32)
    BeaconHmac = HMAC(SHA256, HKxy, Expiration).first(16)

    beacons.append(BeaconFlag + BeaconHmac)
  }
  return beacons
}
```

The variables used above are:

__beacons__ - a byte array containing beacon content.

__Expiration__ - a 64 bit integer encoding the desired expiration date for the discovery announcement measured in milliseconds since the epoch (1 January 1970 00:00:00 UTC).

__GCM__ - Specifies Galois/Counter Mode for AES encryption.

__Ke__ - an ephemeral key public/private key pair.

__Kx__ - a public/private key pair for device x.

__PubKy__ - a public key taken from setOfReceivingDevicesPublicKeys.

__setOfReceivingDevicesPublicKeys__ - a set containing the public keys of the devices that the creator of the discovery announcement, device X, wants to synch with.

The functions used above are defined as follows:

__AESEncrypt(mode, key, IV, GCMHashSizeInBits, value)__ - Returns the AES encryption of the value using the specified mode, GCM hash size in bits and key.

__append(value)__ - Appends the given value to the array the function was called on. In this case we are appending a stream of bytes returned by the AES encryption.

__ECDH(private key, public key)__ - Generates an ECDH shared secret using the given public key and private key (which are assumed to be from the same curve).

__encode()__ - Returns a byte array with X.509 SubjectPublicKeyInfo encoding

__first(length)__ - Returns the first length bytes of the array the function was called on.

__HKDF(digest, IKM, salt, length)__ - Implements RFC 5869's HKDF function using the specified digest, IKM and salt. It will then return length number of bytes of keying material.

__HMAC(Digest, key, value)__ - Generates the HMAC of the specified value using the given digest and key.

__public()__ - The public key of a public/private key pair

__private()__ - The private key of a public/private key pair

__SHA256(value)__ - Generates the SHA-256 digest of the given value.

__+__ - When applied to two arrays it concatenates them together.

Note that the security of the protocol rests, amongst other things, on us never encrypting the same value for the same peer at the same time. In general this guarantee is easy to keep because our code limits how often we update beacons to a time period greater than a millisecond which is our clock accuracy that we use for the expiration value. But it's worth keeping in mind our dependence on the expiration value being unique when implementing the protocol. If we can't guarantee that then we will need to introduce a nonce that is sent along with the beacons.

A beacon string is parsed by reading in the expiration, making sure it isn't expired or too far in the future and then walking through each beacon. First we check the BeaconFlag to see if the GCM check passes. If it does then we validate the BeaconHmac to make prove who sent the beacon and their desire to talk to us. At that point, if we want, we can choose to contact the advertiser and sync data with them.

# Switching to TLS
Once the beacons have been downloaded and if a beacon is found for the device and the device wants to communicate with the sender then regardless of the actual transport (e.g. Bluetooth, Wi-Fi, etc.) a TLS connection is going to be established.

In Thali a user's identity is a public key so in theory we could now do a TLS mutual auth handshake. But TLS exposes the public keys in a handshake in the clear and since the public keys are the device's identity this would cause us to expose the device's identity.

The way we choose to work around this is to use PSK. Both sides will generate an identity using the following strategy:

```
PSK_Identity_Field = base64(preamble + beacon)
```

The actual K in PSK is generated as:

```
function generatePSK(PubKy, Kx, PSKIdentity) {
   Sxy = ECDH(Kx.private(), PubKy)
   return HKDF(SHA256, Sxy, PSKIdentity, 32);
}
```

The version of OpenSSL we use (1.0.1) doesn't have a lot of options for PSK:

*  'psk-3des-ede-cbc-sha',
*  'psk-aes128-cbc-sha',
*  'psk-aes256-cbc-sha',
*  'psk-rc4-sha',

Currently we are planning on using psk-aes256-cbc-sha.
