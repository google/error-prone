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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnicodeEscape")` to the enclosing element.
