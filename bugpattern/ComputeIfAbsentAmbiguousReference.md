---
title: ComputeIfAbsentAmbiguousReference
summary: computeIfAbsent passes the map key to the provided class's constructor
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
With ambiguous constructor references used in `java.util.Map#computeIfAbsent`
function parameter, it becomes unclear what the code intends to do.

```java
map.computeIfAbsent(someLong, AtomicLong::new).incrementAndGet()
```

Code of this form can seemingly look like it's trying to make a counter,
creating the key if absent. Unfortunately the code is surprising, because it
will call the wrong `AtomicLong` constructor. Instead, it will create the
counter initialized with the key value, which is probably not desired.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComputeIfAbsentAmbiguousReference")` to the enclosing element.
