Any additional statements after the statement that is expected to throw will
never be executed in a passing test. This can lead to inappropriately passing
tests where later incorrect assertions are skipped by the thrown exception. For
instance, the final assertion in the following example will never be executed if
the call throws as expected.

```java
@Test
public void testRemoveFails() {
  AppendOnlyList list = new AppendOnlyList();
  list.add(0, "a");
  thrown.expect(UnsupportedOperationException.class);
  list.remove(0); // throws
  assertThat(list).hasSize(1); // never executed
}
```

[`ExpectedException`]: http://junit.org/junit4/javadoc/latest/org/junit/rules/ExpectedException.html
