Lenient format strings, such as those accepted by `Preconditions`, are often
constructed lazily. The message is rarely needed, so it should either be cheap
to construct or constructed only when needed. This check ensures that these
messages are not constructed using expensive methods that are evaluated eagerly.

Prefer this:

```java
checkNotNull(foo, "hello %s", name);
```

instead of this:

```java
checkNotNull(foo, String.format("hello %s", name));
```
