---
title: PrivateConstructorForUtilityClass
summary: Classes which are not intended to be instantiated should be made non-instantiable with a private constructor. This includes utility classes (classes with only static members), and the main class.
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Utility classes are classes that only include static members and are not
designed to be instantiated, for example `java.lang.Math` or `java.util.Arrays`.

In the absence of explicit constructors, however, the compiler provides a
public, parameterless default constructor. To a user, this constructor is
indistinguishable from any other. It is not uncommon for a published API to
accidentally include a public constructor for a class intended to be
uninstantiable.

To prevent users from instantiating classes that are not designed to be
instantiated, you can add a private constructor:

```java
public class UtilityClass {
  private UtilityClass() {}
}
```

See:

*   [Effective Java, Third Edition, Item 4][ej3e-4]

[ej3e-4]: https://books.google.com/books?id=BIpDDwAAQBAJ

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("PrivateConstructorForUtilityClass")` to the enclosing element.
