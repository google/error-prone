---
title: ScannerUseDelimiter
summary: Scanner.useDelimiter is not an efficient way to read an entire InputStream
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`Scanner.useDelimiter("\\A")` is not an efficient way to read an entire
`InputStream`.

```java
Scanner scanner = new Scanner(inputStream, UTF_8).useDelimiter("\\A");
String s = scanner.hasNext() ? scanner.next() : "";
```

`Scanner` separates its input into "tokens" based on a delimiter that is a
regular expression. The regular expression `\A` matches the beginning of the
input, only, so there is no later delimiter and the single token consists of
every character read from the `InputStream`.

This works, but has multiple drawbacks:

*   You need a special case for an empty `InputStream`. In that case there is no
    token after `\A`. That's why the extract above checks `hasNext()`. If you
    forget to do that, you get `NoSuchElementException` in the empty case.
*   It swallows `IOException`. Quoting the `Scanner` specification:

    > A scanner can read text from any object which implements the `Readable`
    > interface. If an invocation of the underlying readable's `read()` method
    > throws an `IOException` then the scanner assumes that the end of the input
    > has been reached. The most recent `IOException` thrown by the underlying
    > readable can be retrieved via the `ioException()` method.

*   It is much slower than calling `inputStream.readAllBytes()`.

Instead, prefer one of the following alternatives:

Since Java 9, it has been possible to write this:

```java
String s = new String(inputStream.readAllBytes(), UTF_8);
```

On Android, that does require API level 33, though. Guava's
[`ByteStreams.toByteArray(inputStream)`](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/io/ByteStreams.html#toByteArray\(java.io.InputStream\))
is equivalent to `inputStream.readAllBytes()`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ScannerUseDelimiter")` to the enclosing element.
