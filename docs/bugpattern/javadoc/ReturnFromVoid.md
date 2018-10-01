Methods which do not return anything should not have a `@return` tag.

```java {.bad}
/**
 * Frobnicates.
 *
 * @return a frobnicator
 */
void frobnicator(int a) {
  ...
}
```
