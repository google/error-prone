In certain contexts literal arguments - such as `0`, `""`, `true` and `false`,
or `null` - can make it difficult for readers to know what a method will do.
Defining methods that take boolean parameters or otherwise expect users to pass
in ambiguous literals is generally discouraged. However, when you must call such
a method you're encouraged to use the parameter name as an inline comment at the
call site, so that readers don't need to look at the method declaration to
understand the parameter's purpose.

Error Prone recognizes such comments that use the following formatting, and
emits an error if the comment doesn't match the name of the corresponding formal
parameter:

```java
booleanMethod(/* enableFoo= */ true);
```

If the comment deliberately does not match the formal parameter name, using a
regular block comment without the `=` is recommended: `/* enableFoo */`.

