Calling `get()` on an `Optional` that is not present will result in a
`NoSuchElementException`.

This check detects cases where `get()` is called whent the optional is
definitely not present, e.g.:

```java
if (!o.isPresent()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

```java
if (o.isEmpty()) {
  return o.get(); // this will throw a NoSuchElementException
}
```

This cases are almost definitely bugs, the intent may have been to invert the
test:

```java
if (o.isPresent()) {
  return o.get();
}
```
