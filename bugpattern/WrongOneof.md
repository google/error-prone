---
title: WrongOneof
summary: This field is guaranteed not to be set given it's within a switch over a
  one_of.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When switching over a proto `one_of`, getters that don't match the current case
are guaranteed to be return a default instance:

```java
switch (foo.getBlahCase()) {
  case FOO:
    return foo.getFoo();
  case BAR:
    return foo.getFoo(); // should be foo.getBar()
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("WrongOneof")` to the enclosing element.
