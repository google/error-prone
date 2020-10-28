Consider using `switch` instead of `if`/`else` for enums. That is, prefer this:

```java
switch (foo.getBar()) {
  case BAZ:
    doSomething();
    break;
  default:
    doSomethingElse();
}
```

instead of this:

```java
if (foo.getBar().equals(Bar.BAZ)) {
  doSomething();
} else {
  doSomethingElse();
}
```

Switches on `enums` have a few small advantages worth considering:

*   It sidesteps the `equals` vs. `==` debate.
*   You get to call `BAZ` by its simple name without a static import. That's
    good because you might not want to see it appear unqualified in other parts
    of the file where the context would not make its meaning so clear.
*   You have the option of
    [protection against missing cases][MissingCasesInEnumSwitch] if you want it.

[`MissingCasesInEnumSwitch`]: https:errorprone.info/bugpattern/MissingCasesInEnumSwitch
