A [`Charset`][charset] is a mapping between sequences of [16-bit Unicode code
units][codeunit] and sequences of bytes. Charsets are used when encoding
characters into bytes and decoding bytes into characters.

[charset]: https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html
[codeunit]: http://unicode.org/glossary/#code_unit

Using APIs that rely on the JVM's default Charset under the hood is dangerous.
The default charset can vary from machine to machine or JVM to JVM. This can
lead to unstable character encoding/decoding between runs of your program, even
for ASCII characters (e.g.: `A` is `0100 0001` in `UTF-8`, but is `0000 0000
0100 0001` in `UTF-16`).

If you need stable encoding/decoding, you must specify an explicit charset. The
[`StandardCharsets`][charsets] class provides these constants for you.

[charsets]: https://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html

When in doubt, use [UTF-8].

[UTF-8]: http://www.utf8everywhere.org/

