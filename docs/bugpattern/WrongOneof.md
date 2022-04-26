When switching over a proto `one_of`, getters that don't match the current case
are guaranteed to be return a default instance:

```java
switch (foo.getBlahCase()) {
  case FOO:
    return foo.getFoo();
  case BAR:
    return foo.getFoo(); // should be foo.getBar()
}
```
