When a boolean expression is a compile-time constant (e.g.: `2 < 1`, `1 == 1`,
`'a' < 'A'`), these expressions can be directly replaced with `true` or `false`,
as appropriate. In any context where these expressions are used, `true` or
`false` is a more readable alternative:

```java
if (2 < 1) {
  // Some code I don't want to run right now
}

while (1 == 1) {
  // Some loop that I will manually break out of
}

assert 1 != 2; // I want to force an AssertionFailure if assertions are enabled
```

```java
if (false) {
  // Some code I don't want to run right now
}

while (true) {
  // Some loop that I will manually break out of
}

assert false; // I want to force an AssertionFailure if assertions are enabled
```

When some boolean expression is a compile-time constant unexpectedly, it
generally represents a bug in the code:

```java
for (int i = 0; i < 100; i++) {
  System.out.println("Is " + i + " greater than 50?: " + (1 > 50));
}

// Prints "... true" 100 times, since i > 50 is mistyped as 1 > 50
```
