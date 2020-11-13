`@AutoValue` is used to represent pure data classes. Mocking these should not be
necessary: prefer constructing them in the same way production code would.

To make the argument another way: the fact that `AutoValue` classes are not
`final` is an implementation detail of the way they're generated. They should be
regarded as logically final insofar as they must not be extended by
non-generated code. If they were final, they also would not be mockable.

Instead of mocking:

```java
@Test
public void test() {
  MyAutoValue myAutoValue = mock(MyAutoValue.class);
  when(myAutoValue.getFoo()).thenReturn("foo");
}
```

Prefer simply constructing an instance:

```java
@Test
public void test() {
  MyAutoValue myAutoValue = MyAutoValue.create("foo");
}
```
