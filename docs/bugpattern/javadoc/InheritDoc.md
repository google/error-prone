The `@inheritDoc` tag should only be used on classes/interfaces which extend or
implement another and methods which override a method from a superclass.

```java
class Frobnicator {
  /** {@inheritDoc} */
  public void frobnicate() {
    // ...
  }
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InheritDoc")` to the element being
documented.
