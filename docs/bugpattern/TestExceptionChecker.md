The use of `@Test(expected = FooException.class)` is strongly discouraged, since
the test passes if *any* statement throws an exception of the expected type.

For example, if `add(0, "a")` throws an `UnsupportedOperationException` below,
the test will pass without even executing `remove(0)`, much less testing whether
it throws the right kind of exception. Such false negatives are particularly
likely when testing for common unchecked exceptions like `NullPointerException`.

```java
@Test(expected = UnsupportedOperationException.class)
public void testRemoveFails() {
  AppendOnlyList list = new AppendOnlyList();
  list.add(0, "a");
  list.remove(0);
}
```

To avoid this issue, prefer JUnit's `assertThrows()` API:

```java
import static org.junit.Assert.assertThrows;

@Test
public void testRemoveFails() {
  AppendOnlyList list = new AppendOnlyList();
  list.add(0, "a");
  assertThrows(UnsupportedOperationException.class, () -> {
    list.remove(0);
  });
}
```
