Using a method reference to refer to the abstract method of the target type is
unnecessary. For example,

```java
Stream<Integer> filter(Stream<Integer> xs, Predicate<Integer> predicate) {
  return xs.filter(predicate::test);
}
```

```java
Stream<Integer> filter(Stream<Integer> xs, Predicate<Integer> predicate) {
  return xs.filter(predicate);
}
```
