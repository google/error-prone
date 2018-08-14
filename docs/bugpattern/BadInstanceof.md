Flags `instanceof` checks where the expression can be determined to be a
supertype of the type it is compared to.

[JLS 15.28](https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.28)
specifically calls `instanceof` out as *not* being a compile-time constant
expression, so the usage of this pattern can lead to unreachable code that won't
be flagged by the compiler:

```java {.bad}
class Foo {
  void doSomething() {
    if (this instanceof Foo) { // BAD: always true
      return;
    }
    interestingProcessing();
  }
}
```

In general, an `instanceof` comparison against a superclass is equivalent to a
null check:

```java {.bad}
foo instanceof Foo
```

```java {.good}
foo != null
```
