Prefer method references to constant lambda expressions, or to a helper method
that does nothing but return a lambda.

That is, prefer this:

```java
private static Bar getBar(Foo foo) {
  return BarService.lookupBar(foo, defaultCredentials());
}

...

return someStream().map(MyClass::getBar)....;
```

to this:

```java
private static final Function<Foo, Bar> GET_BAR_FUNCTION =
    foo -> BarService.lookupBar(foo, defaultCredentials());

...

return someStream().map(GET_BAR_FUNCTION)....;
```

Advantages of using a method include:

*   It avoids hardcoding a named dependency on the functional interface type
    (e.g., is it a `com.google.common.base.Function` or a
    `java.util.function.Function`?)
*   It is easier to name the method than the constant.
*   It is more natural to test the method than the constant.
*   If the behavior is nontrivial, it's more natural to write javadoc for the
    method.

Be aware that this change is not purely syntactic: it affects the semantics of
your program in some small ways. In particular, evaluating the same method
reference twice is not guaranteed to return an identical object.

This means that, first, inlining the reference instead of storing a lambda may
cause additional memory allocations - usually this very slight performance cost
is worth the improved readability, but use your judgment if the performance
matters to you.

Secondly, if the correctness of your program depends on reference equality of
your lambda, inlining it may break you. Ideally, you should *not* depend on
reference equality for a lambda, but if you are doing so, consider not making
this change.

