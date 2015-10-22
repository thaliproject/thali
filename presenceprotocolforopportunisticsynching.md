---
title: Thali local P2P presence protocol for opportunistic synching
layout: default
---
# Informally defining the problem
Bob creates a post on his phone. The only way for Bob to communicate this post is via local radios (BLE, Bluetooth and Wi-Fi) because there is no Internet infrastructure in the neighborhood. In other words, all we have are mobile ad-hoc networks. Bob addressed the post to Alice, among others. So Bob wants to determine if Alice and the other people on the post are around and if so send them the post. The only way for Bob to figure this out is using his local radios.

The challenge is that Bob is being harassed by someone in his business group and he doesn't want that person able to determine his location. In other words, he doesn't ever want any of his local radios to give away his location to the person.

So how can Bob figure out if Alice and the others are in the area without leaking his identity?

This is just a specific example from a [Bill of Rights I wrote up](http://www.goland.org/localdiscoverybillofrights/) for people using P2P presence. P2P presence is an awesome feature but it's scope for abuse is extensive. Only by designing our technologies to respect user rights can we hope to leverage P2P presence in a way that brings more benefit than harm.

# Formally defining the problem

The previous description is nice but it leaves out some critical details of the problem. So I'm going to try to define the problem a bit more formally here.

A given device x has the following characteristics:

__Kx:__ This is the public key that device x uses to identify its user

__Lx:__ A set of devices, identified by their public key, that the user of device X is willing to expose their presence to. The value of Lx for any x is a secret known only to the device. Under no circumstances is Lx ever (EVER) shared between devices.

__Mx:__ A set of devices, identified by their public keys, that Device X has been asked to synch data to.

__Target Set:__ The target set is defined as M<sub>x</sub> &cap; L<sub>x</sub>. This is the set of devices that Device x has been asked to deliver to that it is willing to deliver to. Any device in Mx that is not in the target set will not be contacted by Device x.

There is a fixed period of time starting at T1 and ending at T2 during which a device will try to deliver any messages it has to any devices in the target set.

The challenge is that messages can only be delivered point to point. That is, if Device x has a message for Device y then Devices x and y must form a direct wireless connection to transfer the information.

Devices find each other using local discovery. Local discovery only works over a finite distance and has a very low bandwidth (in the case of BLE the maximum data rate is 35 KB/s and often much less). For the sake of this problem we will assume that it is impossible for two devices to identify each other just by their BLE signal. That is, we are constantly randomizing the BLE device address and we have sprinkled magic pixie dust to prevent radio fingerprinting. So the only way two devices can discover each other's identity over local discovery is if they explicitly communicate it.

During the time window T2 - T1 a random set of devices will come within range of each other. There is no way to predict ahead of time who those devices will be and if any of them happen to be in a given device's target set. In addition there is no reliable way before time T1 for all devices, or even the devices in a particular device's target set to coordinate together. It's possible, for example, for device x to have an entry in Mx for a device it's literally never talked to before. This would happen, for example, if at some point before T1 Device x sent a message to Device y addressed to y and z. Device y has never seen Device z but now it has a message it needs to forward to z if it runs into that device.

The goal then is to figure out how to enable devices to discover if someone who is in their target set is within range without disclosing their identity to anyone who is not in the target set and without disclosing the contents of Lx beyond the obvious fact that if Device x and y talk then x must have been in Device y's target set and y must have been in Device x's target set.

# Overview of Thali discovery for opportunistic synchronization
The base scenario for this specification is that there are two or more devices who are at least occasionally within radio range of each other but do not otherwise have a means to communicate directly. When one of these devices has information it would like to synch with another device then the first device will try to discover the second device. In other words, for any device x there is a target set of other devices Tx which consists of all the other devices that device x would like to discover and synchronize data with.

Device x will use whatever discovery mechanism is supported by its radios, this can be BLE, Wi-Fi Direct, Bluetooth, etc. Device x will create a discovery announcement which will consist of a version ID, a pre-amble and one or more beacons. One beacon for each member of Tx. In other words, device X isn't going to announce its identity. Instead its going to announce, in a secure way, the identities of the devices it is looking for.

If device y, which is a member of Tx, receives device x's discovery announcement, identifies itself in one of the beacons and decides that it wishes to respond to the discovery request then device y will contact device x and negotiate a connection. The exact details of how the connection is to be negotiated varies based on radio type and will be defined below.

# Generating the pre-amble and beacons
A discovery announcement consists of three parts. A version Id, a pre-amble and one or more beacon values. How the version Id is transmitted is radio technology specific and will be defined below. This section of the document just defines the content of the pre-amble and beacon values which are the same regardless of transport.

The pre-amble and beacon are defined using Augmented BNF as specified in RFC 4234 with the exception that I changed the section 3.7 requirement on nRule to be n-Rule because 88OCTET look like 880 instead of 88:

```
DiscoveryAnnouncementWithoutVersionId = PreAmble 1*Beacon
PreAmble = PubKe Expiration
Expiration = 8-OCTET
PubKe = 88-OCTET
Beacon = 48-OCTET
```

PubKe MUST encode an Elliptic Curve Diffie-Hellman (ECDH) key using the secp256k1 curve. To avoid potential patent issues we will transfer the key uncompressed using the X.509 SubjectPublicKeyInfo encoding as defined in RFC 5480. This means that a key that should probably have required (256/8 + 1) = 33 bytes to send will instead require 88 bytes.

PubKe is an ephemeral public key that MUST be regenerated any time any part of the pre-amble or beacon list change.

__OPEN ISSUE:__ If we at least used a direct binary encoding we could reduce the size to 66 bytes (no point compression) or 33 bytes with point compression. I didn't do this because I wanted to use an encoding that was widely supported rather than create our own.

__OPEN ISSUE:__ I am asserting that the X.509 encoding (with point compression explicitly disallowed) is always 88 bytes long. But I haven't actually proven this.

Expiration MUST encode a 64 bit integer, in network byte order, specifying the number of milliseconds since the epoch (1/1/1970 at 00:00:00 GMT). The value in the Expiration MUST encode the point in time after which the sender does not want the discovery announcement to be honored.

Implementers MUST NOT honor discovery announcements with expirations that are too far into the future. It is up to each implementation to decide what constitutes "too far" but generally anything greater than 24 hours SHOULD be rejected.

Beacons are generated as given in the following pseudo-code:

```
function generateBeacons(setOfReceivingDevicesPublicKeys, Kx, IV, Ke, Expiration) {
  beacons = []
  UnencryptedKeyId = SHA256(Kx.public().encode()).first(16)

  for(PubKy : listOfPublicKeysToSyncWith) {
    Sxy = ECDH(Kx.private(), PubKy)
    HKxy = HKDF(SHA256, Sxy, Expiration, 32)
    BeaconHmac = HMAC(SHA256, HKxy, Expiration).first(16)
    
    Sey = ECDH(Ke.private(), PubKy)
    KeyingMaterial = HKDF(SHA256, Sey, Expiration, 32)
    IV = KeyingMaterial.slice(0,16)
    HKey = KeyingMaterial.slice(16, 32)
    beacons.append(AESEncrypt(GCM, HKey, IV, 128, UnencryptedKeyId) + BeaconHmac)
  }
  return beacons
}
```

The variables used above are:

__beacons__ - a byte array containing beacon content.

__Expiration__ - a 64 bit integer encoding the desired expiration date for the discovery announcement.

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

__slice(a, b)__ - Returns a sub-set of an array starting at byte a and end at the byte before b

__+__ - When applied to two arrays it concatenates them together.

# Processing the pre-amble and beacons
When a device receives a discovery announcement it has to validate several things.

If the device has seen the PubKe value previously then it MUST ignore the discovery announcement it has just received as a duplicate of a discovery announcement it has seen before. Therefore devices MUST have a cache to store PubKe values (or a secure hash therefore) they have seen before and SHOULD keep entries in the cache until the associated expiration time of the discovery announcement they were received on has passed. A should is specified versus a must in recognition of the fact that the cache may have to be purged due to space issues.

If a device hasn't previously seen the PubKe then it MUST validate that the PubKe is from the appropriate curve or it MUST be ignored.

A device MUST also validate the expiration. If the expiration defines a time in the past or a time too far into the future then the receiver MUST ignore the discovery announcement.

The receiver then parses through the beacon values included in the announcement. Each beacon value is processed as follows:

```
function parseBeacons(beaconStream, addressBook, Ky, PubKe, Expiration) {
   while(beaconStream.empty() == false) {
    encryptedBeaconKeyId = beaconStream.read(48)
    Sey = ECDH(Ky.private, PubKe)
    KeyingMaterial = HKDF(SHA256, Sey, Expiration, 32)
    IV = KeyingMaterial.slice(0,16)
    HKey = KeyingMaterial.slice(16, 32)
    UnencryptedKeyId = AESDecrypt(GCM, HKey, IV, 128, encryptedBeaconKeyId.slice(0, 32))
    
    if (UnencryptedKeyId == null) { // GCM mac check failed
       next;
    }
    
    PubKx = addressBook.get(UnencryptedKeyId)
    
    if (PubKx == null) { // Device that claims to have sent the announcement is not recognized
      return null; // Once we find a matching beacon we stop, even if the sender is unrecognized
    }
    
    BeaconHmac = encryptedBeacon.slice(32, 48)
    Sxy = ECDH(Ky.private(), PubKx)
    HKxy = HKDF(SHA256, Sxy, Expiration, 32)
    if (BeaconHmac.equals(HMAC(SHA256, HKxy, Expiration).first(16)) {
      return UnencryptedKeyId;
    }
   }
   return null;
}
```

Where variables or functions used above have the same names as in the previous section then they have the same functionality. Below are only listed variables and functions that do not appear above.

The new variables are:

__addressBook__ - A dictionary object whose key is the SHA256 hash of the X.509 SubjectPublicKeyInfo serialization of a public key and whose value is an object representing that public key.

__beaconStream__ - A byte stream containing all the beacons in a discovery announcement concatenated together.

__Ky__ - The public/private key pair for the device parsing the discovery announcement.

__PubKe__ - The de-serialized ephemeral public key from the discovery announcement.

The new functions are:

__AESDecrypt(mode, key, GCMHashSizeInBits, value)__ - Returns the AES decryption of the value using the specified mode, GCM hash size in bits and key.

__empty()__ - Specifies if a stream still has remaining content.

__get(k)__ - Returns the value associated with the submitted key, null if there is no associated value.

__read(n)__ - Reads n bytes from a stream.

# Transfering from notification beacon to TLS
This spec will define a number of bindings of the notification framework to various transports such as BLE, Bluetooth, Wi-Fi, etc. In each and every case once discovery has occured, notification beacons have been validated and a desired peer discovered the next step wll be the establishmen of a TCP/IP connection. This is true even when the underlying transport, such as Apple's Multi-Peer Connectivity Framework, doesn't natively support TCP/IP.

In the case of all bindings described in this spec the relationship between a peer and a particular TCP/IP address and port is transitory. The bindings will change over time. This introduces a security issue known as the channel binding problem. Put simply, when someone connects to a particular IP, how does one know that the entity at the other end of the TCP/IP connection is who we think it is? Thali generally uses TLS to solve this particular problem.

The most straight forward way to use TLS in this scenario is TLS mutual auth. The peer that received the notification presents its public key as a client cert, the peer that sent the notification presents its public key on the listening socket as a server cert, we do a handshake and it's good. Everyone is authenticated.

The problem is that TLS mutual auth exposes the public keys of the participants in the clear. So if we use TLS mutual auth then we force users to expose their identities to anyone in the area any time they connect.

To work around this problem we plan on using a TLS feature known as "Pre-Shared Key" or  defined in [RFC 4279](https://tools.ietf.org/html/rfc4279). PSK assumes that somehow the TLS client and TLS server have a pre-existing key that they have negotiated out of band. TLS then provides a mechanism by which the TLS client and TLS server can securely negotiate a connection using that pre-shared key.

Thali clients that establish connections based on discovery via notification beacons SHOULD establish TCP/IP connections to the discovered peer using TLS with the `DHE_PSK_WITH_AES_256_GCM_SHA384` and MUST NOT use a weaker cipher suite. A new Diffie-Hellman private key MUST be generated for each handshake.

The previous cipher suite is selected because:

* It uses a Diffie Hellman key exchange in order to provide for perfect forward secrecy
* AES 256 is considered secure against even theoretical quantum computing attacks (how's that for famous last words?)
* GCM provides protection against various types of data insertion and manipulation attacks

If a Thali peer receives a pre-amble and a set of beacons and determines that one of the beacons is intended for itself and if the Thali peer wishes to communicate with the peer who sent the beacon then the Thali peer MUST set aside the pre-amble and the specific beacon from the set of discovered beacons that was targetted at it. The Thali peer MUST then establish a TCP/IP connection using the binding specific mechanism to the Thali peer that sent the beacon. The Thali peer MUST then establish a TLS connection on top of the TCP/IP connection using the previously defined PSK cipher suite.

When creating a TLS PSK connection, the Thali TLS client MUST include the following value in the PSK identity field of the ClientKeyExchange message: base64(pre-amble + beacon). That is, the pre-amble and the beacon put aside in the previous requirement are combined together and then `base64` encoded as defined in [RFC 4648 section 4](https://tools.ietf.org/html/rfc4648#section-4). Note that the base64 encoding is introduced to meet the UTF-8 encoding requirement for PSK identity fields specified in section [5.1 of RFC 4279](https://tools.ietf.org/html/rfc4279#section-5.1).

__Note:__ In theory it would probably be a good thing if the PSK identity field was obscured. That is, rather than advertising in the clear which beacon is being responded to the Thali TLS client could encrypt the pre-amble and beacon using the Thali TLS server's (known) public key. However it's unclear in practice how useful this really is. Obviously any eavesdropping party can see who is talking to whom. Which beacon was used to establish that communication so far only seems useful in one particular attack. This attack depends on a feature we have not yet implemented in Thali that we call the personal mesh. With the personal mesh one can have many devices that all recognize a set of keys as belonging to themselves. So let's say that at Time A peer Alpha sends out a beacon B that is received by peer Beta on device DB1. Peer Beta records beacon B and will never respond to it again on device DB1. But the key used in beacon B is also recognized by device DB2 which is part of peer Beta's personal mesh. But at Time A devices DB1 and DB2 are not in communication so DB2 doesn't know that DB1 has responded to beacon B. An attacker could therefore observe that peer Beta has responded to beacon B and thus could start to replay that beacon in different places that the attacker thinks DB2 is. DB2 would also recognize the beacon and respond, thus revealing its identity. Encrypting the beacon in the PSK identity field makes this attack harder but not all that much harder. Presumably the set of beacons that peer Alpha originally sent out isn't that large. So an attacker could just repeat all the beacons and known that anyone who responds is at least a friend of Alpha's and quite likely a defined of peer Beta. So it just doesn't seem worth the effort to encrypt the value given that it doesn't really stop the attack.

When terminating a PSK TLS connection a Thali TLS server MUST NOT send a PSK identity hint in the ServerKeyExchange message. The hint is not necessary because the client's hint provides all the necessary binding.

When a Thali TLS server receives a PSK identity it MUST base64 decode the value and confirm that the pre-amble and beacon come from a reasonably recent advertisement by the Thali peer. If the value is not recognized or cannot for some reason be validated then the Thali peer MUST respond either with `unknown_psk_identity` or `decrypt_error` and terminate the TCP/IP connection.

__Note:__ In theory it would probably be a good thing if the Thali TLS server who receives an unrecognized or otherwise unacceptable PSK identity field from a client responded with a `decrypt_error` rather than an `unknown_psk_identity`. The benefit of such an approach is that if the attacker does not have radio triangulation capability or radio fingerprinting capabilities then it would make it harder to bind a particular device's discovery channel with its high bandwidth communication channel (when those things are actually separate). But in practice at least the high bandwidth channel tends use a fixed address (think Bluetooth) and we pretty much must assume that attackers at least have triangulation capabilities. That is why, for example, we transmit the Bluetooth address in the clear during BLE advertising for Android. However if it should turn out that it is useful to hide the relationship of the discovery and high bandwidth channels then we can always change our BLE design to use characteristics and communicate the Bluetooth channel address securely. Note that none of this will help a wit with Wi-Fi based discovery where the discovery and high bandwidth channels are identical.

Both the Thali TLS client and Thali TLS server need to generate the same PSK value. They will do so using an algorithm that matches the following pseudo-code:

```
function generatePSK(PubKy, Kx, PSKIdentity) {
   Sxy = ECDH(Kx.private(), PubKy)
   return HKDF(SHA256, Sxy, PSKIdentity, 16);
}
```

In the case of the Thali TLS client PubKy represents the public key confirmed for the Thali TLS server from the decrypted beacon and Kx is the Thali TLS client's own public/private key pair. The PSKIdentity is the base64 encoded value the Thali TLS client will send as the PSK identity value. The functions used in the pseudo-code work as previously defined. The returned 16 octet value is the value to be used as the PSK in the TLS PSK connection.

In the case of the Thali TLS server PubKy represents the public key associated with the beacon sent in the PSK Identity and Kx represents the Thali TLS server's own public/private key pair. The PSKIdentity value is the base64 encoded string sent by the Thali TLS client as the PSK identity value.

## Node.js API for adding TLS PSK support

Node 10 and above do not support OpenSSL's PSK support. Therefore we have to extend the existing Node.js TLS object to enable such support.

### tls.createServer

We propose adding a new option to tls.createServer, `PSKCallback`. This option takes as its value a function which will receive a UTF-8 string as input.

`PSKCallback` will only be called if the TLS connection negotiates a `PSK` cipher suite. Otherwise it is ignored.

If the synchronous response to the `PSKCallback` is a Node.js buffer object with at least one byte then that buffer's content MUST be used the PSK on the TLS connection. If the response is either not a buffer object or is an empty buffer object then the PSK connection MUST be rejected with a TLS `decrypt_error`.

If a PSK cipher suite is to be used with the TLS connection (either because it was passed in using the ciphers option or because OpenSSL was otherwise configured that way) and if `PSKCallback` is either undefined or not a function object then all PSK connections MUST be rejected with a TLS `decrypt_error`.

If a PSK cipher suite other than those defined below in the supportedPskCiphers is used then the connection MUST fail with `decrypt_error`. This last restriction is just laziness. We don't want to take on the burden at this time of testing the other PSK cipher suites.

### tls.connect

We propose adding a new option to the tls.connect options object, pskConfig.

```Javascript
/**
  * @readonly
  * @enum {string}
*/
var supportedPskCiphers = {
  DHE-PSK-AES128-GCM-SHA256: "DHE-PSK-AES128-GCM-SHA256",
  DHE-PSK-AES256-GCM-SHA384: "DHE-PSK-AES256-GCM-SHA384"
}

/**
  * @typedef pskConfig
  * @type {object}
  * @property {supportedPskCiphers} cipher
  * @property {buffer} psk
  * @property {string} pskIdentity
*/
```

If pskConfig is set to null, if any of the required properties are missing, if any of the required properties do not have the specified type (including the enum binding for cipher), if the psk buffer is empty or if the pskIdentity string is null then the pskConfig value MUST be ignored.

If the pskConfig option is used with pfx, key, passphrase, cert or ca then pskConfig MUST be ignored.

If the pskConfig option is not ignored then the TLS client connection MUST use the specified cipher, must submit the specified pskIdentity and must use the submitted psk value with the connection. If the server should reject any of these choices then an appropriate clientError MUST be emitted.

Any `decrypt_error` or `unknown_psk_identity` TLS errors MUST be returned to the clientError event as an `error` object with the message "decrypt\_error" or "unknown\_psk\_identity" as appropriate.

### Hints on implementing PSK in node.js

[This project](https://bitbucket.org/tiebingzhang/tls-psk-server-client-example) has stripped down examples of using PSK on the client and server side. This provides some concrete guidance as to what OpenSSL APIs we need to call.

From a node.js perspective the fun starts [here](https://github.com/jxcore/jxcore/blob/master/lib/https.js) since that is the actual interface we need to support for PouchDB but very quickly the venue will change to [here](https://github.com/jxcore/jxcore/blob/master/lib/tls.js). From there I believe you end up [here](https://github.com/jxcore/jxcore/blob/master/src/wrappers/node_crypto.cc). From there you can finally just pull out your favorite C IDE and start to navigate around.

# Transfering discovery beacon values over HTTP
Many of our bindings need to move the pre-amble and beacon values over a TCP/IP connection. When required we use HTTP (not HTTPS) as the transport. Specifically, the IP address and port will be discovered using whatever mechanisms are specified by the binding. But at the other end of that IP address and port is a HTTP server which supports unauthenticated/unencrypted GET requests to the endpoint /NotificationBeacons.

If the Thali peer is not currently advertising any notification beacons (this should only occur during a race condition as normally a request to /NotificationBeacons results from an advertisement that such values exist) then a GET request MUST be responded to with a 204 No Content.

Otherwise the Thali peer MUST respond with the content-type application/octet-stream, cache-control: no-cache and a value that contains the pre-amble and beacons as previously defined encoded in network byte order.

__Note:__ We currently don't bother with etag support as we generally try to avoid polling and the returned values are small anyway. But we can obviously add this in later if it proves useful.

# BLE Binding
For our current functionality the ideal mode would be `ADV_NONCONN_IND` and we would use the AdvData to transmit the information we need to send. However Android doesn't appear to support this mode and anyway we eventually have to switch to `ADV_IND` in order to support using BLE as a discovery transport for iOS background operations (more on that later).

So we will use `ADV_IND`. We do not currently define any `ScanRspData` values for `SCAN_RSP` responses to `SCAN_REQ` PDUs. We also do not currently define any characteristics. We will define characteristics later for reasons explained below.

Our BLE service UUID is: `b6a44ad1-d319-4b3a-815d-8b805a47fb51`

The format of our `ADV_IND` PDU is:
AdvA - The random device address
AdvData - We define this field using [RFC 4234](https://www.ietf.org/rfc/rfc4234.txt) Augmented BNF as previously defined above:

```
AdvData = Flags ServiceUUID ServiceData

Flags = FlagSize FlagGapType FlagValue
FlagSize = OCTET
FlagGapType = %x01
FlagValue = OCTET ; This value is set by BLE stack, not us

ServiceUUID = ServiceUUIDSize ServiceUUIDType ServiceUUIDValue
ServiceUUIDSize = OCTET
ServiceUUIDType = %x06
ServiceUUIDValue = b6a44ad1d3194b3a815d8b805a47fb51

ServiceData = ServiceDataSize ServiceDataType ServiceDataValue
ServiceDataSize = OCTET
ServiceDataType = %x16
ServiceDataValue = BluetoothAddress / iOSDevice

BluetoothAddress = BluetoothFlag BluetoothValue
BluetoothFlag = 0x00
BluetoothValue = 6-OCTET

iOSDevice = 0x01
```

The payload of an advertising packet is 31 octets. This field is made up of AD structures which consist of three parts, a 1 octet length field, an AD type and AD Data. Generally advertisements consist of at least two AD structures. The first is for flags and the second is to advertise our service UUID. The flag field takes up a total of 3 bytes, 1 for length, 1 for type and 1 for the actual flag values. ServiceUUID takes up 18 bytes, 1 for length, 1 for type and 16 for the 128 bit UUID. So this leaves us with only 10 bytes that we control. We will advertise service data which means our overhead for length and type is 2 bytes leaving us with 8 bytes of actual data.

## BluetoothAddress
In theory once someone has discovered a Thali peripheral the next step would be to issue a connect and start to use characteristics to move the beacon values. In practice however we have found this to be problematic because we have had serious reliability issues with connecting over BLE on Android. It is quite common for connects to randomly fail and for the BLE stack to then become unresponsive for a minute or two afterwards. We have had no problems receiving BLE advertisements, just connecting.

Therefore we are starting with a conservative stance. We use BLE advertisements to find Android Thali peripherals but we then switch to Bluetooth in order to transmit the beacons. To make this switch we have to discover the peripheral's Bluetooth address. It so happens that a Bluetooth address is 6 octets long. This fits nicely into our 8 octets and explains the `BluetoothAddress` structure above.

## Binding TCP/IP to Bluetooth

Once the Bluetooth Address is discovered the central will switch to an insecure RFCOMM connection to the supplied Bluetooth Address. This creates a situation in which the central device becomes a Bluetooth client and the peripheral device becomes a Bluetooth server. Bluetooth connections a full duplex. The contents of the output stream from the Bluetooth client to the Bluetooth server will be a binary stream encoded using [multiplex](https://github.com/maxogden/multiplex). Each of those streams represents the contents of the output stream of a single TCP/IP connection. In other words the Bluetooth client can open multiple simultaneous TCP/IP connections to the Bluetooth server by taking the output streams for each TCP/IP client connection and sending them individually through multiplex which will then mux them into a single stream which will then be transmitted over the Bluetooth output stream. Each of the multiplex streams 

The central, once connected over Bluetooth should issue a HTTP GET request to the path /NotificationBeacons and will get back an application/octet-stream response which contains the preamble and beacons as defined above in network byte order.

### Notifying when beacons change
Once a Thali central finds a Thali peripheral and makes a request to /NotificationBeacons how does it know if the value of the notification beacons ever changes? After all, the peripheral might have new data for the central. How will this be discovered? If we were using characteristics then we could do a connect and use the notify functionality built into BLE. But for the reasons previously discussed we are not using characteristics.

Our solution depends on a behavior we have observed with Android. Whenever we stop and re-start a BLE peripheral Android appears to always give us a new BLE address. We therefore mandate that any time an Android Thali client changes the value of its beacons it must stop and re-start its BLE peripheral. This will give the peripheral a new random address. This will cause all the centrals to reconnect to /NotificationBeacons to get the new value.

## iOSDevice
Normally we do not use BLE for discovery with iOS. Instead we use the multi-peer connectivity framework whose binding will be described later in this document. However in order to enable iOS devices to be discovered in the background we also want to support BLE. However when an iOS device is in the background it can only communicate over BLE and so any further communication will have to occur using BLE characteristics. We will define the characteristics used to communicate beacon data in the future when we get closer to implementing this functionality.

# Multi-Peer Connectivity Framework (MPCF)
Apple's proprietary multi-peer connectivity framework has its own discovery mechanism that appears to run over both Bluetooth and Wi-Fi. Note however that iOS's implementation of Bluetooth uses a proprietary extension that requires having a public key cert pair signed by Apple. And multi-peer connectivity's use of Wi-Fi appears to use a proprietary variant of Wi-Fi Direct. In any case, Multi-Peer Connectivity only works with Apple devices (either iOS or OS/X).

MPCF starts off by advertising via `MCNearbyServiceAdvertiser` the types of sessions that the device is willing to join. The advertisement consists of a peer ID, an info object made up of key/value pairs and a serviceType which can be between 1-15 characters long. Each key/value pair in the info object cannot be longer than 255 bytes. The total size of the info object cannot be more than 400 bytes (so it will fit into a single Bluetooth packet).

It's hard to be sure what the exact maximum size of a MPCF announcement is. But given that info tops out at 400 bytes one would be reasonable to assume that peerID and service type are both less. The point then being that the announcement mechanism is not the best way to discover the full beacon string which can easily be 1K or more.

Another factor to consider is that MPCF is only available (mostly) when the Thali app is in the foreground. This is a limited period so battery consumption is not that big a deal. As such we will use the same approach as BLE on Android.

Peer ID - A GUID that MUST be randomly generated whenever the app is activated or comes out of sleep.
serviceType - org.thaliproject.mpcf.announcement
Info - None

Upon discovering a Thali resource the searcher will then establish our fantastically complex stream based network connection that eventually tops out into TCP/IP links and make a request to the /NotificationBeacons endpoint as described in the previous section.

I'm not going to even bother to fully describe how we used paired streams with default names to create MPCF connections. It would take a long time and one sincerely doubts that many people will ever successfully reproduce it (sometimes we aren't sure we can reproduce it). But such as the joys of MPCF. For details feel free to dig through the Objective-C code.

The searcher is free to use a polling mechanism to detect any changes. Polling every 500 ms should be fine. This will provide an experience for the user of 'immediate' response if someone they are in the room with should happen to update something. Again, battery is not the primary concern given the limitations on MPCF usage.

For now we won't worry about etags as a way to shortcut requests if the beacon string hasn't changed. It's a nice optimization we can add in later.

# Local Wi-Fi Binding
The current plan is to use SSDP for local Wi-Fi discovery. This assumes that the local Wi-Fi access point allows both multicast as well as unicast between nodes on the network. That is not a safe assumption. But where it works we'll try to use SSDP. The main reason for picking SSDP over mDNS is simplicity. SSDP is a dirt simple text based protocol so it's very easy to deal with. If anyone has a super good reason why we should use mDNS instead we can switch.

When a Thali peer comes onto a Wi-Fi network it must send a ssdp:alive message and repeat it every 500 ms while the application is being actively used and every minute when the application is in the background if using a power constrained device.

The ssdp:alive message will use the following header values:
NT: http://www.thaliproject.org/ssdp
USN: A UUID URL whose value MUST be changed every time the beacon string is changed
Location: A HTTP URL pointing to the device's IP address and port over which a HTTP GET request for /NotificationBeacons can be accepted
Cache-Control: max-age = 60

If the server goes offline in a clean way then it must send a SSDP:byebye message using the same header value as given above. specifically NT and USN.

In general SEARCH requests should be used sparingly. Given that services are regularly announcing themselves anyway the only real purpose of SEARCH is to find devices that have gone into a sleep mode and aren't announcing themselves regularly. So in general a SEARCH request should only be issued when the Thali peer joins the network and then very infrequently there after, say no more than once every 5 minutes or so. The S header is to be set to the USN value as defined above. The ST header is set to the NT value above. The SEARCH response must contain a location header with a HTTP URL set as decribed above for the responding Thali peer. The SEARCH response must also use the same Cache-Control: max-age as defined above.

After getting the location header, either via ssdp:alive message or SEARCH response the Thali peer must then issue a GET request to /NotificationsBeacons as described in previous sections.

Any time the value of the NotificationBeacons endpoint changes a new USN must be generated and a new ssdp:alive message sent out. This will automatically cause all the Thali endpoints in range to get the new value.

The relatively short max-age used on requests means that caches will not overflow due to all the 'new' devices constantly showing up on the network.

The SSDP client must be smart enough to realize if it is in a situation where multi-cast is allowed but unicast between peers on the network is not. If every unicast request to addresses discovered via SSDP are being rejected then the SSDP client must stop making announcements for some reasonable period of time.

The obvious problem with SSDP (or mDNS for that matter) is that if the local multicast domain is large enough then all the traffic generated by a large enough number of Thali clients could get quite ugly. Imagine a conference hall with a single multi-cast domain with 10,000 people in it. Now, to be fair, this is unlikely. Most Wi-Fi APs used outside the home are not configured to allow multicast much less point to point communication. At a minimum each Thali peer will need to initially make 10,000 /NotificationBeacons requests. And process them! In general battery based devices have absolutely no business trying to operate in such a scenario. Therefore Thali peers need to be configured with some maximum number of devices they are willing to discover. When that maximum is exceeded the device needs to shut down SSDP for some period of time. The cost here btw is not just the messages traffic (10,000 devices all issuing GETs against each other translates to roughly 10000^2 = 100,000,000 requests) but also the cost of trying to validate all the beacons!

# Q&A
## Why can't users just announce their keys publicly? Maybe encrypt them with a group key?

Looking at our informal definition we can imagine that one way for Bob to find Alice is for Alice to just announce her public key over BLE. In fact, if everyone would just announce their public keys over BLE everything would be so much simpler! Everyone can see exactly who is around, figure out if they have something for them and then transmit it. The problem is that now everyone is announcing their identity to anyone who will listen.

The usual work around at this point is some kind of group key. For example, imagine that what we announce is someone's personal key but first we encrypt it with some group key that everyone at work has a copy of. This way locations will only be understandable to employees, nobody else will know who is announcing what.

The problem of course is that both Bob and his harasser are employees of the company and so the harasser will have the group key and be able to discover Bob.

There are variants on the group key approach but they tend to fail for reasons having to do both with the ad-hoc nature of communication in our scenario as well as for space reasons.

For example, imagine that Alice creates a new discussion group with Bob and they agree on a group key just for that discussion. Later on Alice decides to add Bob's harasser to the discussion group and shares the group key with the harasser. Since this is an ad-hoc networking scenario there is absolutely no guarantee that Alice ever had the opportunity to tell Bob about this. And presumably Alice has no idea that Bob has put a ban on his harasser. So if Bob just looks for the group key, thinking it's Alice, he could be inadvertently advertise his location to his harasser.

And, of course, our discovery channel is quite limited. So if there are lots and lots of groups there won't be enough bandwidth to announce them all.

## Code anyone?
[https://github.com/yaronyg/cryptopresencetest](https://github.com/yaronyg/cryptopresencetest)

The repro has a reference implementation of the discovery announcement. It is not as well tested as it should be but it gives the idea. It also contains a bunch of hacked together perf tests I used to learn how APIs worked and to test out different approaches. See the readme for details.

## HMAC-MD5 vs HMAC-SHA256
I started this work out just using HMAC-MD5 both because of size but more importantly because of performance. CPU is at a premium. I would also (naively) argue that most of the attacks on MD5 wouldn't work properly in our scenario anyway because of the limited data size. But using SHA256 would make it easier for the crypto deities to be happy with us and it's not really a size issue since we can just truncate the hash. But what about perf? Given that we are using the AES-GCM approach perf really isn't that big a deal to be honest. We just aren't using that many hashes. But for the 'hot list' approach the perf really matters since we have to do a lot of hashing.

So I ran some quick tests. In the first test I checked 20 hashes against an address book with 1000 keys where all comparisons fail. I repeated the test 100 times.

|Algorithm | Min | Median | Max|
|----------|-----|--------|----|
|HMAC-MD5  |131 ms | 136 ms   |230 ms |
|HMAC-SHA256 | 152 ms | 179.5 ms | 454 ms |

For the next test I generated 20 HMACs using 20 different keys. This simulates creating beacons in an announcement.

|Algorithm | Min | Median | Max|
|----------|-----|--------|----|
|HMAC-MD5  |1 ms | 2 ms   |26 ms|
|HMAC-SHA256 | 2 ms | 3 ms | 26 ms|

The perf difference for generating tokens is noise. The slightly bigger issue is with validating them where using SHA256 looks to reduce performance by roughly 30%. That is small enough (and my tests are ham fisted enough) that I suspect the real world perf difference will be noise. So let's make the crypto gods happy and just use SHA-256 with a truncated output to 128 bits to save space on the wire.

## Comparing Addressbook Size Invariant Options - HMAC vs HMAC-AES-CBC vs AES-GCM
E(HMAC)xy = Ke + Timestamp + (HMAC-SHA256(Sey, Timestamp) + HMAC-SHA256(Sxy, Timestamp))+

E(HMAC-AES-CBC)xy = Ke + Timestamp + IV + (HMAC-SHA256(Sey, IV + Timestamp) + AES-CBC(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp)))+

E(AES-GCM)xy = Ke + Timestamp + IV + (AES-GCM(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp))+

I put the IV into the HMAC's (where there was an IV) on the advice of Tolga Alcar who suggested this would add some additional randomness to the HMAC-SHA256 output. Since it's cheap to do and doesn't increase the size of the output this seemed reasonable.

Tolga also said that using the ECDH values (for Sey and Sxy) directly is a bad idea. That the output secret is effectively a point on the curve and that the is a non-uniform space (e.g. the points are predictably on the curve). He suggested looking up NIST SP800-108 and looking for the simplest possible hash based KDF and setting its counter to 1 and using that. Effectively just hashing the ECDH secret so that we map it into a uniform space from a non-uniform space. Bouncy Castle supports KDFCounterBytesGenerator so I can add that in for testing purposes. Later on I actually switched to HKDF mostly because I could actually read and understand its spec in a way that eluded me with the NIST document. It also claims to be more secure.

In each of the three options the first thing we have to do is use the ephemeral key with the receiver's key to generate an Ellptic Curve Diffie Hellman key. For the performance test we used secp256k1 as our curve. We picked this solely because it was a 256 bit key thus providing roughly AES 128 equivalent strength and it was supported by bouncy castle on Android which was our test environment. The test device is a Nexus 7 phone running Android 4.4.4.

In each case the ECDH key is generated once regardless of how many identities are advertised in the presence announcement. That same key is then used to try to check each value. To test how expensive generating this ECDH key is I generated one key per test run and ran the test 1000 times. The cost to generate a single ECDH key was:

| Min | Median | Max |
|-----|--------|-----|
| 18 ms | 25 ms | 64 ms |

So we know that right away the overhead for each of these approaches in time is around lets say 30 ms per announcement. Just keep in mind that it doesn't matter how big the announcement is because this cost is fixed regardless of how many entries are in the announcement.

In the size table below we will ignore both Ke and Timestamp since they are the same for all options.

| Approach | Fixed Sized Element | Size for each announced identity |
|----------|---------------------|----------------------------------|
| HMAC     | 0                   | 16 + 16 = 32 bytes               |
| HMAC-AES-CBC | 16              | 16 + (32 bytes unencrypted -> 48 bytes encrypted) = 64 bytes |
| AES-GCM  | 16                  | 32 bytes unencrypted -> 48 bytes encrypted |

To compare each approach I generated 20 entries in a single presence announcement. The last entry will match against the receiver. The receiver has an address book with 10,000 entries in it. The only reason for even specifying the number of entries is to give us a meaningful comparison between the cost of the HMAC approach versus everything else. We will intentionally match on the last entry in the address book to provide the worse case scenario. Each test was run 100 times except for the HMAC tests which I ran 10 times because I'm impatient.

| Approach | Min | Median | Max |
|----------|-----|--------|-----|
| HMAC/HMAC| 1691 ms | 1694 ms | 1741 ms |
| HMAC/AES-CBC| 22 ms |  36 ms |  73 ms |
| AES-GCM | 24 ms   | 52 ms | 59 ms |

## What about ECIES?
E(ECIES)xh = Ke + Timestamp + IV + (ECIES(Sey, SHA256(Kx) + HMAC-SHA256(Sxy, IV + Timestamp))+

One can reasonably argue that AES-GCM and ECIES are morally equivalent. And I would expect them to produce outputs of comparable size and processing time.

But in practice that doesn't seem to be the case. For example, the ECIES output I generate in my sample code is 133 bytes as opposed to the 48 bytes generated by AES-GCM. But I strongly suspect that this isn't a fair comparison because I'm pretty sure my configuration for ECIES is wrong. I don't think I'm using comparable hash functions or KDFs. My suspicion is that with the right configuration AES-GCM and ECIES would produce similar results.

But I have to admit my limits. I found hacking through the Bouncy Castle code really painful. Docs and examples are few and far between and out of date. I found one [test file](https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/jce/provider/test/ECIESTest.java) and one [mail thread](http://bouncy-castle.1462172.n4.nabble.com/ECC-with-ECIES-Encrypt-Decrypt-Questions-td4656750.html) that even discuss ECIES. Not much else.

So my current thinking is basically that ECIES is not a standard, it's not supported in the same way in different environments, the Bouncy Castle implementation is tricky and the AES-GCM approach seems to work well using widely available crypto primitives. So my plan is to bring both ECIES and AES-GCM to the crypto gurus at work and let them tell me what they think I should do.

## Hot List
Exy = Timestamp + (HMAC-SHA256(Sxy, Timestamp))+

Imagine if someone has a relatively small address book. In that case someone could go through one time and generate Sxy for every person in the address book. Then when they receive a discovery announcement that just contains HMAC-SHA256 hashes of the timestamp they could very quickly check all the beacon values to see if they matched. I ran some tests (HashAndHmacTest.java/testCheckHashesAgainstAddressBook) and was able to check 20 hashes against an address book with 1000 entries in 131/187.5/596 (min/median/max). Which doesn't sound great until you remember that most people, most of the time, won't be working with more than say 10 or 20 people. So let's imagine an address book that is based on [Dunbar's number](http://en.wikipedia.org/wiki/Dunbar%27s_number) (150). It's what I call the "Hot List". You would have your 'full address book' which could have an unlimited number of people. But you could also have a "hot list" of just people you frequently interact with. Those people could just send their hashes. We would only need 16 bytes per hash plus 8 bytes for the Timestamp. This would seriously shrink the size of our announcement. With the GCM approach announcing 20 identities requires 88 + 8 + (20 * 48) = 1056 bits. With a pure hash approach you would need 8 + (8 * 20) = 168 bits! That's a massive difference. So running the same test with 150 people in the address book and 20 identities we get 20/28/99. That is essentially the same perf as the AES-GCM approach but with 6 times fewer bytes on the wire!

I did think about using Bloom Filters as well. In other words someone sending out an announcement using hashes could instead of including the hash just enter the hash into a bloom filter and send out the bloom filter. Then the person receiving the announcement could generate all the hashes and match them against the announced bloom filter. If I did the math right then a bloom filter that could hold 20 entries with a 1% false positive rate would need 192 or so bits. Which is longer than just sending out the hashes directly. I even tried a bloom filter that could hold the whole address book, say 150 entries with a 1% false positive rate. That required 1438 bits. Compare that to the hash which would be 8 + (8 * 150) = 1208 bits. So Bloom Filters only save space (at the cost of extra calculations) if we are willing to tolerate a higher error rate (which then wastes space due to unnecessary handshakes due to false positives). I'll revisit this math later to make sure I didn't screw anything up but it doesn't look like bloom filters buy anything here.

As small as hot lists are there is a catch. If device X wants to find device Y and device X thinks it is on device Y's hot list but is not then its hash won't match even though device X might be in device Y's address book. So devices have to know if they are on each other's hot lists. And since hot lists have to have a maximum size there needs to be a way to get folks off the hot list. This means we need extra protocol cruft to let end points negotiate being on each other's hot lists and we need at least some kind of expiration mechanism to automatically remove folks from the hot list after some window of time. None of this is hard but it's just more stuff to do.

So my guess is that hot lists are a v3 optimization.

## Transitive Attacks
It's all nice and good to talk about protecting user privacy but we have to realize that in social networks protecting against other members of the network is somewhere between difficult to impossible. Our goal is to make harassers lives harder but realistically we cannot stop a determined enough harasser. For example, let's say that Bob's harasser is determined to be able to discovery Bob's physical location anytime he is within a few hundred feet. The harasser will quickly be able to determine that they are blocked by Bob because any time the harasser sends out a message to Bob and Bob is within range the harasser will notice that their device doesn't get connected to. 

In fact dealing with blocking co-workers or members of the community is complicated enough that one suspects that in practice Bob won't be able to use the filter list when his Thali app is in the foreground. That way if Bob and the harasser are working face to face then data will flow.

But once the harasser realizes they have been "de-friended" then the harasser still has options to discover Bob's location. For example, the harasser probably knows that Bob works a lot with Alice and the harasser can reasonably guess that Alice isn't blocked by Bob. In that case the harasser can create a message addressed to both Bob and Alice. Then the harasser can send the message directly to Alice. For bonus points the harasser can wait until Alice isn't advertising any beacons. As soon as Alice gets the message from the harasser the first thing Alice's phone will do is advertise a beacon for Bob in order to pass the message along. The harasser can now grab that token and use it to try to advertise for Bob.

We have put in some protections from this attack. For example, if Bob sees Alice's token before the harasser sends it then Bob won't honor the harasser's stolen token because the token will already be in Bob's cache. If, on the other hand, the harasser is able to use the token before Bob sees it from Alice then Bob will respond to it. No connection will be fully negotiated since the harasser would need Alice's private key for that. But just the fact that a connection was made gives away Bob's location. But remember, this only works once. Bob won't respond to the same token again.

So the attacks aren't super easy and they aren't super effective but they absolutely are possible. It's not clear that these kinds of attacks are even theoretically possible to stop in the absence of a mix or onion network. But mix/onion networks are not generally very usable in ad-hoc mobile networks with poor connectivity.

We still have obligations to do our best to protect users but we have to also recognize the limits of what is possible, especially in a social network where these sorts of transitive attacks are very possible.

And of course this is all without discussing the various IDs that Bob's phone is already send out (see the last two sections [here](http://www.goland.org/localdiscoverybillofrights/)). So beware. There is no security panacea.

## Are we guilty of bad public key hygiene?
The notification protocol and TLS binding require the use of a public key to identify both parties and then that public key is used directly for Diffie-Hellman key exchanges. In and of itself this isn't bad. What's bad is that currently those public keys are the root identity keys and so long lived. Typically good practice argues for having some kind of infrequently used root key which is then used to sign an intermediary key which is then used to sign a leaf key which is then used to sign an ephemeral key. Each key then rotates based on how low in the tree it is, the lower in the tree the more frequently it is changed. So we range from the root key at the top which is essentially invariant to the ephemeral key at the bottom which is changed on each use.

The notification protocol and TLS binding require the use of a public key to identify both parties and then that public key is used directly for Diffie-Hellman key exchanges. In and of itself this isn’t bad. What’s bad is that currently those public keys are the root identity keys and so long lived. Typically good practice argues for having some kind of infrequently used root key which is then used to sign an intermediary key which is then used to sign a leaf key which is then used to sign an ephemeral key. Each key then rotates based on how low in the tree it is, the lower in the tree the more frequently it is changed. So we range from the root key at the top which is essentially invariant to the ephemeral key at the bottom which is changed on each use.

The problem with this approach is that we are both space and processor constrained. In an ideal world the beacon’s encrypted value would actually be encrypted using a key derived not from the sender’s root public key but instead from the ephemeral key and inside the encrypted statement would be a key chain where the ephemeral key would be signed by the leaf key which would be signed by the intermediate key which would then be signed by the root key. Unfortunately this approach would vastly (by orders of magnitude) increase the size of the beacon. So it’s not workable over the types of battery/bandwidth/process constrained environments we need to run in.

What’s interesting is that while this approach would protect the notifier’s root key from being over used, it would not protect the peer being notified. After all the root of the discovery mechanism is a Diffie-Hellman key exchange with the public key of the peer to be notified. There is no way for the notifier to know ahead of time what keys below the root the peer to be notified is currently using. So the notifier can’t use those keys in the Diffie-Hellman key exchange.

Another, probably much more significant, problem with using the root keys is that this means that the root identity keys have to be physically present on the device and usable by a process that is network connected. This is usually considered less than an ideal because it means a security compromise, much more likely with a network connected process, can be escalated into a full identity compromise. In an ideal work the root identity key would either not be on the device at all (the device only using a time limited key chain from the root authorizing it to act on the user’s behalf) or at the very least not on a process that is anywhere near a network connection.

My current best guess is that the way we will address these issues is by using purpose specific notification keys. These keys are not the root identity key but instead are separate keys that are generated and then signed by the root keys and exchanged during identity exchange. The keys are time limited and the peers would need to occasionally do exchanges of updated notification keys. In the case of personal meshes it’s even possible (although clearly not ideal as it puts too much of a burden on others and leaks too much information about the user’s devices) that each device in the mesh would have its own distinct notification keys. In that case if user A wants to notify user B who has 5 different devices then user A might have to advertise 5 different notification beacons, one for each of user B’s devices. Imagine replicating this across 100 users and we just increased the number of notification beacons by a factor of 5. But the alternative is that all the devices in the personal mesh have to share the same key which means moving the notification private key across the wire, generally a no-no.
