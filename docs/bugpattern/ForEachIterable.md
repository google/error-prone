Prefer enhanced for loops instead of explicitly using an iterator where
possible.

That is, prefer this:

```java
for (T element : list) {
  doSomething(element);
}
```

to this:

```java
for (Iterator<T> iterator = list.iterator(); iterator.hasNext(); ) {
  doSomething(iterator.next());
}
```
