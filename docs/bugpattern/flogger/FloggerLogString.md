Only compile-time constants may be passed to `log(String)`; use one of the
formatting `log` methods with a constant format string if at all possible.

Some common fixes include:

-   If the argument is a call to `String.format()`, just unwrap that call:
    `log(String.format("format %s", arg))` becomes `log("format %s", arg)`.
-   If the argument is `obj.toString()`, remove the `toString()` so that you're
    calling `log(Object)` instead. It's equivalent to `log("%s", obj)`.
-   If the argument is a parameter, add `@CompileTimeConstant` to that parameter
    if possible.
-   If the argument is an effectively final local variable that is initialized
    to a constant value, either explicitly add `final` to that variable or, if
    it's only used in this log statement, inline the constant value.
-   If the argument is a string concatenation with literal and variable parts,
    use the equivalent formatting. Instead of `log("foo " + bar + " baz")`,
    write `log("foo %s baz", bar)`.
-   If the argument is `exception.toString()` or `exception.getMessage()`,
    consider using `withCause(exception).log()` (with no separate log message)
    instead.

If none of these work, the easiest workaround is to use `"%s"` as a format
string, i.e. replace `log(expr)` with `log("%s", expr)`.
