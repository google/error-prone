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

Note that any Javadoc line starting with `@`, even embedded inside `<pre>` is
interpreted as a block tag by the Javadoc parser. As such, if you wish your
Javadoc to include a code block containing an annotation, you should surround
the snippet with `{@code ...}`. Alternatively, and if you are using a version of
javadoc prior to JDK 15, you may escape the symbol using `{@literal @}`

```java
/**
 * Designed to be overridden, such as:
 *
 * <pre>{@code
 * class Foo {
 *   @Override public String toString() {return "";}
 * }
 * }</pre>
 */
```

```java
/**
 * Designed to be overridden, such as:
 *
 * <pre>
 * class Foo {
 *   {@literal @}Override public String toString() {return "";}
 * }
 * </pre>
 */
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidBlockTag")` to the element being
documented.
