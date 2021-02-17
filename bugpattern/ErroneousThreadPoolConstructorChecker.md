---
title: ErroneousThreadPoolConstructorChecker
summary: Thread pool size will never go beyond corePoolSize if an unbounded queue
  is used
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Whenever a `ThreadPoolExecutor` is constructed with an unbounded `workQueue`,
the pool size will never go beyond `corePoolSize`. Using `maximumPoolSize`
greater than `corePoolSize` in such case will not have any impact on the maximum
bound of pool size.

Bad:

```java
new ThreadPoolExecutor(
    /* corePoolSize= */ 1,
    /* maximumPoolSize= */ 10,
    /* keepAliveTime= */ 60,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>());
```

Good:

```java
new ThreadPoolExecutor(
    /* corePoolSize= */ 10,
    /* maximumPoolSize= */ 10,
    /* keepAliveTime= */ 60,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>());
```

```java
new ThreadPoolExecutor(
    /* corePoolSize= */ 1,
    /* maximumPoolSize= */ 10,
    /* keepAliveTime= */ 60,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(QUEUE_CAPACITY));
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ErroneousThreadPoolConstructorChecker")` to the enclosing element.
