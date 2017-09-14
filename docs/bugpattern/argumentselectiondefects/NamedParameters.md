For clarity, and to avoid potentially incorrectly swapping arguments, arguments
may be explicitly matched to their parameter by preceding them with a block
comment containing the parameter name followed by an equals sign. Mismatches
between the name in the comment and the actual name will then cause a
compilation error.

## Parameter-name comment style

The style accepted for comments that name parameters is to use a block comment,
before the argument, including an equals sign.

```java
test(/* param1= */ arg1,
     /* param2= */ arg2);
```

The use of spaces (or not) around the parameter name and the equals sign is
optional.

### Rationale

There are a variety of styles in use for commenting arguments in method calls.
Adopting a consistent style should be valuable for readability. There are also a
variety of shortcomings with alternative techniques.

*Shortcomings with the use of line comments* (instead of block comments).
Firstly, they force a vertical layout of the method call even if it would fit on
one line. Secondly, associating line comments with arguments is hard to get
right. For example:

```java
test(arg1,  // param1
     arg2); // param2
```

In this example `param1` is in the same parse tree node as `arg2` (because its
after the comma) and `param2` is not even in the same parse tree node as the
method invocation (its after the semi-colon).

*Shortcomings with the use of block comments after the argument* arise because
its not always natural to put them in the right place. For example:

```java
// not as intended
test(arg1,  /* param1 */
     arg2); /* param2 */

// intended
test(arg1 /* param1 */,
     arg2 /* param2 */);
```

*The benefit of including the equals-sign* is that it helps associate the
comment with the correct argument. For example:

```java
test(arg1, /* param2= */
     arg2);
```

In this example the formatter has moved the comment that should be before `arg2`
onto the previous line. However, the inclusion of the equals sign means that
there is at least a visual cue that this has happened.

*Commonality with other languages*. Python support for named parameters uses an
equals sign, and the clang-tidy
[misc-comment-argument](https://clang.llvm.org/extra/clang-tidy/checks/misc-argument-comment.html)
check uses block-comments with equals signs.

