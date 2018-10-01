A non-standard Javadoc tag was used, or was used in the wrong way. For example,
`@param` should be used as a block tag to describe parameters, but cannot be
used inline to link to parameters (prefer `{@code paramName}` for that).

```java {.bad}
/**
 * Doubles {@param n}
 *
 * @returns two times n
 */
int twoTimes(int n) {
  return 2 * n;
}
```

```java {.good}
/**
 * Doubles {@code n}
 *
 * @return two times n
 */
int twoTimes(int n) {
  return 2 * n;
}
```
