Several of the methods in `java.util.Collections`, such as `sort` and `shuffle`,
modify collections in place. If you call one of these methods on a
newly-allocated collection and don't use it later, you are doing unnecessary
work. You probably meant to keep a reference to the newly-allocated copy of your
collection and use that in the rest of your code.

For example, this code sorts a new `ArrayList` and then throws away the result,
returning the unsorted original collection:

```java
public Collection<String> sort(Collection<String> foos) {
  Collections.sort(new ArrayList<>(foos));
  return foos;
}
```

The author probably meant:

```java
public Collection<String> sort(Collection<String> foos) {
  List<String> sortedFoos = new ArrayList<>(foos);
  Collections.sort(sortedFoos);
  return sortedFoos;
}
```
