---
title: GetClassOnEnum
summary: Calling getClass() on an enum may return a subclass of the enum type
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Enum values that declare methods are a subclass of the actual enum type, so
calling `getClass()` returns a synthetic subclass of the enum. To retrieve
the type of the enum, use `getDeclaringClass()`.

In the following example, `Binop.MULT.getClass()` returns the anonymous class
`Binop$2`, while `Binop.MULTI.getDeclaringClass()` returns the class `Binop`.

```java
enum Binop {
  MULT {
    @Override
    int apply(int x) {
      return x * x;
    }
  },
  ADD {
    @Override
    int apply(int x) {
      return x + x;
    }
  };

  abstract int apply(int x);
}
```

```java
public class Test {
  static void printEnumClass(Enum theEnum) {
    System.err.println(theEnum.getClass());
    System.err.println(theEnum.getDeclaringClass());
  }

  public static void main(String[] args) {
    printEnumClass(Binop.ADD);
  }
}
```

Prints:

```
class Binop$2
class Binop
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("GetClassOnEnum")` annotation to the enclosing element.
