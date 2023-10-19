The [Google Java Style Guide §4.7][style] states:

> Optional grouping parentheses are omitted only when author and reviewer agree
> that there is no reasonable chance the code will be misinterpreted without
> them, nor would they have made the code easier to read. It is not reasonable
> to assume that every reader has the entire Java operator precedence table
> memorized.

[style]: https://google.github.io/styleguide/javaguide.html#s4.7-grouping-parentheses

Use grouping parentheses to disambiguate expressions that could be
misinterpreted.

For example, consider this:

```java
boolean d = (a && b) || c;",
boolean e = (a || b) ? c : d;",
int z = (x + y) << 2;",
```

Instead of this:

```java
boolean r = a && b || c;",
boolean e = a || b ? c : d;",
int z = x + y << 2;",
```
