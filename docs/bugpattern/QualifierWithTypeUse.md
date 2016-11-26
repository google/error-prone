Allowing a qualifier annotation in [`TYPE_PARAMETER`] or [`TYPE_USE`] contexts
allows end users to write code like:

```java
@Inject Foo(List<@MyAnnotation String> strings)
```

Guice, Dagger, and other dependency injection frameworks don't currently see
type annotations in this context, so the above code is equivalent to:

```java
@Inject Foo(List<String> strings)
```

[`TYPE_PARAMETER`]: https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html#TYPE_PARAMETER
[`TYPE_USE`]: https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html#TYPE_USE
