The compound assignment `E1 op= E2` could be mistaken for being equivalent to
`E1 = E1 op E2`. However, this is not the case: compound assignment operators
automatically cast the result of the computation to the type on the left hand
side. So `E1 op= E2` is actually equivalent to `E1 = (T) (E1 op E2)`, where `T`
is the type of `E1`.

If the type of the expression is wider than the type of the
variable (i.e. the variable is a byte, char, short, or float), then the
compound assignment will perform a narrowing primitive conversion. Attempting
to perform the equivalent simple assignment would generate a compilation error.

For example, the following does not compile:

```java
byte b = 0;
b = b << 1;
//    ^
// error: incompatible types: possible lossy conversion from int to byte
```

However, the compound assignment form is allowed:

```java
byte b = 0;
b <<= 1;
```

Similarly, if the expression is a floating point type (float or double),
and the variable is an integral type (long, int, short, byte, or char), then
an implicit conversion will be performed.

Example:

```java
long l = 180;
l = l * 2.0f;
//    ^
// error: incompatible types: possible lossy conversion from float to long
```

Again, the compound assignment form is permitted:

```java
long l = 180;
l *= 2.0f;
```

See Puzzle #9 in 'Java Puzzlers: Traps, Pitfalls, and Corner Cases' for more
information.
