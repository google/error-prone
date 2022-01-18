---
title: UnicodeEscape
summary: Using unicode escape sequences for printable ASCII characters is obfuscated,
  and potentially dangerous.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using unicode escapes in Java for printable characters is obfuscated. Worse,
given the compiler allows unicode literals outside of `String` literals, it can
be potentially unsafe.

Prefer using literal characters for printable characters.

For an example of malicious code, consider:

```java
class Evil {
  public static void main(String... args) {
    // Don't run this, it would be really unsafe!
    // \u000d Runtime.exec("rm -rf /");
  }
}
```

`\u000d` encodes a newline character, so `Runtime.exec` appears on its own line
and will execute.

NOTE: Unicode escapes are defined as a preprocessing step in the Java compiler
(see [JLS §3.3]). After compilation, there is no runtime difference whatsoever
between a Unicode escape and using the equivalent character in source. That is,
writing `"hello \u0077\u006f\u0072\u006c\u0064"` is equivalent to `"hello
world"` in the compiled `.class` file and at runtime.

[JLS §3.3]: https://docs.oracle.com/javase/specs/jls/se11/html/jls-3.html#jls-3.3

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnicodeEscape")` to the enclosing element.
