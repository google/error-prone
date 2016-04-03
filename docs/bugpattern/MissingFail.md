When testing for exceptions in junit, it is easy to forget the call to `fail()`:

```java
try {
  someOperationThatShouldThrow();
  // forget to call Assert.fail()
} catch (SomeException expected) {
  assertThat(expected).hasMessage("Operation failed");
}
```

Without the call to `fail()`, the test is broken: it will pass if the exception
is never thrown *or* if the exception is thrown with the expected message.

If the try/catch block is defensive and the exception may not always be thrown,
then the exception should be named 'tolerated'.

## Detection

This checker uses heuristics that identify as many occurrences of the problem as
possible while keeping the false positive rate low (low single-digit
percentages).

## Heuristics

Five methods were explored to detect missing `fail()` calls, triggering if no
`fail()` is used in a `try/catch` statement within a JUnit test class:

* Cases in which the caught exception is called "expected".
* Cases in which there is a call to an `assert*()` method in the catch block.
* Cases in which "expected" shows up in a comment inside the `catch` block.
* Cases in which the `catch` block is empty.
* Cases in which the `try` block contains only a single statement.

Only the first three yield useful results and also required some more refinement
to reduce false positives. In addition, the checker does not trigger on comments
in the `catch` block due to implementation complexity.

To reduce false positives, no match is found if any of the following is true:

* Any method with `fail` in its name is present in either catch or try block.
* A `throw` statement or synonym (`assertTrue(false)`, etc.) is present in
  either `catch` or `try` block.
* The occurrence happens inside a `setUp`, `tearDown`, `@Before`, `@After`,
  `suite` or`main` method.
* The method returns from the `try` or `catch` block or immediately after.
* The exception caught is of type `InterruptedException`, `AssertionError`,
  `junit.framework.AssertionFailedError` or `Throwable`.
* The occurrence is inside a loop.
* The try block contains a `while(true)` loop.
* The `try` or `catch` block contains a `continue;` statement.
* The `try/catch` statement also contains a `finally` statement.
* A logging call is present in the `catch` block.

In addition, for occurrences which matched because they have a call to an
`assert*()` method in the catch block, no match is found if any of the following
characteristics are present:

* A field assignment in the catch block.
* A call to `assertTrue/False(boolean variable or field)` in the catch block.
* The last statement in the `try` block is an `assert*()` (that is not a noop:
  `assertFalse(false)`, `assertTrue(true))` or `Mockito.verify()` call.
