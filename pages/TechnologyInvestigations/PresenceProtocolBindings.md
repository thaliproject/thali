---
title: Bindings for Non-TCP transports to TCP for Thali
layout: page-fullwidth
permalink: "/PresenceProtocolBindings/"
header:
   image_fullwidth: "header.png"
categories:
    - technologyinvestigations
---

# Bindings for Discovery and Connectivity
Thali provides a [specification](http://thaliproject.org/presenceprotocolforopportunisticsynching) that defines how to advertise notification beacons that are designed so that only the target of the beacon can determine who sent it. But that specification did not define how to advertise those beacons. That is the job of this specification.

It defines a set of bindings for three transports:

* Bluetooth Low Energy + Bluetooth on Android
* Multi-Peer Connectivity Framework on iOS
* Local Wi-Fi Access point based multicast discovery

In each case it shows how to use a local discovery mechanism to flag that new beacon values are available and then defines how to retrieve those values using TCP/IP. Two of the transports do not natively support TCP/IP and so this specification defines how to transport TCP/IP over them.

# Transferring from notification beacon to TLS
__Open Issue:__ See [here](https://github.com/thaliproject/Thali_CordovaPlugin/issues/229) but the basic problem is that there appears to be good reason to [believe](https://weakdh.org/) that 1024 bit DH groups as used in OpenSSL might be compromised. At the moment we choose to use a DH cipher suite because the ECDHE PSK cipher suites supported by OpenSSL uses AES with CBC mode and I've always preferred GCM because it provides integrity protection. But I need to talk to the crypto board and figure out what makes the most sense. Can we just configure OpenSSL to use a 2K key? Should we switch to ECDHE PSK even though this means giving up GCM? Research needs to be done.

This spec will define a number of bindings of the notification framework to various transports such as BLE, Bluetooth, Wi-Fi, etc. In each and every case once discovery has occured, notification beacons have been validated and a desired peer discovered the next step will be the establishment of a TCP/IP connection. This is true even when the underlying transport, such as Apple's Multi-Peer Connectivity Framework, doesn't natively support TCP/IP.

In the case of all bindings described in this spec the relationship between a peer and a particular TCP/IP address and port is transitory. The bindings will change over time. This introduces a security issue known as the channel binding problem. Put simply, when someone connects to a particular IP, how does one know that the entity at the other end of the TCP/IP connection is who we think it is? Thali generally uses TLS to solve this particular problem.

The most straight forward way to use TLS in this scenario is TLS mutual auth. The peer that received the notification presents its public key as a client cert, the peer that sent the notification presents its public key on the listening socket as a server cert, we do a handshake and it's good. Everyone is authenticated.

The problem is that TLS mutual auth exposes the public keys of the participants in the clear. So if we use TLS mutual auth then we force users to expose their identities to anyone in the area any time they connect.

To work around this problem we plan on using a TLS feature known as "Pre-Shared Key" or  defined in [RFC 4279](https://tools.ietf.org/html/rfc4279). PSK assumes that somehow the TLS client and TLS server have a pre-existing key that they have negotiated out of band. TLS then provides a mechanism by which the TLS client and TLS server can securely negotiate a connection using that pre-shared key.

Thali clients that establish connections based on discovery via notification beacons SHOULD establish TCP/IP connections to the discovered peer using TLS with the `DHE_PSK_WITH_AES_256_GCM_SHA384` and MUST NOT use a weaker cipher suite. A new Diffie-Hellman private key MUST be generated for each handshake.

The previous cipher suite is selected because:

* It uses a Diffie Hellman key exchange in order to provide for perfect forward secrecy
* AES 256 is considered secure against even theoretical quantum computing attacks (how's that for famous last words?)
* GCM provides protection against various types of data insertion and manipulation attacks

If a Thali peer receives a preamble and a set of beacons and determines that one of the beacons is intended for itself and if the Thali peer wishes to communicate with the peer who sent the beacon then the Thali peer MUST set aside the preamble and the specific beacon from the set of discovered beacons that was targeted at it. The Thali peer MUST then establish a TCP/IP connection using the binding specific mechanism to the Thali peer that sent the beacon. The Thali peer MUST then establish a TLS connection on top of the TCP/IP connection using the previously defined PSK cipher suite.

When creating a TLS PSK connection, the Thali TLS client MUST include the following value in the PSK identity field of the ClientKeyExchange message: base64(preamble + beacon). That is, the preamble and the beacon put aside in the previous requirement are combined together and then `base64` encoded as defined in [RFC 4648 section 4](https://tools.ietf.org/html/rfc4648#section-4). Note that the base64 encoding is introduced to meet the UTF-8 encoding requirement for PSK identity fields specified in section [5.1 of RFC 4279](https://tools.ietf.org/html/rfc4279#section-5.1).

__Note:__ In theory it would probably be a good thing if the PSK identity field was obscured. That is, rather than advertising in the clear which beacon is being responded to the Thali TLS client could encrypt the preamble and beacon using the Thali TLS server's (known) public key. However it's unclear in practice how useful this really is. Obviously any eavesdropping party can see who is talking to whom. Which beacon was used to establish that communication so far only seems useful in one particular attack. This attack depends on a feature we have not yet implemented in Thali that we call the personal mesh. With the personal mesh one can have many devices that all recognize a set of keys as belonging to themselves. So let's say that at Time A peer Alpha sends out a beacon B that is received by peer Beta on device DB1. Peer Beta records beacon B and will never respond to it again on device DB1. But the key used in beacon B is also recognized by device DB2 which is part of peer Beta's personal mesh. But at Time A devices DB1 and DB2 are not in communication so DB2 doesn't know that DB1 has responded to beacon B. An attacker could therefore observe that peer Beta has responded to beacon B and thus could start to replay that beacon in different places that the attacker thinks DB2 is. DB2 would also recognize the beacon and respond, thus revealing its identity. Encrypting the beacon in the PSK identity field makes this attack harder but not all that much harder. Presumably the set of beacons that peer Alpha originally sent out isn't that large. So an attacker could just repeat all the beacons and known that anyone who responds is at least a friend of Alpha's and quite likely a defined of peer Beta. So it just doesn't seem worth the effort to encrypt the value given that it doesn't really stop the attack.

When terminating a PSK TLS connection a Thali TLS server MUST NOT send a PSK identity hint in the ServerKeyExchange message. The hint is not necessary because the client's hint provides all the necessary binding.

When a Thali TLS server receives a PSK identity it MUST base64 decode the value and confirm that the preamble and beacon come from a reasonably recent advertisement by the Thali peer. If the value is not recognized or cannot for some reason be validated then the Thali peer MUST respond either with `unknown_psk_identity` or `decrypt_error` and terminate the TCP/IP connection.

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

# Transferring discovery beacon values over HTTP
Many of our bindings need to move the preamble and beacon values over a TCP/IP connection. When required we use HTTP (not HTTPS) as the transport. Specifically, the IP address and port will be discovered using whatever mechanisms are specified by the binding. But at the other end of that IP address and port is a HTTP server which supports unauthenticated/unencrypted GET requests to the endpoint /NotificationBeacons.

If the Thali peer is not currently advertising any notification beacons (this should only occur during a race condition as normally a request to /NotificationBeacons results from an advertisement that such values exist) then a GET request MUST be responded to with a 204 No Content.

Otherwise the Thali peer MUST respond with the content-type application/octet-stream, cache-control: no-cache and a value that contains the preamble and beacons as previously defined encoded in network byte order.

__Note:__ We currently don't bother with etag support as we generally try to avoid polling and the returned values are small anyway. But we can obviously add this in later if it proves useful.

HTTP clients making requests to /NotificationBeacons MUST put in place protections to ensure that the response is not excessively long.

HTTP servers offering /NotificationBeacons MUST put in place protections such that if the load of requests on the server becomes excessive the server will either filter out excessive requesters or if that is not workable then the server will disable the notification discovery system all together and not offer the server port for a period of time.

# Denial of Service (DOS) Protections for Discovery and Connectivity
In general it is impossible to stop DOS attacks in a local radio environment. There are just too many easy ways to block channels. However implementers do have a responsibility to mitigate the damage from DOS attacks to just being a loss of connectivity. It should not be possible to escalate such attacks to the point where they cause a loss of CPU, Storage or (usually as a result of the other two) battery.

In the case of network systems that effectively support push discovery (this particularly applies to BLE and Wi-Fi) the implementation MUST detect when it is receiving an excess number of discovery announcements and MUST disable discovery for a period of time before trying again.

Systems MUST also detect when they are receiving an excessive number of incoming connections over the high bandwidth transport (e.g. Bluetooth, Wi-Fi, etc.) and either filter out abusers if possible or shut down the notification discovery system all together for a time before trying again.

# Implications of Bluetooth and Multi-Peer Connectivity Framework's (MPCF) TCP/IP Binding approaches

## TCP/IP Framing and command packets are torn off
As will be explained below we relay TCP/IP content over our non-TCP/IP transports by connecting TCP/IP output streams to the non-TCP/IP transport's input streams and the non-TCP/IP transport's input streams to TCP/IP's output streams. 

But when we do this the only thing that gets moved is the data inside of the TCP/IP data packets. The data packet headers and the TCP control commands like FIN packets are not transmitted.

This means that when either the Bluetooth or MPCF code terminate their TCP/IP level connections then they MUST simultaneously terminate their non-TCP transport connections. If they do not do this then there is no way for the other side of the connection to know that the TCP link has been broken.

This also means that we are depending on the non-TCP transport connections to provide us with proper flow control as none of the TCP window sizing commands will be communicated across the wire.

And most interestingly, this means that the actual port that the client was intended to attach to won't be included in the connection. In a way this makes sense as how would a remote client know which port to connect to anyway?

## Thali's multiplexing layer is a mandatory requirement
In addition we only define a single TCP/IP connection that can be bound to the non-TCP transports. In general it is not useful to have only a single TCP/IP connection so we mandate that both the Bluetooth and MPCF bindings MUST always use the Thali [TCP Multiplexer](https://github.com/thaliproject/Thali_CordovaPlugin/blob/master/thali/tcpmultiplex.js) on top of the singleton TCP/IP connection in order to enable the establishment of multiple TCP/IP connections.

## Optimizations for noisy environments
For both the BLE and MPCF bindings the assumption is that when a node has something to notify they change their ID and this triggers everyone around to reconnect to them and pull down the new notifications. When there are a small number of devices around that works fine. But things can get ugly fast when there are very large number of devices. Imagine a conference or symposium with thousands of Thali apps shoved into a single room. Just pinging all the devices in the area, especially using non-TCP/IP transports, can take a very long time. Just setting up a Bluetooth connection can take several seconds, for example.

To a certain extent peer to peer just doesn't work super well in massively noisy environments. Enormous common multi-cast channels never tend to turn out well for anyone. You have to break the environment down into pieces and then try to set up routing points between them. But that is way, way out of scope for Thali.

Our general reaction to super noisy environments is to just shut down. Basically there is no difference for us between super noisy environments and a denial of service attack.

However, and this clearly violates user privacy, there are hacks. 

For example, let's say someone pulls out their Android Thali application and sends a message to someone. If the Thali app has previously talked to that person then it could record their Bluetooth address and try to just blind connect to the Bluetooth address. This costs several seconds but if the person is around and if there is enough bandwidth to talk then a Bluetooth connection can be established and communication can begin. This approach can be generalized where, when we are in a noisy environment, we switch from broadcast to point to point discovery and try to walk down the list of folks we want to notify. This only works because Android does not rotate Bluetooth network addresses. If it did then we couldn't use this shortcut. Note that this approach also works in the case that the Thali application didn't know the other application's Bluetooth address ahead of time but rather learns it via local discovery. Once the address is known, however that happened, it can be used to directly connect in the future.

A similar trick is possible with iOS if we are willing to play evil games with peerIDs. There are two variants of this game. The less evil variant is that when an application establishes a `MCSession` it will never end it until it goes into the background. That way if two peers find each other then at least they can keep talking until the app goes into the background. A slightly more evil variant is that a peer will pick a `peerID` that it will use for some fixed period of time (say 24 hours) and will always advertise. It would then advertise a second `peerID` (assuming iOS allows for multiple simultaneous `MCNearbyServiceAdvertiser` objects to exist) that would then rotate everytime the notification beacon changes. When the peer makes connection with another peer it can communicate its static `peerID` so that the other peer can try to establish a direct connection in the future. Ideally we would be able to get rid of the second `peerID` by putting notification beacon "updated" values into the info field. But this assumes that if we stop and restart advertising with the same `peerID` but different info objects that this change will be picked up by those around us and thus make it an effective notification mechanism.

# BLE Binding
For now we will use `ADV_NONCONN_IND` as our advertising PDU and leverage AdvData  to transmit the information we need to send. 

We do not currently define any `ScanRspData` values for `SCAN_RSP` responses to `SCAN_REQ` PDUs. We also do not currently define any characteristics. Instead we transfer all the data we need in the BLE announcement PDU. In the future however, when we want to work with iOS in the background, we will need to introduce characteristics.

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
BluetoothFlag = 0
BluetoothValue = 6-OCTET

iOSDevice = 1
```

The payload of an advertising packet is 31 octets. This field is made up of AD structures which consist of three parts, a 1 octet length field, an AD type and AD Data. Generally advertisements consist of at least two AD structures. The first is for flags and the second is to advertise our service UUID. The flag field takes up a total of 3 bytes, 1 for length, 1 for type and 1 for the actual flag values. ServiceUUID takes up 18 bytes, 1 for length, 1 for type and 16 for the 128 bit UUID. So this leaves us with only 10 bytes that we control. We will advertise service data which means our overhead for length and type is 2 bytes leaving us with 8 bytes of actual data.

## BluetoothAddress
In theory once someone has discovered a Thali peripheral the next step would be to issue a connect and start to use characteristics to move the beacon values. In practice however we have found this to be problematic because we have had serious reliability issues with connecting over BLE on Android. It is quite common for connects to randomly fail and for the BLE stack to then become unresponsive for a minute or two afterwards. We have had no problems receiving BLE advertisements, just connecting.

Therefore we are starting with a conservative stance. We use BLE advertisements to find Android Thali peripherals but we then switch to Bluetooth in order to transmit the beacons. To make this switch we have to discover the peripheral's Bluetooth address. It so happens that a Bluetooth address is 6 octets long. We need one byte to flag that this is a Bluetooth address and then 6 bytes for the actual address. This fits nicely into our 8 octets and explains the `BluetoothAddress` structure above.

## The Bluetooth Handshake and Binding TCP/IP to Bluetooth

Once the Bluetooth Address is discovered the central will switch to an insecure RFCOMM connection to the supplied Bluetooth Address. This creates a situation in which the central device becomes a Bluetooth client and the peripheral device becomes a Bluetooth server. Bluetooth connections are full duplex. We will only support a single TCP/IP connection at a time over the Bluetooth transport. But we will then use our [TCP Multiplexer](https://github.com/thaliproject/Thali_CordovaPlugin/blob/master/thali/tcpmultiplex.js) to enable multiple simultaneous TCP/IP connections to be opened from the Bluetooth Client to the Bluetooth Server. Note that if both devices want to open connections to each other then each has to simultaneously take on the role of Bluetooth Client and Bluetooth Server.

## The Bluetooth handshake and surprise connections
Imagine a situation where Thali Peer B is advertising over BLE and suddenly receives a Bluetooth connection from Thali Peer A. Thali Peer B had not previously discovered Thali Peer A over BLE.

There are two possible reasons for why Thali Peer B hadn't previously discovered Thali Peer A. 

One possibility is that Thali Peer A hadn't been advertising itself over BLE because it didn't have any notification beacons to advertise. In other words, Thali Peer B hadn't found Thali Peer A over BLE because Thali Peer A wasn't advertising itself over BLE.

The other possibility is that Thali Peer A was advertising itself over BLE but for some reason Thali Peer B didn't get the advertisement. For example, we turn the BLE power down when the Thali app is in the background. So it's possible that Thali Peer A could hear Thali Peer B's BLE announcements but not vice versa.

So what would be nice is if Thali Peer A, when establishing a Bluetooth connection to Thali Peer B, could indicate if it is advertising any notification beacons. Of course Peer A already knows if any of the beacons it has are for Peer B and could decide to indicate if it is advertising beacons specifically for B. But we should not depend on that because it makes a few edge case attacks a little easier for the attacker. Therefore when a Thali Bluetooth client establishes a Bluetooth connection it MUST indicate as defined below if it has any notification beacon values available even if none of those value are for the Bluetooth Server the Bluetooth Client has connected to.

There is an ulterior motive behind all of this. We have run into issues with Bluetooth on Android where we think a Bluetooth connection has been established but  in fact something went wrong and it hasn't. We typically only find out there is a problem once some data gets sent. So we tend to always start all connections by sending a bit of data just to make sure everything is working. So our little notification exchange is really just an excuse to shake out the Bluetooth connections.

Therefore when a Thali Bluetooth client establishes a connection to a Thali Bluetooth server the client MUST send the following binary data first:

```
BluetoothHandshake = HasNotification / DoesNotHaveNotification
HasNotification = 0
DoesNotHaveNotification = 1
```

The Bluetooth server MUST respond to a properly formatted Bluetooth Handshake by sending the octet 0. If a Bluetooth server receives an initial byte value on a Bluetooth connection other than those allowed by the Bluetooth Handshake then the Bluetooth server MUST terminate the Bluetooth connection. Similarly if a Bluetooth Client receives an initial byte from the Bluetooth Server with a value other than 0 then it MUST terminate the Bluetooth connection. Our assumption in both cases is that the other side of the connection is buggy.

If a Bluetooth server receives a Bluetooth Handshake set to `HasNotification`  then the Bluetooth server MUST treat the handshake as being the equivalent of having a received a BLE announcement with the Bluetooth client's Bluetooth address and an unrecognized BLE address.

In the worst case the Bluetooth server might have already seen the Bluetooth client's BLE advertisement and will now retrieve it again. Ideally we would include the BLE address that the Bluetooth client is using (if any) so as to avoid duplicate notifications but there doesn't seem to be an API available in Android to ask what BLE address the local device's peripheral is using so the Thali peer doesn't know what BLE address it is advertising with.

### Binding TCP/IP to Bluetooth
After the Bluetooth Handshake has been sent then we had the streams over for sending and receiving TCP/IP data.

The actual logic for relaying TCP/IP over the Bluetooth connection works as follows:
1. The Thali software tells the local Thali Bluetooth layer to open a Bluetooth client connection to the remote peer
2. The Bluetooth Client layer will establish the connection (and send the handshake) and will then open a localhost TCP/IP listener and return the port for the listener to the Thali software.
3. The Thali software then opens a TCP/IP connection to the localhost TCP/IP listener. That listener will accept exactly one connection.
4. The Bluetooth code will now take the input stream from the TCP/IP connection to the localhost TCP/IP listener and connect it to the output stream of the Bluetooth client connection. The Bluetooth code will then take the input stream from the Bluetooth client connection and connect it to the output stream from the localhost TCP/IP listener. Of course the first byte sent to the client will be confirmed as the response to the Bluetooth Handshake.

The logic works the same on the Bluetooth Server side. Specifically:
1. The Thali software tells the local Thali Bluetooth layer that it wants to receive incoming Bluetooth connections. As part of that API call telling the Thali Bluetooth layer to listen, the Thali software will specify a TCP/IP localhost listener and port.
2. When a Bluetooth Client connection is made to the device the Thali Bluetooth code will open a TCP/IP client connection to the TCP/IP localhost listener and port specified by the Thali software in step 1. The Bluetooth server will wait to receive the Bluetooth Handshake and then response as defined by the Bluetooth Handshake. Once the Bluetooth Handshake is done then the Bluetooth Server will then take its input stream and connect it to the TCP/IP client's output stream. It will then take the TCP/IP client's input stream and connect it to the Bluetooth Server's output stream.

If there are multiple simultaneous Bluetooth connections to the Bluetooth Server then there will be multiple simultaneous localhost TCP/IP client connections made to the submitted TCP/IP localhost listener and port.

The relay logic is identical to that described in the Bluetooth Client text above.

As mentioned above this only creates a single TCP/IP connection over each Bluetooth connection. The previously mentioned TCP/IP Multiplexer will be used to enable multiple simultaneous TCP/IP connections to be multiplexed over the single TCP/IP connection.

## iOSDevice
Normally we do not use BLE for discovery with iOS. Instead we use the multi-peer connectivity framework whose binding will be described later in this document. However in order to enable iOS devices to be discovered in the background we also want to support BLE. However when an iOS device is in the background it can only communicate over BLE and so any further communication will have to occur using BLE characteristics. We will define the characteristics used to communicate beacon data in the future when we get closer to implementing this functionality.

##Notifying when beacons change
Once a Thali central finds a Thali peripheral and makes a request to /NotificationBeacons how does it know if the value of the notification beacons ever changes? After all, the peripheral might have new data for the central. How will this be discovered? If we were using characteristics then we could do a connect and use the notify functionality built into BLE. But for the reasons previously discussed we are not using characteristics.

Our solution depends on a behavior we have observed with Android. Whenever we stop and re-start a BLE peripheral Android appears to always give us a new BLE address. Therefore whenever the value in /NotificationBeacons change the BLE service MUST be stopped and restarted in order to obtain a new address. The result being that the peripheral will now look like a brand new device to everyone in the vicinity and they will automatically connect to get the new /NotificationBeacons value.

## Android Settings
The source code for the BLE support in Lollipop is available [here](https://android.googlesource.com/platform/frameworks/base/+/lollipop-release/core/java/android/bluetooth/le/).

### startAdvertising
When starting BLE advertising on Android the `startAdvertising()` API of `BluetoothLeAdvertiser` object is used. The settings, created by  [AdvertiseSettings.Builder](https://developer.android.com/reference/android/bluetooth/le/AdvertiseSettings.Builder.html), MUST be set as follows:
* `setAdvertiseMode()` - This MUST be set to `ADVERTISE_MODE_LOW_LATENCY` when the application is in the foreground and `ADVERTISE_MODE_LOW_POWER` when the application is in the background.
* `setConnectable()` - This MUST be set to false as we do not currently support any characteristics.
* `setTimeout()` - This MUST be set to 0. We advertise as long as we are running.
* `setTxPowerLevel()` - This MUST be set to `ADVERTISE_TX_POWER_LOW` when in the background and `ADVERTISE_TX_POWER_HIGH` when in the foreground.

`advertiseData` MUST be set to:
* `addManufacturerData()` - This MUST NOT be set. We need all the space in the BLE Advertisement we can get.
* `addServiceData(serviceDataUuid, serviceData)` - `serviceDataUuid` MUST be set to the Thali service's BLE UUID and serviceData MUST be set to the single byte "0" followed by the BLE UUID as a byte stream.
* `addServiceUuid(serviceUuid)` - `serviceUuid` MUST be set to the Thali service's BLE UUID.
* `setIncludeDeviceName()` - Must be set to false. We need the space.
* `setIncludeTxPowerLevel()` - Must be set to false. We need the space.

When calling `startAdvertising()` the scanResponse argument MUST NOT be used as we do not support a scanResponse.

__Open Issue:__ - We need to perform practical experiments to determine what the actual battery drain is of having BLE advertising on all the time. If we primarily use low power then it really shouldn't be bad since BLE was designed to allow beacons to advertise themselves for years on a single battery. But we need data.

### startScan
When calling startScan the filters argument MUST be used and MUST be set to Thali's BLE service UUID.

When calling startScan the settings argument MUST be used and MUST be set to:
* `setCallbackType(callbackType)` - If on API 23 then `callbackType` MUST be set to the flag `CALLBACK_TYPE_ALL_MATCHES` and MUST NOT include the `CALLBACK_TYPE_MATCH_LOST`. We are explicitly not going to worry about announcing when a BLE peripheral has gone. It really shouldn't matter given how we are using BLE.
* `setMatchMode(matchMode)` - If on API 23 then `matchMode` MUST be set to `MATCH_MODE_STICKY` .
* `setNumOfMatches(numOfMatches)` - If on API 23 then `numOfMatches` MUST bet set to `MATCH_NUM_MAX_ADVERTISEMENT`.
* `setReportDelay(reportDelayMillis)` - `reportDelayMillis` MUST bet set to at least 500 ms in the foreground and 1000ms in the background. The delay helps to make sure that we don't end up killing our node performance by flooding JXcore with endless notifications, each of which results in stopping JXcore.
* `setScanMode(scanMode)` - `scanMode` MUST be set to `SCAN_MODE_LOW_POWER` when running in the background and `SCAN_MODE_LOW_LATENCY` when running in the foreground.

__Open Issue:__ - What is the practical difference between `MATCH_MODE_AGGRESSIVE` and `MATCH_MODE_STICKY`? The real question is - what percentage of the time will `MATCH_MODE_AGGRESSIVE` give us a "hit" where the signal strength is too weak for the Bluetooth follow up connection to work properly?

__Open Issue:__ - We need to run experiments to determine the practical battery consumption consequences of running BLE scanning. Do the different scan modes make a big difference?

__Open Issue:__ - We run both scanning and advertising at the same time. Which means we need to also perform experiments to determine what happens when both are on in terms of battery consumption, responsiveness to discovery, etc.

### listenUsingInsecureRfcommWithServiceRecord
This method is used to start a Bluetooth socket listening. Thali's Bluetooth stack MUST use this api any time it is advertising via BLE to accept requests to retrieve the notification beacon contents as well as to allow for synchronization and other higher level connections.

The arguments MUST be used as follows:

* `name` - "Thali_Bluetooth"
* `uuid` - "0bbfc6ef-14cc-4ab2-af63-b92e887227ae"

### createInsecureRfcommSocketToServiceRecord
After having created a device via getRemoteDevice using the Bluetooth address retrieved over BLE the next step is to create a connection to that device. That is handled via `createInsecureRfcommSocketToServiceRecord`.  It's argument MUST be:

* `uuid` - "0bbfc6ef-14cc-4ab2-af63-b92e887227ae"

### More than one Thali app on an Android device
Our current design has a problem in that we advertise a static SDP UUID This means that if two Thali apps are both running on the same Android device they are going to conflict when they both try to advertise the same SDP UUID. And of course no one can control which of the apps they end up connecting to.

At the heart of this problem is a question of - what is a Thali app? The vision for Thali is that there is a Thali Device Hub which acts as a single store for all Thali related data so that the data is available to all apps and independent of any particular app. If that vision ships then we can give it its own SDP UUID. Meanwhile each Thali app will need its own SDP UUID. So eventually we'll have to amend this spec so that the name and uuid specified above become arguments passed into the Thali framework rather than something that is hard coded. But not today.

# Multi-Peer Connectivity Framework (MPCF)
Apple's proprietary multi-peer connectivity framework has its own discovery mechanism that appears to run over both Bluetooth and Wi-Fi. Note however that iOS's implementation of Bluetooth uses a proprietary extension that requires having a public key pair signed by Apple. And multi-peer connectivity's use of Wi-Fi when not connected to an access point appears to use a proprietary variant of Wi-Fi Direct. In any case, Multi-Peer Connectivity only works with Apple devices (either iOS or OS/X). We use MPCF to enable Thali apps running in the foreground to discovery and communicate with each other.

MPCF starts off by advertising via `MCNearbyServiceAdvertiser` the types of sessions that the device is willing to join. The advertisement consists of a peer ID, an info object made up of key/value pairs and a serviceType which can be between 1-15 characters long. Each key/value pair in the info object cannot be longer than 255 bytes. The total size of the info object cannot be more than 400 bytes (so it will fit into a single Bluetooth packet).

I haven't run an experiment to see what the maximum size of a MPCF announcement is but given that info tops out at 400 bytes it would be reasonable to assume that peerID and service type are both smaller in size than that. The point then being that the announcement mechanism is not the best way to discover the full beacon string which can easily be 1K or more.

Therefore we will only use the MPCF announcement to identify ourselves as a Thali node and then use our TCP/IP binding for further communication in order to retrieve things like notification beacons via the [HTTP endpoint](Transferring-discovery-beacon-values-over-HTTP).

__Note:__ We have runs tests that show that if `MCNearbyServiceAdvertiser` is turned off and then back on with a different `peerID`  this will not affect any preexisting sessions.

__Note:__ Our experiments with iOS 8 have shown that if we form two simultaneous MCSession objects between the same two peers then when moving larger amounts of data the streams associated with those session objects will spontaneously fail. This has proven to be a big enough problem that we have been forced to work around it by making sure that we only have a single MCSession object between any two peers and then multiplexing TCP connections established on either end across a single pair of streams associated with that session.

## MCNearbyServiceBrowser
MPCF discovers nearby services via `MCNearbyServiceBrowser`. When calling `initWithPeer:serviceType:` on  `MCNearbyServiceBrowser` the arguments MUST be:
* `myPeerID` - a newly generated UUID. 
* `serviceType` - "thaliproject".

__Open issue:__ I need to get out wireshark or equivalent to resolve this one but I believe the `serviceType` argument is meant to map to the `service` component of a mDNS discovery name as defined in [RFC 6763](http://tools.ietf.org/html/rfc6763). This then takes us to [RFC 6355](http://tools.ietf.org/html/rfc6335) which manages registration of DNS service names. The requirements in section [5.1 of RFC 6355](http://tools.ietf.org/html/rfc6335#section-5.1) match Apple's rules for `serviceType`. We therefore will use a complying name, in this case, "thaliproject" and yes, we really need to register it with IANA per [this bug](https://github.com/thaliproject/Thali_CordovaPlugin/issues/230). 

The `MCNearbyServiceBrowser` MUST have its delegate property set to a proper callback.

Because iOS requires that `MCNearbyServiceBrowser` MUST stop running when the application goes into the background this means that anytime the application enters the foreground if it wants to discover Thali peers then it MUST activate `MCNearbyServiceBrowser` as defined in this section with a fresh UUID.

## MCNearbyServiceAdvertiser
When the Thali application wishes to be discovered, typically because it has notification beacon values to advertise, it MUST create a `MCNearbyServiceAdvertiser`  object with the arguments as follows:
* `myPeerID` - The same UUID as currently being used by `MCNearbyServiceBrowser`.
* `info` - Empty.
* `serviceType` - "thaliproject".

The MCNearbyServiceAdvertiser object MUST also have its delegate property set to a proper callback.

The use of the same peerID on both `MCNearbyServiceBrowser` and `MCNearbyServiceAdvertiser` is driven by the need to easily identify when we might be trying to create two `MCSessions` objects between the same peers.

## Running MCSession connections in the background
iOS explicitly supports taking existing `MCSession` connections established in the foreground into the background for a period of time (typically one or two minutes) before the OS will terminate them. Thali's MPCF code MUST be implemented to enable `MCSession` connections to be taken into the background. Typically this requires establishing a background task when going into the background and handling the `MCSession` connections there.

## Binding TCP/IP to MPCF
The core of the binding is the `MCSession` object. This object can be created in one of two ways:

1. The local peer makes a request to the native layer to open a connection to a remote peer which causes the local peer to create a `MCSession` object and invite the remote peer to join it.
2. A remote peer creates a `MCSession` object and invites the local peer to join.

But per the previously mentioned bug we MUST NOT end up in a situation where we have two `MCSession` objects on the same peer involving the same remote peer.

Our solution to this situation is to leverage the fact that as defined below `peerID`s are UUIDs. This means they are (within reason) globally unique and they can be lexically compared. The larger of two UUIDs is defined as the UUID whose value represented as ASCII bytes when compared in ASCII ordering is the first to have a higher byte value for a character.

### Lexically Larger Peer
If a lexically larger peer wishes to connect to a lexically smaller peer then it MUST use `invitePeer` from `MCNearbyServiceBrowser` to invite the peer to a session with the following arguments:

* `peerID` - The `peerID` of the discovered peer taken from the `MCNearbyServiceBrowser` callback.
* `toSession` - The `MCSession` object that is passed in MUST be newly created for this connection following the previously specified rules.
* `withContext` - This MUST be set to `base64EncodedString` containing the Thali service's type name, e.g. "thaliproject".
* `timeout` - Unless overridden by the application the default timeout MUST be 10 seconds.

Once the lexically larger peer has invited the lexically smaller peer to a session the lexically larger peer MUST NOT invite the lexically smaller peer to any additional sessions until the outstanding invite has been resolved (either via time out, acceptance or rejection). If the invitation times out then the lexically larger peer MAY repeat the invitation but no more frequently than ever 300 ms. If the invitation is rejected then the lexically larger peer MUST return an error if the invitation came as part of a request from the local Thali application. In either case the lexically larger peer, if the invitation is rejected, MUST NOT retry without another request either from the Thali application or from the lexically smaller peer (see next section).

Once an invitation is accepted the lexically larger peer MUST NOT invite the lexically smaller peer to any additional `MCSession`s until the current session is disconnected.

When the lexically larger peer receives a call on  `MCSessionDelegate`'s `session:peer:didChangeState` with `state` set to `MCSessionStateConnected` then it MUST establish an output stream with the lexically smaller peer by calling `startStreamWithName:toPeer:error:` on the `MCSession` objects targeted at the lexically smaller peer with the `streamName` set to "ThaliStream".

The lexically larger peer will then receive a callback on its `MCSessionDelegate`'s `session:didReceiveStream:withName:fromPeer:` and MUST confirm that:
* The `peerID` matches the `peerID` that they associate with the `session` object. If the `peerID` does not match then a system error must be raised because something went seriously wrong. Specifically the lexically larger peer must have somehow invited more than one peer to the session.
* The `streamName` MUST be "ThaliStream" or the session MUST be terminated.

At this point the session is said to be ready. That is, both peers are members of the same session and both have established output streams to each other.

It is possible for a peer to receive an `invitePeer` request from a lexically smaller peer. In that case the lexically smaller peer is trying to signal to the lexically larger peer that it wishes to connect. The lexically larger peer MUST reject the `invitePeer` request with `accept` set to `false` and if a `MCSession` does not already exist with the lexically smaller peer (e.g. a race condition) then it MUST eventually establish a MPCF connection to the lexically smaller peer following the instructions in this section. Note that the lexically larger peer's ability to establish a connection can be gated by bandwidth, battery or other issues.

__Open Issue:__ It is worth noting that a race condition exists where the lexically larger peer may believe it is already in a `MCSession` with the lexically smaller peer but the lexically smaller peer believes it has disconnected from the `MCSession` and so sent another invitation. But it is difficult to distinguish this from a situation where the lexically larger peer wanted to establish a session at the same time the lexically smaller peer wanted to establish a session and the invites crossed paths. A way to handle this could be to use info to let the lexically smaller peer advertise the last failed `MCSession` it shared with the lexically larger peer. This would explicitly tell the lexically larger peer if the session it thinks is alives isn't as far as the lexically smaller peer is concerned.

### Lexically Smaller Peer

From the lexically smaller peer's perspective when it receives a callback on the `advertiser:didReceiveInvitationFromPeer:withContext:invitationHandler:` interface on the  `MCNearbyServiceAdvertiserDelegate` callback registered with its `MCNearbyServiceAdvertiser` object from the lexically larger peer it MUST validate that the `context` in the callback is set to a `base64EncodedString` that records the Thali service's type name, "thaliproject". If the `context` is not set to the Thali service's type name then the discovered peer MUST reject the invitation. Otherwise the lexically smaller peer MUST call the `invitationHandler` with `accept` set to `true` and the `session` object set to a newly created `MCSession`  object created using the previously specified rules. 

As soon as the session invitation is accepted the lexically smaller peer MUST establish an output stream with the lexically larger peer following the same rules as given for the lexically larger peer above.

The lexically smaller peer MUST wait to receive a callback on its `MCSessionDelegate`'s `session:didReceiveStream:withName:fromPeer:` and MUST confirm its values as given for the lexically larger peer. 

At this point the session is said to be ready. That is, both peers are members of the same session and both have established output streams to each other.

The lexically smaller peer MUST NOT join more than one `MCSession` at a time with the lexically larger peer. There are however race conditions which can create a situation where the lexically smaller peer believes it is in a `MCSession` with the lexically larger peer and the lexically larger peer no longer believes that `MCSession` is active. Therefore if the lexically smaller peer receives an invitation from the lexically larger peer to a new session then the lexically smaller peer MAY accept the invitation but it MUST first disconnect from the existing `MCSession`.

__Open Issue:__ I can't help but think, per the open issue in the previous section, that we should send an ID of the last `MCSession` (if any) that the lexically larger peer thought it shared with the lexically smaller peer so we can affirmatively identify cases where the two peers aren't in agreement as to the 'liveness' of a session. This would always be resolved in the favor of the peer that says a session is dead.

__Open Issue:__ I have a race condition nightmare where the lexically larger peer sends out an invitation, doesn't hear a response, sends out another invitation and the lexically smaller peer gets the second invitation first and rejects it in favor of the first (now dead) invitation and we end up with a dead situation. This should eventually resolve since the lexically smaller peer will get an error on the invitation it accepted (eventually) and the lexically larger peer will eventually time out and send out another invitation. But it still worries me. Since invitations are always send serially we could prevent this situation by using info to send an integer giving an incrementing count of which invitation this is. That way the lexically smaller peer would always know if it got invitations out of order. I'm not sure if this is worth it but this is the kind of bug that causes the clients to fail to connect for some period of time for no apparent reason.

It is possible that a lexically smaller peer wants to communicate with a lexically larger peer with whom it does not have an existing `MCSession`. In that case the lexically smaller peer MAY issue an `invitePeer` request to the lexically larger peer following the rules for `invitePeer` definition give above for the lexically larger peer. If the lexically smaller peer's invitation does not receive a response and if the lexically larger peer has not otherwise established a `MCsession` with the lexically smaller peer then the lexically smaller peer MAY assume that the lexically larger peer has not received the invitation and so the lexically smaller peer MAY try again. Note that there MUST be a reasonable limit on how often invitations are repeated, at a maximum invitations MUST NOT be repeated more often than every 300 ms. If the lexically smaller peer's invitation is rejected without the lexically larger peer having tried to establish its own `MCSession` then the lexically smaller peer MUST assume that the lexically larger peer has its own reasons for not connecting now and MUST NOT repeat the invitation. If the lexically smaller peer's invitation is accepted then the lexically smaller peer MUST log this event (there is something clearly wrong with somebody) along with all involved peerIDs and then MUST close the `MCSession`.

__Note:__ Our current belief is that MPCF won't produce "surprise connections" the way we see on Android. A surprise connection is a situation where a peer gets an incoming connection request from another peer it has not previously found over discovery. This is possible with Android because we use BLE for discovery and Bluetooth for connectivity and depending on the situation the radios are used with different power levels. So it's possible for peer A to discover peer B but not the reverse and so peer B is surprised when it gets a connection from peer A who it hadn't previously discovered. This is what led to the handshake we use in Bluetooth. For now we don't think MPCF will manifest this behavior so we don't use the same kind of handshake.

### Moving TCP Content on two levels
Because we are reusing a single pair of MPCF sockets to establish TCP/IP connections in two directions we have to use a multiplex solution that can layer independent TCP/IP connections going in both directions over a single pair of MPCF sockets. For the native layer TCP/IP connection (not the TCP/IP connections multiplexed on top of it) the lexically larger peer will be treated as the TCP/IP client and the lexically smaller peer as the TCP/IP server.

The details of this are complex and will be defined in the Node.js specifications. The key thing to know is that the lexically larger peer will:

1. Open a localhost TCP/IP listener on an open port and wait for the local Thali application to connect to the port. Only one connection will be accepted at a time.
2. Once the localhost TCP/IP listener gets a connection from the Thali application then connect the output stream from the localhost TCP/IP listener to the MPCF output stream established by the lexically larger peer and the localhost TCP/IP listener's input stream to the input stream established by the lexically smaller peer.
3. Once a session is ready (as defined above) if the `MCSession` was created at the initiative of the lexically smaller peer (e.g. it wasn't created in response to a request from the Thali application) then the lexically larger peer MUST signal the Node.js layer of the `MCSession` and assocaited TCP/IP listener's existence so that a multiplexer can be set up for the connection.

For the lexically smaller peer:

1. Open a localhost TCP/IP client that connects to a port passed in by the Thali application. This port MUST point at multiplexer logic.
2. Connect the TCP/IP client's input stream to the MPCF input stream from the lexically larger peer and the TCP/IP client's output stream to the MPCF output stream from the lexically smaller peer.

It will then be up to the Node.js layer to add a multiplexer on top of this single TCP/IP connect. That multiplexer will be able to establish TCP/IP connections in both directions. See the Node.js documentation for more details.

## Handling beacon changes
Whenever the beacons change a Thali peer MUST call `stopAdvertisingPeer` on `MCNearbyServiceAdvertiser`. The old `MCNearbyServiceAdvertiser` instance SHOULD be discarded but it is acceptable for there to be a delay in doing so as apparently iOS reacts badly if `MCNearbyServericeAdvertiser` objects get discarded too quickly. Then the Thali peer MUST create a new `MCNearbyServiceAdvertiser` instance with a new `peerID`. This process is required because once `peerID` is set on a `MCNearbyServiceAdvertiser` it cannot be changed.

By changing the `peerID` this should trigger a `browser:foundPeer:withDiscoveryInfo:` callback on the local `MCNearbyServiceBrowserDelegate` for the surrounding peers. This then notifies those peers that the advertiser has new notification values they need to examine.

## Multiple Thali Apps on the same iOS device
Thali as specified here can only run in the foreground. So you can have as many Thali apps as you want, only the one in the foreground gets to play. If we want to distinguish between multiple types of Thali apps we can either require each app to get its own unique service type (which we would then take as an argument). Or we can specify a HTTP endpoint where remote apps can just query as to the app's type. And if and when we put in BLE support it would be easy enough to put in a characteristic with type data. So we are probably o.k.

# Local Wi-Fi Binding
If a device is connected to a Wi-Fi access point (AP) then the device may use SSDP to discover other Thali nodes on the same network. It is worth pointing out that Wi-Fi APs are configured in many different ways. Some APs will allow for UDP multi-cast (to allow things like discovering printers) but won't allow for TCP connections (or even unicast UDP) to any but a white list of devices. So just because discovery seems to work the Thali peer can't be sure if direct connectivity will work. We will handle this uncertainty by providing Thali applications with multiple ways to connect to the same peer if possible (e.g. both local Wi-Fi AP and Bluetooth) and letting them try the different choices in turn. The details of this behavior will be defined in a later spec on the connection layer.

The main reason for picking SSDP over mDNS is simplicity. SSDP is a dirt simple text based protocol so it's very easy to deal with. If anyone has a super good reason why we should use mDNS instead we can switch.

SSDP supports two discovery mechanisms which each have their own performance and battery implications. One mechanism is ssdp:discover requests. These requests are multicast over UDP and responses are then unicast over UDP back to the requester. This makes discovery requests particularly expensive because each ssdp:discover request will result in all present Thali clients sending typically 3 responses directly to the requester. This puts a high load on the network (especially dense networks) for everyone. ssdp:alive messages on the other hand are sent out publicly to everyone with no response. So in general we will try to use ssdp:alive more than ssdp:discover.

## ssdp:alive
When a Thali application is in the foreground and connected to a Wi-Fi network it MUST issue a ssdp:alive request as defined below every 500 ms. When the Thali application is in the background and connected to a Wi-Fi network it MUST issue a ssdp:alive message no more frequently than every 1000 ms. The purpose of these ssdp:alive messages is to account for the fact that ssdp:discover requests and responses are not reliable. It also provides a way to detect when a peer has gone away. If no ssdp:alive requests are heard for quite some time then the peer can be marked as gone.

Whenever the Thali peer connects to a Wi-Fi network or if the Thali peer is already connected but the BSSID changes then the Thali peer MUST send an immediate ssdp:alive message and restart its send timer.

The ssdp:alive message MUST use the following header values:

* __NT:__ "http://www.thaliproject.org/ssdp"
* __USN:__ A UUID URL whose value MUST be changed every time the beacon string is changed
* __Location:__ A HTTP URL pointing to the device's IP address and port
* __Cache-Control:__ max-age = 180

We choose the relatively long period of 180 seconds in order to provide an opportunity for at least 3 rounds of ssdp:alive announcements from the peer to be detected before giving up. Note however that our current use of SSDP means we will likely ignore the cache-control header.

## ssdp:byebye
If a Thali peer is going to stop listening for SSDP events for a reason other than a throttling response or loss of network then it MUST send a ssdp:byebye event with the following header values:

* __NT:__ "http://www.thaliproject.org/ssdp"
* __USN:__ The same UUID URL used in the last ssdp:alive announcement from the peer

__Open Issue:__ It's likely that we are no longer going to communicate when peers disappear inside of the Thali software stack. The worst thing that happens if a peer is gone and we don't know it is that we waste time trying to connect to them. This is far from catastrophic. So maybe we can just skip byebye for now? Once less thing to test! This also means we can ignore the cache-control header in ssdp:alive. Although to be fair we will need some kind of cache clearing logic if only to keep the cache from growing to unbounded size.

## ssdp:discover
When a Thali peer is connected to a Wi-Fi AP after having been in a disconnected state or if the Thali peer has been in a connected state but is handed off to a Wi-Fi AP with a new BSSID ID then the Thali peer MUST issue a sspd:discover request as defined below in order to look for Thali devices on the local network. The Thali SSDP client MUST NOT otherwise use ssdp:discover requests as they are expensive for everyone involved.

The ssdp:discover request MUST use the following header value:

* __ST:__ "http://www.thaliproject.org/ssdp"

A ssdp:discover response MUST use the following header values:

* __ST:__ "http://www.thaliproject.org/ssdp"
* __USN:__ The UUID URL that the peer is currently using for its ssdp:alive messages
* __Cache-Control:__ max-age set to 180
* __Location:__ The same value returned in ssdp:alive

Thali's SSDP layer MUST put in place a throttling mechanism to ensure that it will only send responses to ssdp:discover requests at a fixed rate. The throttling mechanism MUST be designed to ensure that if a discovery request sits in the request queue beyond a specific maximum time period then it MUST be dropped without response. The throttling mechanism MUST also be able to limit the size of the request queue so that if the queue gets too full then a LIFO mechanism will be used to keep the queue at a fixed maximum size.

## UDP Throttling
Thali's SSDP layer MUST put in place a throttling mechanism on the UDP Multicast port used to listen for SSDP related multicasts. If the traffic rate exceeds a configured threshold then the Thali SSDP layer MUST stop listening on the port for a period of time before trying to listen again.

## Notification value changes
When a Thali application changes its notification values it MUST issue a ssdp:byebye message using its existing USN and then issue a ssdp:alive message using a new USN and reset its next message timer. This signals to all listeners that there are new notification values to be retrieved.

__Open Issue:__ It's tempting to just add in a new HTTP header to indicate that the notification values have been updated. This would make the logic for managing SSDP peers simpler as the USN would change less frequently. This is really a performance optimization. It's main benefit is that if a peer knows it doesn't care about a specific peer then once it detects that peer by matching on a notification beacon it can ignore the peer's further announcements. Also in the case that two peers are already synching and causing notification beacon value changes the two peers won't have to perform additional discoveries on each other because they don't recognize the USN. But we can implement this if perf analysis shows it's worth the effort.

## BSSID vs SSID
The use of BSSID rather than SSID is meant to be a conservative choice. In theory a set of Wi-Fi APs all sharing the same SSID could be configured to share UDP multicast information between each other. In that case we would only care about checking for changes in SSID, not BSSID. But it's possible for a networking of APs that share the same SSID to not allow routing beyond the local Wi-Fi AP in which case each new BSSID is in effect a new multicast domain. Since there isn't a particularly good way to figure this out we are erring on the conservative side and treating each new BSSID as an isolated network.

#DELETEME

THIS IS A BUNCH OF NONSENSE THAT I'M STICKING AT THE END OF THE DOC TO MAKE IT LONG ENOUGH THAT THE AWFUL JUMPING BUG  IN STACKEDIT.IO DOESN'T ATTACK ME.

JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK
JUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNKJUNK

