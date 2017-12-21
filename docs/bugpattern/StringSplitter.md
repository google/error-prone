`String.split(String)` has surprising behavoiur. For example, consider the
following puzzler from
http://konigsberg.blogspot.com/2009/11/final-thoughts-java-puzzler-splitting.html:

```java
String[] nothing = "".split(":");
String[] bunchOfNothing = ":".split(":");
```

The result is `[""]` and `[]`!

Prefer guava's
[`String.splitter`](http://google.github.io/guava/releases/23.0/api/docs/com/google/common/base/Splitter.html),
which has more predicitable behaviour and provides explicit control over the
handling of empty strings and the trimming of whitespace.

TIP: consider extracting the `Splitter` instance to a static final field.
