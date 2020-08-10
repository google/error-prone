---
title: IdentityHashMapBoxing
summary: Using IdentityHashMap with a boxed type as the key is risky since boxing may produce distinct instances
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Usage of `java.util.IdentityHashMap` with a boxed primitive type as a key is
risky and can yield unexpected results because `java.util.IdentityHashMap` uses
reference-equality when comparing keys and reference equality for primitive
wrappers is particularly bug-prone: Primitive wrapper classes cache instances
for some (but usually not all) values, so == may be equivalent to equals() for
some values but not others. Additionally, not all versions of the runtime and
other libraries use the cache in the same cases, so upgrades may change
behavior.

Thus:

```java
  Map<Integer, Foo> map = new IdentityHashMap<>();
  int n = randomInt();
  map.put(n, x);
  map.get(n);  // This could be null since boxing happens twice and could produce distinct values.
```

But:

```java
  Map<Integer, Foo> map = new IdentityHashMap<>();
  Integer n = randomInt();
  map.put(n, x);
  map.get(n);  // This cannot be null because it's the same instance.
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IdentityHashMapBoxing")` to the enclosing element.
