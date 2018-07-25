Consider a switch statement that doesn't handle all possible values and doesn't
have a default:

```java
enum Colors { RED, GREEN, BLUE }

switch (color) {
  case RED:
  case GREEN:
    paint(color);
    break;
}
```

The author's intent isn't clear. There are three possibilities:

1.  The default case is known to be impossible. This could be made clear by
    adding: \
    `default: throw new AssertionError(color);`

2.  The code intentionally 'falls out' of the switch on the default case, and
    execution continues below. This could be made clear by adding: \
    `default: // fall out`

3.  The code has a bug, and the missing cases should have been handled.

To avoid this ambiguity, the Google Java Style Guide [requires][style] each
switch statement on an enum type to either handle all values of the enum, or
have a default statement group.

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4.3-switch-default
