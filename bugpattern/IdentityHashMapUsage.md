---
title: IdentityHashMapUsage
summary: IdentityHashMap usage shouldn't be intermingled with Map
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`java.util.IdentityHashMap` uses reference equality to compare keys. This is
[in violation of the contract of `java.util.Map`](http://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/IdentityHashMap.html),
which states that object equality (the keys' `equals` methods) should be used
for key comparison. This peculiarity can lead to confusion and subtle bugs,
especially when the two types of maps are used together. This check attempts to
reduce confusion between these two very different kinds of maps in a few ways:

1.  An `IdentityHashMap`'s reference-equality behavior would be surprising if
    its declared type was `java.util.Map`. The type of an `IdentityHashMap` is
    required to be explicit, i.e., `IdentityHashMap` should not be upcast to
    `Map`.
1.  `IdentityHashMap`'s `equals` method will only compare equal to a `Map` if
    the keys of both are the same instances. This makes little sense for an
    object-equality map, so `IdentityHashMap.equals()` should only be used to
    compare to other `IdentityHashMap`s.
1.  The semantics of `Map` and `IdentityHashMap` are different enough that they
    should be considered different, incompatible types. Converting one to the
    other is discouraged, as it is often a mistake.

```java
Map<String, String> bad(Map<String, String> aMap, IdentityHashMap<String, String> identityMap) {
  // Don't assign an `IdentityHashMap` to a plain `Map`-typed variable
  Map<String, String> myMap = identityMap;
  // Don't use `IdentityHashMap`'s `equals` method with a `Map`
  identityMap.equals(aMap);
  // Don't convert between `IdentityHashMap` and `Map`
  identityMap.putAll(aMap);
  identityMap = new IdentityHashMap<>(aMap);
  return identityMap;
}
```

```java
// Keep `IdentityHashMap`'s type information around so maintainers know when
// reference equality is being used.
IdentityHashMap<String, String> good() {
  IdentityHashMap<String, String> identityMap = new IdentityHashMap<>();
  // ...
  return identityMap;
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IdentityHashMapUsage")` to the enclosing element.
