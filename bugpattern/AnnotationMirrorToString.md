---
title: AnnotationMirrorToString
summary: AnnotationMirror#toString doesn't use fully qualified type names, prefer
  auto-common's AnnotationMirrors#toString
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
In [recent versions of javac](https://bugs.openjdk.java.net/browse/JDK-8268729),
`AnnotationMirror#toString` returns a string representation of the annotation
that uses simple names. If the string is used in generated source code, it may
require additional imports.

For example, instead of this:

```
@com.pkg.Foo(bar = com.pkg.Bar.class, baz = com.pkg.Baz.ONE)
```

javac now generates the following:

```
@Foo(bar = Bar.class, baz = Baz.ONE)
```

which may require imports for `com.pkg.Foo`.

`auto-common`'s `AnnotationMirrors#toString` method produces a string that uses
fully qualified names for annotations, class literals, and enum constants,
ensuring that source code containing that string will compile without additional
imports.

TIP: `AnnotationMirrors#toString` may be beneficial even if the string isn't
being used in generated code, e.g. if it's part of a diagnostic message or
assertion failure message, since the fully qualified names makes it clearer
which types are being referred to.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AnnotationMirrorToString")` to the enclosing element.
