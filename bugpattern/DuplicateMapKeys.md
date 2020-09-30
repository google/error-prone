---
title: DuplicateMapKeys
summary: Map#ofEntries will throw an IllegalArgumentException if there are any duplicate
  keys
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
JDK 9 has
[`Map#ofEntries`](https://docs.oracle.com/javase/9/docs/api/java/util/Map.html#ofEntries-java.util.Map.Entry...-)
factory which throws runtime error when provided multiple entries with the same
key.

For eg, the following code is erroneously adding two entries with `Foo` as key.

```
Map<String, String> map = Map.ofEntries(
    Map.entry("Foo", "Bar"),
    Map.entry("Ping", "Pong"),
    Map.entry("Kit", "Kat"),
    Map.entry("Foo", "Bar"));
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DuplicateMapKeys")` to the enclosing element.
