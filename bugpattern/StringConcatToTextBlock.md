---
title: StringConcatToTextBlock
summary: This string literal can be written more clearly as a text block
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using [text blocks] for strings that span multiple lines can make code easier to
read.

For example, prefer this:

```java
String message =
    """
    'The time has come,' the Walrus said,
    'To talk of many things:
    Of shoes -- and ships -- and sealing-wax --
    Of cabbages -- and kings --
    And why the sea is boiling hot --
    And whether pigs have wings.'
    """;
```

instead of this:

```java
String message =
    "'The time has come,' the Walrus said,\n"
        + "'To talk of many things:\n"
        + "Of shoes -- and ships -- and sealing-wax --\n"
        + "Of cabbages -- and kings --\n"
        + "And why the sea is boiling hot --\n"
        + "And whether pigs have wings.'\n";
```

## Trailing newlines

If the string should not contain a trailing newline, use a `\ ` to escape the
final newline in the text block. That is, these two strings are equivalent:

```java
String s = "hello\n" + "world";
```

```java
String s =
    """
    hello
    world\
    """;
```

The suggested fixes for this check preserve the exact contents of the original
string, so if the original string doesn't include a trailing newline the fix
will use a `\ ` to escape the last newline.

If the whitespace in the string isn't significant, for example because the
string value will be parsed by a parser that doesn't care about the trailing
newlines, consider removing the final `\ ` to improve the readability of the
string.

[text blocks]: https://docs.oracle.com/en/java/javase/23/text-blocks/index.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringConcatToTextBlock")` to the enclosing element.
