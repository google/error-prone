---
title: IterablePathParameter
summary: Path implements Iterable<Path>; prefer Collection<Path> for clarity
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
[`java.file.nio.Path`] implements `Iterable<Path>`, and provides an iterator
over the name elements of the path. Declaring a parameter of type
`Iterable<Path>` is not recommended, since it allows clients to pass either an
`Iterable` of `Path`s, or a single `Path`. Using `Collection<Path>` prevents
clients from accidentally passing a single `Path`.

[`java.file.nio.Path`]: https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html

Example:

```java
void printPaths(Iterable<Path> paths) {
  for (Path path : paths) System.err.println(path);
}
```

```
printPaths(Paths.get("/tmp/hello"));
tmp
hello
```

```
printPaths(ImmutableList.of(Paths.get("/tmp/hello")));
/tmp/hello
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IterablePathParameter")` to the enclosing element.
