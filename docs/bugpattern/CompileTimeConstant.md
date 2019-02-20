A method or constructor with one or more parameters whose declaration is
annotated with the `@CompileTimeConstant` type annotation must only be invoked
with corresponding actual parameters that are computed as compile-time constant
expressions, such as a literal or static final constant.

Getting Java 8 references to methods with `@CompileTimeConstant` parameters is
disallowed because we couldn't check if the method reference is later applied to
a compile-time constant. Use the methods directly instead.


For the same reason, it's also disallowed to create lambda expressions with
`@CompileTimeConstant` parameters.
