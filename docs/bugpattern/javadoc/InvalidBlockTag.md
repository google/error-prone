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

Note that any Javadoc line starting with `@`, even embedded inside `<pre>` and
`{@code ...}`, is interpereted as a block tag by the Javadoc parser. As such, if
you wish your Javadoc to include a code block containing an annotation, you
should generally avoid `{@code ...}` and instead write the HTML yourself,
manually escaping the `@` entity.

```java
/**
 * Designed to be overridden, such as:
 *
 * <pre>
 * class Foo {
 *   &#64;Override public String toString() {return "";}
 * }
 * </pre>
 */
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidBlockTag")` to the element being
documented.
