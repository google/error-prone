Format strings for the printf family of functions must follow the specification
in the documentation for [java.util.Formatter][formatter].

[formatter]: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html

The syntax for format specifiers is:

```
%[argument_index$][flags][width][.precision]conversion
```

Format strings can have the following errors:

## DuplicateFormatFlags

Duplicate flags are provided in the format specifier:

```java
String.format("e = %++10.4f", Math.E);
```

## FormatFlagsConversionMismatch

A conversion and flag are incompatible:

```java
String.format("%#b", Math.E);
```

## IllegalFormatCodePoint

The argument is a character with an invalid Unicode code point.

```java
String.format("%c", 0x110000);
```

## IllegalFormatConversion

The argument corresponding to the format specifier is of an incompatible type:

```java
String.format("%f", "abcd");
```

## IllegalFormatFlags

An illegal combination of flags is given:

```java
String.format("%-010d", 5);
```

## IllegalFormatPrecision

The conversion does not support a precision:

```java
String.format("%.c", 'c');
```

## IllegalFormatWidth

The conversion does not support a width:

```java
String.format("%1n");
```

## MissingFormatArgument

There is a format specifier which does not have a corresponding argument or if
an argument index refers to an argument that does not exist:

```java
String.format("%<s", "test");
```

## MissingFormatWidth

The format width is required:

```java
String.format("e = %-f", Math.E);
```

## UnknownFormatConversion

An unknown conversion is given:

```java
String.format("%r", "hello");
```
