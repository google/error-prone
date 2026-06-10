---
title: PreferCharsetOverload
summary: Prefer calling overloads that accept a Charset over those that accept a String
  encoding name.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
# PreferCharsetOverload

APIs that accept a `String` charset name often have an overload that accepts a
`java.nio.charset.Charset`. Prefer using the `Charset` overload, as it provides
stronger typing.

If a `Charset` instance is being converted to a `String` (e.g. via `.name()`,
`.displayName()`, or `.toString()`) just to call the `String` overload, the
conversion can simply be removed.

**Note:** This check relies on method parameter name information being preserved
in class files at runtime. Therefore, compiling the targeted libraries or JDK
stubs with the `-parameters` flag (introduced in
[JEP 118](https://openjdk.org/jeps/118)) is required for the check to discover
candidate parameters reliably.

## Examples

### Instead of this...

```java
OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
String s = new String(bytes, "ISO-8859-1");
log("hello", charset.name());
```

### Prefer this...

```java
OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
String s = new String(bytes, ISO_8859_1);
log("hello", charset);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PreferCharsetOverload")` to the enclosing element.
