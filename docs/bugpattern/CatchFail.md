Ignoring an exception and calling `fail()` is unnecessary, since an uncaught
exception will already cause a test to fail. It also makes the test output less
useful, since the exception's message and stack trace is lost.

Do this:

``` {.good}
@Test public void testFoo() throws Exception {
   int x = foos(); // the test fails if this throws
   assertThat(x).isEqualTo(42);
}
```

or this:

``` {.good}
@Test public void testFoo() throws Exception {
   int x;
   try {
     x = foos();
   } catch (Exception e) {
     throw new AssertionError("the test failed", e); // wraps the exception with additional context
   }
   assertThat(x).isEqualTo(42);
}
```

Not this:

``` {.bad}
@Test public void testFoo() {
   int x;
   try {
     x = foos();
   } catch (Exception e) {
     fail("the test failed"); // the exception message and stack trace is lost
   }
   assertThat(x).isEqualTo(42);
}
```
