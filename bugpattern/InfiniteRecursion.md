---
title: InfiniteRecursion
summary: This method always recurses, and will cause a StackOverflowError
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A method that always calls itself will cause a StackOverflowError.

```java
int oops() {
  return oops();
}
```

```
Exception in thread "main" java.lang.StackOverflowError
  at Test.oops(X.java:3)
  at Test.oops(X.java:3)
  ...
```

The fix may be to call another method with the same name:

```java
void process(String name, int id) {
  process(name, id); // error
  process(name, id, /*verbose=*/ true); // ok
}

void process(String name, int id, boolean verbose) {
  // ...
}
```

or to call the method on a different instance:

```java
class Delegate implements Processor {
  Procesor delegate;

  void process(String name, int id) {
    process(name, id); // error
    delegate.process(name, id); // ok
  }
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("InfiniteRecursion")` annotation to the enclosing element.
