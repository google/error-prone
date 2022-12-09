---
title: NewFileSystem
summary: Starting in JDK 13, this call is ambiguous with FileSystem.newFileSystem(Path,Map)
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Starting in JDK 13, calls to `FileSystem.newFileSystem(path, null)` are
ambiguous.

The calls match both:

*   [`FileSystem.newFileSystem(Path, ClassLoader)`](https://docs.oracle.com/en/java/javase/13/docs/api/java.base/java/nio/file/FileSystems.html#newFileSystem\(java.nio.file.Path,java.lang.ClassLoader\))
*   [`FileSystem.newFileSystem(Path, Map<?, ?>)`](https://docs.oracle.com/en/java/javase/13/docs/api/java.base/java/nio/file/FileSystems.html#newFileSystem\(java.nio.file.Path,java.util.Map\))

To disambiguate, add a cast to the desired type, to preserve the pre-JDK 13
behaviour.

That is, prefer this:

```java
FileSystem.newFileSystem(path, (ClassLoader) null);
```

Instead of this:

```java
FileSystem.newFileSystem(path, null);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NewFileSystem")` to the enclosing element.
