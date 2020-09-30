---
title: ChainingConstructorIgnoresParameter
summary: The called constructor accepts a parameter with the same name and type as
  one of its caller's parameters, but its caller doesn't pass that parameter to it.  It's
  likely that it was intended to.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When a class exposes multiple constructors, they're generally used as a means of
initializing default parameters. If a chaining constructor ignores a parameter,
it's likely the parameter needed to be plumbed to the chained constructor.

```java
MissileLauncher(Location target) {
  this(target, false);
}
MissileLauncher(boolean askForConfirmation) {
  this(TEST_TARGET, false); // should be askForConfirmation
}
MissileLauncher(Location target, boolean askForConfirmation) {
   ...
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ChainingConstructorIgnoresParameter")` to the enclosing element.
