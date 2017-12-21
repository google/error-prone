From the [Java Collection
Tutorial](https://docs.oracle.com/javase/tutorial/collections/interfaces/collection.html):

> Note that `Iterator.remove` is the only safe way to modify a collection during
> iteration; the behavior is unspecified if the underlying collection is
> modified in any other way while the iteration is in progress.

That is, prefer this:

```java {.good}
Iterator<String> it = ids.iterator();
while (it.hasNext()) {
  if (shouldRemove(it.next())) {
    it.remove();
  }
}
```

to this:

```java {.bad}
for (String id : ids) {
  if (shouldRemove(id)) {
    ids.remove(id); // will cause a ConcurrentModificationException!
  }
}
```

TIP: This pattern is simpler with Java 8's
[`Collection.removeIf`](https://docs.oracle.com/javase/8/docs/api/java/util/Collection.html#removeIf-java.util.function.Predicate-):

    ```java {.good}
    ids.removeIf(id -> shouldRemove(id));
    ```
