Including a default case is redundant when switching on an enum type if the
switch handles all possible values of the enum. Removing the unnecessary default
allows Error Prone to enforce that the switch continues to handle all cases,
even if new values are added to the enum.

This check only reports cases where there's a simple fix, either because all
cases (including the default) return or throw, or the default is empty. If
execution may continue below the switch, then removing the default is still
recommended but requires additional refactoring.

## Examples

### All cases return or throw

Before:

```java
boolean isReady(State state) {
  switch (state) {
    case READY:
      return true;
    case DONE:
      return false;
    default:
      throw new AssertionError("unknown state: " + state);
  }
}
```

After:

```java
boolean isReady(State state) {
  switch (state) {
    case READY:
      return true;
    case DONE:
      return false;
  }
  throw new AssertionError("unknown state: " + state);
}
```

### The default case is empty

Before:

```java
boolean isReady(State state) {
  switch (state) {
    case READY:
      return true;
    case DONE:
      break;
    default:
      break;
  }
  return false;
}
```

After:

```java
boolean isReady(State state) {
  switch (state) {
    case READY:
      return true;
    case DONE:
      break;
  }
  return false;
}
```

### Execution may continue below

The fix requires additional refactoring, so this case isn't reported:

```java
boolean isReady(State state) {
  boolean result;
  switch (state) {
    case READY:
      result = true;
      break;
    case DONE:
      result = false;
      break;
    default:
      throw new AssertionError("unknown state: " + state);
  }
  return result;
}
```
