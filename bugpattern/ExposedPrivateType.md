---
title: ExposedPrivateType
summary: Private member classes should not be referenced in signatures of non-private
  members.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Classes referenced in signatures of non-private APIs should usually also be
non-private.

For example, consider:

```java
class Foo {
  private record Person(String name) {}

  public static void printMessage(Person person) {
    System.out.printf("hello %s\n", person.name());
  }
}
```

Because `Person` is `private`, clients outside the current compilation unit will
be unable to create an instance of it to pass to `printMessage`.

Prefer to make classes referenced in APIs at least as visible as the API, or
else if the API is only intended to be used in the current compilation unit it
should also be `private`.

That is, prefer:

```java
class Foo {
  public record Person(String name) {}

  public static void printMessage(Person person) {
    System.out.printf("hello %s\n", person.name());
  }
}
```

or:

```java
class Foo {
  private record Person(String name) {}

  private static void printMessage(Person person) {
    System.out.printf("hello %s\n", person.name());
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ExposedPrivateType")` to the enclosing element.
