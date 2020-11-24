The [Google Java Style Guide §4.8.4.2][style] requires that within a switch
block, each statement group either terminates abruptly (with a `break`,
`continue`, `return` or `throw` statement), or is marked with a comment to
indicate that execution will or might continue into the next statement group.
This special comment is not required in the last statement group of the switch
block.

Example:

```java
switch (input) {
  case 1:
  case 2:
    prepareOneOrTwo();
    // fall through
  case 3:
    handleOneTwoOrThree();
    break;
  default:
    handleLargeNumber(input);
}
```

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4.2-switch-fall-through
