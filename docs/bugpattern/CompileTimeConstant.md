A method or constructor with one or more parameters whose declaration is
annotated with the `@CompileTimeConstant` type annotation must only be invoked
with corresponding actual parameters that are computed as compile-time constant
expressions, specifically:

*   an expression that the Java compiler can determine a constant value for at
    compile time (see
    [JLS §15.28](https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.28)),
    or
*   a literal `null`, or
*   an enum constant, or
*   a class literal (e.g. `String.class`), or
*   a field of a class or record, which is declared `final` and is either:
    *   explicitly annotated with `@CompileTimeConstant`, or
    *   a well-known compatible constant field, e.g. from the Java standard
        library, or
*   a formal parameter of a method or constructor, which is effectively `final`
    (whether inferred or explicitly declared) and is annotated with
    `@CompileTimeConstant`, or
*   a concatenation expression of compile-time constant `String`s, or
*   a switch expressions where all case bodies are compile-time constants, or
*   a ternary expressions with both branches being compile-time constants, or
*   a call to a well-known constructor of an immutable type, where all arguments
    are compile-time constants, or
*   a call to one of the known immutable collection factory methods, where the
    provided elements are all compile-time constants.

For example, the following are valid compile-time constants:

*   `"some literal string"`
*   `"literal string" + compileTimeConstantParameter`
*   `debug ? compileTimeConstantParameter : "foo"`
*   `ImmutableList.of("a", "b", "c")`

When applied to fields, this check enforces that the field is `final` and has an
initializer which satisfies the above conditions.

Getting Java 8 references to methods with `@CompileTimeConstant` parameters is
disallowed because we couldn't check if the method reference is later applied to
a compile-time constant. Use the methods directly instead.

For the same reason, it's also disallowed to create lambda expressions with
`@CompileTimeConstant` parameters.
