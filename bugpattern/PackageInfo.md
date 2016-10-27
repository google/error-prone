---
title: PackageInfo
summary: Declaring types inside package-info.java files is very bad form
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Classes should not be declared inside `package-info.java` files.

> Typically package-info.java contains only a package declaration, preceded
> immediately by the annotations on the package. While the file could
> technically contain the source code for one or more classes with package
> access, it would be very bad form.

-- [JLS 7.4]

[JLS 7.4]: https://docs.oracle.com/javase/specs/jls/se8/html/jls-7.html#jls-7.4

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PackageInfo")` annotation to the enclosing element.
