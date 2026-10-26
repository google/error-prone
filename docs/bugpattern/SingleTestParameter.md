When a test method or constructor has only a single parameter, annotating that
parameter directly with `@TestParameter` is simpler and more concise than
annotating the method with `@TestParameters` YAML mappings.

```java
@Test
@TestParameters("{count: 1}")
@TestParameters("{count: 2}")
@TestParameters("{count: 3}")
public void myTest(int count) {
  ...
}
```

```java
@Test
public void myTest(@TestParameter({"1", "2", "3"}) int count) {
  ...
}
```
