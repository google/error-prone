---
title: ModifyCollectionInEnhancedForLoop
summary: Modifying a collection while iterating over it in a loop may cause a ConcurrentModificationException to be thrown or lead to undefined behavior.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
From the javadoc for
[`Iterator.remove`](https://docs.oracle.com/javase/9/docs/api/java/util/Iterator.html#remove--):

> The behavior of an iterator is unspecified if the underlying collection is
> modified while the iteration is in progress in any way other than by calling
> this method, unless an overriding class has specified a concurrent
> modification policy.

That is, prefer this:

```java
Iterator<String> it = ids.iterator();
while (it.hasNext()) {
  if (shouldRemove(it.next())) {
    it.remove();
  }
}
```

to this:

```java
for (String id : ids) {
  if (shouldRemove(id)) {
    ids.remove(id); // will cause a ConcurrentModificationException!
  }
}
```

TIP: This pattern is simpler with Java 8's
[`Collection.removeIf`](https://docs.oracle.com/javase/8/docs/api/java/util/Collection.html#removeIf-java.util.function.Predicate-):

```java
ids.removeIf(id -> shouldRemove(id));
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ModifyCollectionInEnhancedForLoop")` to the enclosing element.
