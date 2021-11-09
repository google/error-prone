---
title: UnicodeInCode
summary: Avoid using non-ASCII Unicode characters outside of comments and literals,
  as they can be confusing.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using non-ASCII Unicode characters in code can be confusing, and potentially
unsafe.

For example, homoglyphs can result in a different method to the one that was
expected being invoked.

```java
import static com.google.common.base.Objects.equal;

public void isAuthenticated(String password) {
  // The "l" here is not what it seems.
  return equaⅼ(password, this.password());
}

// ...

private boolean equaⅼ(String a, String b) {
  return true;
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnicodeInCode")` to the enclosing element.
