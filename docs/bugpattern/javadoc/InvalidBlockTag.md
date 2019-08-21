A non-standard Javadoc block tag was used.

```java
/**
 * @returns two times n
 */
int twoTimes(int n) {
  return 2 * n;
}
```

```java
/**
 * @return two times n
 */
int twoTimes(int n) {
  return 2 * n;
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidBlockTag")` to the element being
documented.
