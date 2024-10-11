When Java introduced text blocks as a feature, it also introduced a new string
escape sequence `\s`. This escape sequence is another way to write a normal
space, but it has the advantage that it can be used at the end of a line in a
text block, where a normal space would be stripped.

This new escape sequence can easily be confused with the regex `\s`, which is a
metacharacter that matches any kind of whitespace character. To write that
metacharacter in a Java string, you must still write `\\s`: an escaped backslash
followed by an `s`.

There is little reason to ever write the Java escape `\s` except at the end of a
line. Either use a normal space, or switch to `\\s` if you are trying to write
the regex metacharacter.

```java
// Each line here is five characters long.
String colors = """
    one \s
    two \s
    three
    """;
```
