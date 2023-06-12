---
title: InlineTrivialConstant
summary: Consider inlining this constant
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Constants should be given names that emphasize the semantic meaning of the
value. If the name of the constant doesn't convey any information that isn't
clear from the value, consider inlining it.

For example, prefer this:

```java
System.err.println(1);
System.err.println("");
```

to this:

```java
private static final int ONE = 1;
private static final String EMPTY_STRING = "";
...
System.err.println(ONE);
System.err.println(EMPTY_STRING);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InlineTrivialConstant")` to the enclosing element.
