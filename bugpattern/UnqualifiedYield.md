---
title: UnqualifiedYield
summary: In recent versions of Java, 'yield' is a contextual keyword, and calling
  an unqualified method with that name is an error.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
In recent versions of Java, `yield` is a restricted identifier:

```java
class T {
  void yield() {}
  {
    yield();
  }
}
```

```
$ javac --release 20 T.java
T.java:3: error: invalid use of a restricted identifier 'yield'
    yield();
    ^
  (to invoke a method called yield, qualify the yield with a receiver or type name)
1 error
```

To invoke existing methods called `yield`, use qualified names:

```java
class T {
  void yield() {}
  {
    this.yield();
  }
}
```

```java
class T {
  static void yield() {}
  {
    T.yield();
  }
}
```

```java
class T {
  void yield() {}
  class I {
    {
      T.this.yield();
    }
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnqualifiedYield")` to the enclosing element.
