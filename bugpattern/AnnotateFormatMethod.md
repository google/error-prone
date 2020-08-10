---
title: AnnotateFormatMethod
summary: This method passes a pair of parameters through to String.format, but the enclosing method wasn't annotated @FormatMethod. Doing so gives compile-time rather than run-time protection against malformed format strings.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This method passes a pair of parameters through to `String#format`, but the
enclosing method wasn't annotated `@FormatMethod`. Doing so gives compile-time
rather than run-time protection against malformed format strings. Consider
annotating the format string with
`@com.google.errorprone.annotations.FormatString` and the method with
`@FormatMethod` to allow compile-time checking for well-formed format strings.

```java
static void log(String format, String... args) {
  Log.w(format, args);
}

void frobnicate(int a, int b) {
  if (a < b) {
    // Whoops: didn't provide enough format args.
    log("%s < %s", a);
  }
}
```

```java
@FormatMethod
static void log(@FormatString String format, String... args) {
  Log.w(format, args);
}
```

WARNING: There's a very high chance that manual intervention will be required
after applying this fix, either due to existing uses of the method not passing
in valid format strings, or methods which delegate to this one requiring the
`@FormatMethod` annotation as well. Please ensure that everything depending on
this code still compiles after applying.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AnnotateFormatMethod")` to the enclosing element.
