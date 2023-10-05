---
title: NonFinalStaticField
summary: Static fields should almost always be final.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`static` fields should almost always be both `final` and deeply immutable.

Instead of:

```java
private static String FOO = "foo";
```

Prefer:

```java
private static final String FOO = "foo";
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NonFinalStaticField")` to the enclosing element.
