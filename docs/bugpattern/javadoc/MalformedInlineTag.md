This error is triggered by a malformed inline tag, anywhere @{tag appears
instead of {@tag inside a Javadoc comment. See [javadoc documentation][javadoc]
for more explanation on the use of inline tags.

[javadoc]: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#CHDJGIJB

## Suppression

Suppress by applying `@SuppressWarnings("MalformedInlineTag")` to the element
being documented.
