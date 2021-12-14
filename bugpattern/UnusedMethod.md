---
title: UnusedMethod
summary: Unused.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: Unused, unused, UnusedParameters_

## The problem
The presence of an unused method may indicate a bug. This check highlights
_private_ methods which are unused and can be safely removed without considering
the impact on other source files.

## Suppression

Methods and fields which are used by reflection can be annotated with `@Keep` to
suppress the warning.

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

All false positives can be suppressed by annotating the method with
`@SuppressWarnings("unused")` or prefixing its name with `unused`.

