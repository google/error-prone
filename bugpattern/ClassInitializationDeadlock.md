---
title: ClassInitializationDeadlock
summary: Possible class initialization deadlock
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
To avoid deadlocks, class initializers should not reference subtypes of the
current class.

For example:

```java
class Foo {
  public static final Bar INSTANCE = new Bar();
  public static class Bar extends Foo {}
}
```

There is a circular reference between the class initializers for `Foo` and
`Bar`: `Foo` depends on `Bar` in the initializer for a `static` field, and
initializing `Bar` requires initializing its supertype `Foo`. If one thread
starts initializing `Foo` and another thread simultaneously starts initializing
`Bar`, it will result in a deadlock.

--------------------------------------------------------------------------------

## AutoValue

AutoValue implementation classes are necessarily non-`private`, since they are
generated into separate files. However examples like the following can't
deadlock as long as the only reference to the `AutoValue_Base` class is inside
`Base`, since there is no way for a thread to cause `AutoValue_Base` to be
initialized without first having initialized `Base`:

```java
@AutoValue
abstract class Base {
  abstract String bar();

  static final Object DEFAULT = new AutoValue_Base("bar");

  static Base of(String bar) {
    return new AutoValue_Base(bar);
  }
}
```

There is a separate Error Prone check, https://errorprone.info/bugpattern/AutoValueSubclassLeaked, to
prevent `AutoValue_` classes from being accessed outside the file containing the
corresponding `@AutoValue` base class.

## Private member classes

The check ignores references that cross from a `private` inner class (or any
class inside it) to its immediately enclosing class, e.g.

```java
public class A {
  private static Object benignCycle = new B.C();
  private static class B {
    public static class C extends A { }
  }
}
```

There is a cycle `A` -> `A.B.C` -> `A`, but (without reflection) it's not
possible to access `A.B.C` in a way that causes initialization until after A is
initialized.

There are situations where deadlocks involving `private` classes can still
occur, but the heuristic of ignoring paths cycles from `private` members is good
enough for most real-world examples of deadlocks that have been observed.

In the following, `A.C` can trigger initialization of `B`, despite `B` being
private.

```java
  public class A {
    private static Object bad_cycle = new B();
    private static class B extends A { }
    public static class C extends B { }
  }
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ClassInitializationDeadlock")` to the enclosing element.
