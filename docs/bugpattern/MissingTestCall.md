Some test helpers such as `EqualsTester` require a terminating method call to be
of any use.

```java
  @Test
  public void string() {
    new EqualsTester()
        .addEqualityGroup("hello", new String("hello"))
        .addEqualityGroup("world", new String("world"))
        .addEqualityGroup(2, Integer.valueOf(2));
    // Oops: forgot to call `testEquals()`
  }
```
