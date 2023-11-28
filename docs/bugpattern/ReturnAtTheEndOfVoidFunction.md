Detects no-op `return` statements in `void` functions when they occur at the end
of the method.

Instead of:

```java
public void stuff() {
  int x = 5;
  return;
}
```

do:

```java
public void stuff() {
  int x = 5;
}
```
