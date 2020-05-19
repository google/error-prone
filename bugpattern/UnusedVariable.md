---
title: UnusedVariable
summary: Unused.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


_Alternate names: unused, UnusedParameters_

## The problem
The presence of an unused variable may indicate a bug. This check highlights
_private_ fields and parameters which are unused and can be safely removed
without considering the impact on other source files.


## Suppression

False positives on fields and parameters can be suppressed by prefixing the
variable name with `unused`, e.g.:

```java
private static void authenticate(User user, Application unusedApplication) {
  checkState(user.isAuthenticated());
}
```


All false positives can be suppressed by annotating the variable with
`@SuppressWarnings("unused")`.


