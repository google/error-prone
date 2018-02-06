---
title: DoubleBraceInitialization
summary: Prefer collection factory methods or builders to the double-brace intitialization pattern.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The [double-brace initialization pattern][dbi] should be avoided—especially in
non-static contexts.

The double-brace pattern uses an instance-initializer in an anonymous inner
class to express the initialization of a class (often a collection) in a single
step.

Inner classes in a non-static context are terrific sources of memory leaks! If
you pass the collection somewhere that retains it, the entire instance you
created it from can no longer be garbage collected. Even if it is completely
unreachable. And if someone serializes the map? Yep, the entire creating
instance goes along for the ride (or if that fails, serializing the map fails,
which is also awfully strange). All this is completely nonobvious.

Luckily, there are more readable and more performant alternatives in the factory
methods and builders for `ImmutableList`, `ImmutableSet`, and `ImmutableMap`.

The `List.of`, `Set.of`, and `Map.of` static factories [added in Java
9](http://openjdk.java.net/jeps/269) are also a good choice.

That is, prefer this:

```java {.good}
ImmutableList.of("Denmark", "Norway", "Sweden");
```

Not this:

```java {.bad}
new ArrayList<>() {
  {
    add("Denmark");
    add("Norway");
    add("Sweden");
  }
};
```


[dbi]: https://stackoverflow.com/questions/1958636/what-is-double-brace-initialization-in-java

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DoubleBraceInitialization")` to the enclosing element.
