---
title: ClasspathShadowing
summary: Top-level class shadows a class on the compilation classpath
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Declaring a top-level class whose binary name already exists on the compilation
classpath or module path (`StandardLocation.CLASS_PATH` or
`StandardLocation.MODULE_PATH`) shadows the dependency's class during
compilation, which causes duplicate class conflicts, compilation and runtime
classpath skew, or redundant compilation of the same source file across targets.

Depend on the library that already provides the class instead of recompiling its
source file or defining a duplicate class with the same binary name.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ClasspathShadowing")` to the enclosing element.
