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
private fields, and parameters of private methods, which are unused and can be
safely removed without considering the impact on other source files. "Private"
in this context also includes effectively-private members, like public members
of private classes.

## Suppression

False positives on fields and parameters can be suppressed by prefixing the
variable name with `unused`, e.g.:

```java
private static void authenticate(User user, Application unusedApplication) {
  checkState(user.isAuthenticated());
}
```

Fields which are used by reflection can be annotated with `@Keep` to suppress
the warning.

This annotation can also be applied to annotations, to suppress the warning for
any member annotated with that annotation:

```java
import com.google.errorprone.annotations.Keep;

@Keep
@Retention(RetentionPolicy.RUNTIME)
@interface Field {}

...

public class Data {
  @Field private int a; // no warning.
  ...
}
```

All false positives can be suppressed by annotating the variable with
`@SuppressWarnings("unused")`.

