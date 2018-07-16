---
title: EqualsGetClass
summary: Overriding Object#equals in a non-final class by using getClass rather than instanceof breaks substitutability of subclasses.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Implementing `Object#equals` using `getClass` violates substitutability of
subclasses. Suppose a `Complex` number type is defined,

```java {.bad}
public class Complex {
  public static final Complex ORIGIN = new Complex(0, 0);

  public final double re;
  public final double im;

  public Complex(double re, double im) {
    this.re = re;
    this.im = im;
  }

  public Complex signum() {
    if (equals(ORIGIN)) {
      return ORIGIN;
    }
    double norm = norm();
    return new Complex(re / norm, im / norm);
  }

  public double norm() {
    return Math.hypot(re, im);
  }

  @Override
  public equals(@Nullable Object other) {
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    Complex that = (Complex) other;
    return re == that.re && im == that.im;
  }
}
```

If `Complex` is later subclassed in some innocuous way, the `equals` call in
`signum` will fail, and `signum` will return `new Complex(NaN, NaN)`.

Interactions between `Complex` and its subclasses will also be confusing for
users. Two `Complex` instances which are observably the same may compare as
unequal.

Prefer using `instanceof`:

```java {.good}
public class Complex {
  ...

  @Override
  public equals(@Nullable Object other) {
    if (!(other instanceof Complex)) {
      return false;
    }
    Complex that = (Complex) other;
    return re == that.re && im == that.im;
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsGetClass")` to the enclosing element.
