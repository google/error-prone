---
title: AssignmentExpression
summary: The use of an assignment expression can be surprising and hard to read; consider
  factoring out the assignment to a separate statement.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using the result of an assignment expression can be quite unclear, except for
simple cases where the result is used to initialise another variable, such as
`x = y = 0`.

Consider a common pattern of lazily initialising a field:

```java
class Lazy<T> {
  private T t = null;

  abstract T create();

  public T get() {
    if (t != null) {
      return t;
    }
    return t = create();
  }
}
```

```java
class Lazy<T> {
  private T t = null;

  abstract T create();

  public T get() {
    if (t != null) {
      return t;
    }
    t = create();
    return t;
  }
}
```

At the cost of another line, it's now clearer what's happening. (Note that
neither the before nor the after are thread-safe; this particular example would
be better served with `Suppliers.memoizing`.)

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssignmentExpression")` to the enclosing element.
