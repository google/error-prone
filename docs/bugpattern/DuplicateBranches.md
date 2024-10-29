Branching constructs (`if` statements, `conditional` expressions) should contain
difference code in the two branches. Repeating identical code in both branches
is usually a bug.

For example:

```java
condition ? same : same
```

```java
if (condition) {
  same();
} else {
  same();
}
```

this usually indicates a typo where one of the branches was supposed to contain
different logic:

```java
condition ? something : somethingElse
```

```java
if (condition) {
  doSomething();
} else {
  doSomethingElse();
}
```
