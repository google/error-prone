

Most references are never modified, and accidentally modifying a reference is a
potential source of bugs.

One option for avoiding accidental modification is to annotate all variables
that are not modified as `final`. However that approach is very noisy, since
most variables are never modified, and accidental modification is only avoided
if you remember to add `final` everywhere.

A better solution is to invert the default, and assume that all variables are
constant unless they are explicitly declared as modifiable. The `@Var`
annotation provides a way to mark variables as modifiable. The accompanying
Error Prone check enforces that all modifiable parameters and local variables
are explicitly annotated with `@Var`.

Since Java 8 can infer whether a local variable or parameter is effectively
`final`, and `@Var` makes it clear whether any variable is non-`final`,
explicitly marking local variables and parameters as `final` is discouraged.

The annotation can also be applied to fields to indicate that the field is
deliberately non-final, but the use of @Var on fields is not required.

