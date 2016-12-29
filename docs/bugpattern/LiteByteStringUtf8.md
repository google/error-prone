When serializing bytes from a `MessageLite`, one can use `toByteString` to get a
`ByteString`, effectively an immutable wrapper over a `byte[]`. This ByteString
can be passed around and deserialized into a message using
[`MyMessage.Builder.mergeFrom(ByteString)`][merge].

[`ByteString#toStringUtf8`] copies UTF-8 encoded byte data living inside the
`ByteString` to a `java.lang.String`, replacing any [invalid UTF-8 byte
sequences][invalid-utf8-byte-sequences] with � (the Unicode replacement character).

In this circumstance, a protocol message is being serialized to a `ByteString`,
then immediately turned into a Java `String` using the `toStringUtf8` method.
However, serialized protocol buffers are arbitrary binary data and not
UTF-8-encoded data. Thus, the resulting `String` may not match the actual
serialized bytes from the protocol message.

Instead of holding the serialized protocol message in a Java `String`, carry
around the actual bytes in a `ByteString`, `byte[]`, or some other equivalent
container for arbitrary binary data.

[merge]: https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/Message.Builder#mergeFrom-com.google.protobuf.ByteString-
[`ByteString#toStringUtf8`]: https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/ByteString#toStringUtf8--
[invalid-utf8-byte-sequences]: https://en.wikipedia.org/wiki/UTF-8#Invalid_byte_sequences
