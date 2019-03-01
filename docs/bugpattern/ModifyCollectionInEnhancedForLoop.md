From the javadoc for
[`Iterator.remove`](https://docs.oracle.com/javase/9/docs/api/java/util/Iterator.html#remove--):

> The behavior of an iterator is unspecified if the underlying collection is
> modified while the iteration is in progress in any way other than by calling
> this method, unless an overriding class has specified a concurrent
> modification policy.

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
