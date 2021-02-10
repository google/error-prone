This error is triggered by calls to regex-accepting methods with invalid string
literals. These calls would cause a PatternSyntaxException at runtime.

We deliberately do not check `java.util.regex.Pattern#compile` as many of its
users are deliberately testing the regex compiler or using a vacuously true
regex.

`"."` is also discouraged, as it is a valid regex but is easy to mistake for
`"\\."`. Instead of e.g. `str.replaceAll(".", "x")`, prefer `Strings.repeat("x",
str.length())` or `CharMatcher.ANY.replaceFrom(str, "x")`.
