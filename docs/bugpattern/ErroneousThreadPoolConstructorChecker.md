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
