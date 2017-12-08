---
title: DefaultCharset
summary: Implicit use of the platform default charset, which can result in e.g. non-ASCII characters being silently replaced with '?' in many environments
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A [`Charset`][charset] is a mapping between sequences of [16-bit Unicode code
units][codeunit] and sequences of bytes. Charsets are used when encoding
characters into bytes and decoding bytes into characters.

[charset]: https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html
[codeunit]: http://unicode.org/glossary/#code_unit

Using APIs that rely on the default Charset under the hood is dangerous. The
default charset can vary from machine to machine or JVM to JVM. This can lead to
unstable character encoding/decoding between runs of your program!

If you need stable encoding/decoding, you must specify an explicit charset. The
[`StandardCharsets`][charsets] class provides these constants for you.

[charsets]: https://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html

When in doubt, use [UTF-8].

[UTF-8]: http://www.utf8everywhere.org/

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DefaultCharset")` to the enclosing element.
