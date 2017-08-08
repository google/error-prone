JUnit's `fail()` and `assert*` methods throw an `AssertionError`, so using the
try/fail/catch pattern to test for `AssertionError` (or any of its super-types)
is incorrect. The following example will never fail:

```java
try {
  doSomething();
  fail("expected doSomething to throw AssertionError");
} catch (AssertionError expected) {
  // expected exception
}
```

To avoid this issue, prefer JUnit's `assertThrows()` or `expectThrows()` API:

```java
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.expectThrows;

@Test
public void testFailsWithAssertionError() {
  AssertionError thrown = expectThrows(
      AssertionError.class,
      () -> {
        doSomething();
      });
  assertThat(thrown).hasMessageThat().contains("something went terribly wrong");
}
```
