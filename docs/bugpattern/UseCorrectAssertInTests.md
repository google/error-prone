Java assert statements are not run unless targets explicitly opt in via runtime
flags to the JVM invocation. Tests are typically not run with asserts enabled,
meaning a test will continue to pass even if a bug is introduced since these
statements were never executed. To avoid this, use one of the assertion
libraries that are always enabled, such as JUnit's `org.junit.Assert` or
Google's Truth library. These will also produce richer contextual failure
diagnostics to aid and accelerate debugging.

Don't do this:

```java
@Test
public void testArray() {
  String[] arr = getArray();

  assert arr != null;
  assert arr.length == 1;
  assert arr[0].equals("hello");
}
```

Do this instead:

```java
import static com.google.common.truth.Truth.assertThat;

@Test
public void testArray() {
  String[] arr = getArray();

  assertThat(arr).isNotNull();
  assertThat(arr).hasLength(1);
  assertThat(arr[0]).isEqualTo("hello");
}
```
