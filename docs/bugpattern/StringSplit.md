`String.split(String)` has surprising behaviour. For example, consider the
following puzzler from
http://konigsberg.blogspot.com/2009/11/final-thoughts-java-puzzler-splitting.html:

```java
String[] nothing = "".split(":");
String[] bunchOfNothing = ":".split(":");
```

The result is `[""]` and `[]`!

More examples:

input    | `input.split(":")`  | `Splitter.on(':').split(input)`
-------- | ------------------- | -------------------------------
`""`     | `[""]`              | `[""]`
`":"`    | `[]`                | `["", ""]`
`":::"`  | `[]`                | `["", "", "", ""]`
`"a:::"` | `["a"]`             | `["a", "", "", ""]`
`":::b"` | `["", "", "", "b"]` | `["", "", "", "b"]`

Prefer either:

*   Guava's
    [`Splitter`](http://google.github.io/guava/releases/23.0/api/docs/com/google/common/base/Splitter.html),
    which has less surprising behaviour and provides explicit control over the
    handling of empty strings and the trimming of whitespace with `trimResults`
    and `omitEmptyStrings`.

*   [`String.split(String,
    int)`](https://docs.oracle.com/javase/9/docs/api/java/lang/String.html#split-java.lang.String-int-)
    and setting an explicit 'limit' to `-1` to match the behaviour of
    `Splitter`.

TIP: if you use `Splitter`, consider extracting the instance to a `static`
`final` field.
