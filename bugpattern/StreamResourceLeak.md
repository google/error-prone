---
title: StreamResourceLeak
summary: Streams that encapsulate a closeable resource should be closed using try-with-resources
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: FilesLinesLeak_

## The problem
The problem is described in the [javadoc] for `Files`.

## `Files.newDirectoryStream`

> When not using the try-with-resources construct, then directory stream's close
> method should be invoked after iteration is completed so as to free any
> resources held for the open directory.

## `Files.list`

> The returned stream encapsulates a `DirectoryStream`. If timely disposal of
> file system resources is required, the try-with-resources construct should be
> used to ensure that the stream's close method is invoked after the stream
> operations are completed.

## `Files.walk`

> The returned stream encapsulates one or more `DirectoryStreams`. If timely
> disposal of file system resources is required, the try-with-resources
> construct should be used to ensure that the stream's close method is invoked
> after the stream operations are completed. Operating on a closed stream will
> result in an `IllegalStateException`.

## `Files.find`

> The returned stream encapsulates one or more `DirectoryStreams`. If timely
> disposal of file system resources is required, the try-with-resources
> construct should be used to ensure that the stream's close method is invoked
> after the stream operations are completed. Operating on a closed stream will
> result in an `IllegalStateException`.

## `Files.lines`

> The returned stream encapsulates a `Reader`. If timely disposal of file system
> resources is required, the try-with-resources construct should be used to
> ensure that the stream's close method is invoked after the stream operations
> are completed.

[javadoc]: https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html

## The solution

To ensure the stream is closed, always use try-with-resources. For example, when
using `Files.lines`, do this:

```java {.good}
String input;
try (Stream<String> stream = Files.lines(path)) {
  input = stream.collect(Collectors.joining(", "));
}
```

Not this:

```java {.bad}
// the Reader is never closed!
String input = Files.lines(path).collect(Collectors.joining(", ");
```

## What about methods that return closeable streams? {#must-be-closed}

Methods that return `Stream`s that encapsulate a closeable resource can be
annotated with `com.google.errorprone.annotations.MustBeClosed` to ensure their
callers remember to close the stream.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StreamResourceLeak")` to the enclosing element.
