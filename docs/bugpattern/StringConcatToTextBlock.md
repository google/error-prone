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

[text blocks]: https://docs.oracle.com/en/java/javase/23/text-blocks/index.html
