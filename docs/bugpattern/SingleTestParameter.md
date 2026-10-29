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

When the values of a `boolean`, `Boolean`, or `Enum` parameter are exhaustively
listed (excluding `null`), the explicit values can be omitted from
`@TestParameter` because it injects all possible values automatically:

```java
@Test
@TestParameters({"{enabled: true}", "{enabled: false}"})
public void testFlag(boolean enabled) {
  ...
}

@Test
@TestParameters({
  "{suit: HEARTS}",
  "{suit: DIAMONDS}",
  "{suit: CLUBS}",
  "{suit: SPADES}",
})
public void testSuit(Suit suit) {
  ...
}
```

```java
@Test
public void testFlag(@TestParameter boolean enabled) {
  ...
}

@Test
public void testSuit(@TestParameter Suit suit) {
  ...
}
```
