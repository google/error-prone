---
title: ForEachIterable
summary: This loop can be replaced with an enhanced for loop.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer enhanced for loops instead of explicitly using an iterator where
possible.

That is, prefer this:

```java
for (T element : list) {
  doSomething(element);
}
```

to this:

```java
for (Iterator<T> iterator = list.iterator(); iterator.hasNext(); ) {
  doSomething(iterator.next());
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ForEachIterable")` to the enclosing element.
