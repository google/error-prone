Preconditions checks take an error message to display if the check fails. The
error message is rarely needed, so it should either be cheap to construct or
constructed only when needed. This check ensures that these error messages are
not constructed using expensive methods that are evaluated eagerly.

Prefer this:

```java
checkNotNull(foo, "hello %s", name);
```

instead of this:

```java
checkNotNull(foo, String.format("hello %s", name));
```
