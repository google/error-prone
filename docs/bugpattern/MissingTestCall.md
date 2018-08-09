Some test helpers such as `EqualsTester` require a terminating method call to be
of any use.

```java {.bad}
  @Test
  public void string() {
    new EqualsTester()
        .addEqualityGroup("hello", new String("hello"))
        .addEqualityGroup("world", new String("world"))
        .addEqualityGroup(2, new Integer(2));
    // Oops: forgot to call `testEquals()`
  }
```
