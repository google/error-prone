---
title: ModifySourceCollectionInStream
summary: Modifying the backing source during stream operations may cause unintended results.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
From the javadoc for
[`java.util.stream: Non-interference`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/package-summary.html#NonInterference):

> Accordingly, behavioral parameters in stream pipelines whose source might not
> be concurrent should never modify the stream's data source. A behavioral
> parameter is said to interfere with a non-concurrent data source if it
> modifies, or causes to be modified, the stream's data source. The need for
> non-interference applies to all pipelines, not just parallel ones. Unless the
> stream source is concurrent, modifying a stream's data source during execution
> hg of a stream pipeline can cause exceptions, incorrect answers, or
> nonconformant behavior.

That is, prefer this:

```java
mutableValues.stream()
  .filter(x -> x < 5)
  .collect(Collectors.toList()) // Terminate stream before source modification.
  .forEach(mutableValues::remove);
```

to this:

```java
mutableValues.stream()
  .filter(x -> x < 5)
  .forEach(mutableValues::remove);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ModifySourceCollectionInStream")` to the enclosing element.
