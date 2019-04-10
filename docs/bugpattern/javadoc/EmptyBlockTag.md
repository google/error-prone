Block-level Javadoc tags like @param, @return, @throws, and @deprecated are
meant to provide additional information about the method. Without a description
they are no more informative than the method signature and should be removed.

Either add a description after the tag, or delete the low-information tag.

### @throws

```java {.bad}
interface Test {
  /**
   * @throws StorageException
   */
  void write() throws StorageException;
}
```

```java {.good}
interface Test {
  /**
   * @throws StorageException when unable to write to storage
   */
  void write() throws StorageException;
}
```

```java {.good}
interface Test {
  void write() throws StorageException;
}
```

### @param

```java {.bad}
interface Test {
  /**
   * Does a foo.
   *
   * @param p
   */
  void foo(int p);
}
```

```java {.good}
interface Test {
  /**
   * Does a foo.
   *
   * @param count How many knobs to turn
   */
  void foo(int count);
}
```

```java {.good}
interface Test {
  /**
   * Does a foo.
   */
   void foo(int count);
}
```

## Suppression

Suppress by applying `@SuppressWarnings("EmptyBlockTag")` to the element being
documented.
