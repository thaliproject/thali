---
title: Thali local P2P presence protocol for opportunistic synching
layout: default
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

As will be explained below we relay TCP/IP content over our non-TCP/IP transports by connecting TCP/IP output streams to the non-TCP/IP transport's input streams and the non-TCP/IP transport's input streams to TCP/IP's output streams. 

But when we do this the only thing that gets moved is the data inside of the TCP/IP data packets. The data packet headers and the TCP control commands like FIN packets are not transmitted.

This means that when either the Bluetooth or MPCF code terminate their TCP/IP level connections then they MUST simultaneously terminate their non-TCP transport connections. If they do not do this then there is no way for the other side of the connection to know that the TCP link has been broken.

This also means that we are depending on the non-TCP transport connections to provide us with proper flow control as none of the TCP window sizing commands will be communicated across the wire.

In addition we only define a single TCP/IP connection that can be bound to the non-TCP transports. In general it is not useful to have only a single TCP/IP connection so we mandate that both the Bluetooth and MPCF bindings MUST always use the Thali [TCP Multiplexer](https://github.com/thaliproject/Thali_CordovaPlugin/blob/master/thali/tcpmultiplex.js) on top of the singleton TCP/IP connection in order to enable the establishment of multiple TCP/IP connections.

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

# Multi-Peer Connectivity Framework (MPCF)
Apple's proprietary multi-peer connectivity framework has its own discovery mechanism that appears to run over both Bluetooth and Wi-Fi. Note however that iOS's implementation of Bluetooth uses a proprietary extension that requires having a public key pair signed by Apple. And multi-peer connectivity's use of Wi-Fi when not connected to an access point appears to use a proprietary variant of Wi-Fi Direct. In any case, Multi-Peer Connectivity only works with Apple devices (either iOS or OS/X). We use MPCF to enable Thali apps running in the foreground to discovery and communicate with each other.

MPCF starts off by advertising via `MCNearbyServiceAdvertiser` the types of sessions that the device is willing to join. The advertisement consists of a peer ID, an info object made up of key/value pairs and a serviceType which can be between 1-15 characters long. Each key/value pair in the info object cannot be longer than 255 bytes. The total size of the info object cannot be more than 400 bytes (so it will fit into a single Bluetooth packet).

I haven't run an experiment to see what the maximum size of a MPCF announcement is but given that info tops out at 400 bytes it would be reasonable to assume that peerID and service type are both smaller in size than that. The point then being that the announcement mechanism is not the best way to discover the full beacon string which can easily be 1K or more.

Therefore we will only use the MPCF announcement to identify ourselves as a Thali node and then use our TCP/IP binding for further communication in order to retrieve things like notification beacons via the [HTTP endpoint](Transferring-discovery-beacon-values-over-HTTP).

__Open Issue:__ This design explicitly assumes that it is fine to have multiple independent sessions between the same peers using MPCF.

__Open Issue:__ This design also assumes that if `MCNearbyServiceAdvertiser` is turned off and then back on with a different `peerID`  this will not affect any preexisting sessions.

## MCNearbyServiceBrowser
MPCF discovers nearby services via `MCNearbyServiceBrowser`. When calling `initWithPeer:serviceType:` on  `MCNearbyServiceBrowser` the arguments MUST be:
* `myPeerID` - a newly generated UUID. 
* `serviceType` - "thaliproject".

__Open issue:__ I need to get out wireshark or equivalent to resolve this one but I believe the `serviceType` argument is meant to map to the `service` component of a mDNS discovery name as defined in [RFC 6763](http://tools.ietf.org/html/rfc6763). This then takes us to [RFC 6355](http://tools.ietf.org/html/rfc6335) which manages registration of DNS service names. The requirements in section [5.1 of RFC 6355](http://tools.ietf.org/html/rfc6335#section-5.1) match Apple's rules for `serviceType`. We therefore will use a complying name, in this case, "thaliproject" and yes, we really need to register it with IANA per [this bug](https://github.com/thaliproject/Thali_CordovaPlugin/issues/230). 

The `MCNearbyServiceBrowser` MUST have its delegate property set to a proper callback.

Because iOS requires that `MCNearbyServiceBrowser` MUST stop running when the application goes into the background this means that anytime the application enters the foreground if it wants to discover Thali peers then it MUST activate `MCNearbyServiceBrowser` as defined in this section with a fresh UUID.

__Open Issue:__ Requiring a fresh UUID in theory makes security a tiny bit better (it's hard to argue it makes it a lot better given that the Bluetooth and WiFi MAC addresses are hard coded in iOS) but it also has a performance penalty. When an application goes into the background it's unlikely (although not impossible, see requirements below) that its database state will change. Therefore when it comes out of background it most likely has no new notifications to share. Therefore if it could use the same UUID it previously used in the case that the notification beacons haven't changed since it went into the background then this could save network connections by peers who know they have already retrieved and handled those beacons. Is this optimization worth implementing? Certainly not now, but eventually it might.

## MCNearbyServiceAdvertiser
When the Thali application wishes to be discovered, typically because it has notification beacon values to advertise, it MUST create a `MCNearbyServiceAdvertiser`  object with the arguments as follows:
* `myPeerID` - The same UUID as currently being used by `MCNearbyServiceBrowser`.
* `info` - Empty.
* `serviceType` - "thaliproject".

The MCNearbyServiceAdvertiser object MUST also have its delegate property set to a proper callback.

__Open Issue:__ It isn't clear to me if it is actually necessary for the UUID used by `MCNearbyServceAdvertise` and `MCNearbyServiceBrowser` to be the same. This certainly provides no security benefit since any advertisements and session establishment requests (which would come from discovery) will have the same network addresses and so clearly be related. So even if we used different UUIDs it wouldn't provide any additional security. But using the same UUID hard codes in the idea that `MCNearbyServiceBrowser` MUST be running for `MCNearbyServiceAdvertise` to be used. And given how Thali works that is actually a reasonable assumption. So it may not be harmful to require the same UUID but maybe it's not helpful either? Should we just use different UUIDs?

## Running MCSession connections in the background
iOS explicitly supports taking existing `MCSession` connections established in the foreground into the background for a period of time (typically one or two minutes) before the OS will terminate them. Thali's MPCF code MUST be implemented to enable `MCSession` connections to be taken into the background. Typically this requires establishing a background task when going into the background and handling the `MCSession` connections there.

## Binding TCP/IP to MPCF
MPCF communication starts when one peer sends a session invitation to another peer. Once a session is establish between two peers then each peer can open an output socket to another peer. Because these are just output sockets they are simplex, not duplex. But our goal is to move a TCP/IP connection over MPCF and that requires a duplex connection. Our approach then is to create a situation where one Thali peer can open an output socket to another Thali peer and that Thali peer will then automatically respond with its own matching output socket going to the first peer. This then creates a full duplex connection between the peers.

Below we have two places where `MCSession` objects need to be created. In each case the `myPeerID` for the `MCSession` object MUST be set to the same `peerID` as being advertised with the peer's `MCNearbyServiceBrowser`. The `MCSession` object also MUST specify a proper callback for its `delegate` property.

__Open Issue:__ I actually cannot come up with a good reason why the `peerID` for `MCSession` couldn't be a brand new value. It's not like we use it for anything.

When a Thali peer discovers another Thali peer via `MCNearbyServiceBrowser` then the discovering peer, if it wishes to establish a TCP/IP connection with the discovered peer, MUST use `invitePeer` from `MCNearbyServiceBrowser` to invite the peer to a session with the following arguments:

* `peerID` - The `peerID` of the discovered peer taken from the `MCNearbyServiceBrowser` callback.
* `toSession` - The `MCSession` object that is passed in MUST be newly created for this connection following the previously specified rules.
* `withContext` - This MUST be set to `base64EncodedString` containing the Thali service's type name, e.g. "thaliproject".
* `timeout` - Unless overridden by the application the default timeout MUST be 10 seconds.

The discovered peer will then receive a callback on the `advertiser:didReceiveInvitationFromPeer:withContext:invitationHandler:` interface on the  `MCNearbyServiceAdvertiserDelegate` callback registered with its `MCNearbyServiceAdvertiser` object. The discovered peer MUST validate that the `context` in the callback is set to a `base64EncodedString` that records the Thali service's type name, "thaliproject". If the `context` is not set to the Thali service's type name then the discovered peer MUST reject the invitation. Otherwise the discovered peer MUST call the `invitationHandler` with `accept` set to `true` and the `session` object set to a newly created `MCSession`  object created using the previously specified rules. If the discovered peer accepts the invitation then it MUST record the `peerID` value and associate it with the created `MCSession` object. If the discovered peer joins the session then it MUST establish an output stream as defined below.

When the discovering peer receives a callback on its `MCSessionDelegate`'s `session:peer:didChangeState` with `state` set to `MCSessionStateConnected` then it MUST establish an output stream with the discovered peer as defined below.

Both the discovering and discovered peers MUST establish output streams with each other by calling `startStreamWithName:toPeer:error:` on their `MCSession` objects targeted at the other peer with the `streamName` set to "ThaliStream".

When each of the peers  receives a callback on their `MCSessionDelegate`'s `session:didReceiveStream:withName:fromPeer:` they MUST confirm that:
* The `peerID` matches the `peerID` that they associate with the `session` object. If the `peerID` does not match then a system error must be raised because something went seriously wrong. In the case of the discovering peer it means it invited more than one peer to the session.  In the case of the discovered peer it accepted invitations to the same session from more than one peer.
* The `streamName` MUST be "ThaliStream" or the session MUST be terminated.

Both the discovering and discovered peers MUST set a timer starting when they issue the `startStreamWithName:toPeer:error:` request. If they have not received the `session:didReceiveStream:withName:fromPeer:` callback for "ThaliStream" before the timer expires then they MUST kill the session. By default the timer MUST be set to 10 seconds unless this value is overridden by the application.

__OPEN ISSUE:__ Is there any reason to have a handshake like we do with Bluetooth on Android? Do we need to "prime" the connection the way we do with Bluetooth? Are surprise connections (a la BLE/Bluetooth) possible with MPCF? I suspect that discovery and connectivity generally happens over the same transports but I'm not 100% sure. If MPCF were to do discovery over bluetooth but connectivity of Wi-Fi then a surprise connection si certainly possible. Certainly there is nothing in the MPCF specs that belies the possibility of a peer showing up that one hasn't ever discovered. But could it be a peer that wanted to be discovered and somehow wasn't? If so then we need to put in the same surprise handling we have for Bluetooth.

Now we finally have a single session with both the discovering and discovered peer with an output stream between each of the peers giving us full duplex. So now we can finally switch to TCP/IP.

The process for establishing a TCP/IP connection from the perspective of the discovering peer is:

1. Open a localhost TCP/IP listener on an open port and wait for the local Thali application to connect to the port. Only one connection will be accepted at a time.
2. Once the localhost TCP/IP listener gets a connection from the Thali application then connect the output stream from the localhost TCP/IP listener to the MPCF input stream established by the discovering peer and connect the MPCF input stream from the discovered peer to the localhost TCP/IP listener output stream.

On the discovered peer side the process is:

1. The discovered peer's Thali application tells the Thali MPCF layer that it wishes to be discoverable and specifies the localhost TCP/IP port that any incoming connections should connect to.
2. The discovered peer's Thali MPCF layer creates a TCP/IP client that it has connect to the localhost TCP/IP port specified by the Thali application is step 1.
3. The discovered peer then connects the MPCF input stream from the discovering peer to the output stream from the TCP/IP client connection and the TCP/IP client connection's input stream to the MPCF's output stream.

Note that if two peers simultaneously want to open connections to each other than they will end up with two separate sessions.

## Handling beacon changes
Whenever the beacons change a Thali peer MUST call `stopAdvertisingPeer` on `MCNearbyServiceAdvertiser` and discard the `MCNearbyServiceAdvertiser` instance. Then the Thali peer MUST create a new `MCNearbyServiceAdvertiser` instance with a new `peerID`. This process is required because once `peerID` is set on a `MCNearbyServiceAdvertiser` it cannot be changed.

By changing the `peerID` this should trigger a `browser:foundPeer:withDiscoveryInfo:` callback on the local `MCNearbyServiceBrowserDelegate` for the surrounding peers. This then notifies those peers that the advertiser has new notification values they need to examine.

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
