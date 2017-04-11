When an assignment expression is used as the condition of a loop, it isn't clear
to the reader whether the assignment was deliberate or it was intended to be an
equality test. Parenthesis should be used around assignments in loop conditions
to make it clear to the reader that the assignment is deliberate.

That is, instead of this:

```java
void f(boolean x) {
  while (x = checkSomething()) {
    // ...
  }
}
```

Prefer `while ((x = checkSomething())) {` or `while (x == checkSomething()) {`.
