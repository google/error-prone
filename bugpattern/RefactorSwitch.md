---
title: RefactorSwitch
summary: This switch can be refactored to be more readable
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This checker aims to improve the readability of new-style arrow `switch`es by
simplifying and refactoring them.

### Simplifications:

*   When a `case` consists only of a `yield`ing some value, it can be re-written
    with just the value. For example, `case FOO -> { yield "bar"; }` can be
    shortened to `case FOO -> "bar";`
*   When a case has redundant braces around the right-hand side of the arrow,
    they can be removed. For example, `case FOO -> { System.out.println("bar");
    }` can shortened to `case FOO -> System.out.println("bar");`

### Factoring out `return switch`:

*   When every value of an `enum` is covered by a `case` which `return`s, the
    `return` can be factored out

``` {.bad}
enum SideOfCoin {OBVERSE, REVERSE};

private String renderName(SideOfCoin sideOfCoin) {
  switch(sideOfCoin) {
    case OBVERSE -> {
      return "Heads";
    }
    case REVERSE -> {
      return "Tails";
    }
  }
  // This should never happen, but removing this will cause a compile-time error
  throw new RuntimeException("Unknown side of coin");
}
```

The transformed code is simpler and elides the "should never happen" handler.

``` {.good}
enum SideOfCoin {OBVERSE, REVERSE};

private String renderName(SideOfCoin sideOfCoin) {
  return switch(sideOfCoin) {
    case OBVERSE -> "Heads";
    case REVERSE -> "Tails";
  };
}
```

*   When switching on something other than an `enum`, if the `case`s are
    exhaustive, then a similar refactoring can be performed.

### Factoring out assignment `switch`:

*   When every `case` just assigns a value to the same variable, the assignment
    can potentially be factored out
*   If a local variable is defined and then immediately overwritten by a
    `switch`, the definition and assignment can potentially be combined

``` {.bad}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void updateScore(Suit suit) {
  int score = 0;
  switch(suit) {
    case HEARTS, DIAMONDS -> {
      score = -1;
    }
    case SPADES -> {
      score = 2;
    }
    case CLUBS -> {
      score = 3;
    }
  }
}
```

Which can be consolidated:

``` {.good}
enum Suit {HEARTS, CLUBS, SPADES, DIAMONDS};

private void updateScore(Suit suit) {
   int score = switch(suit) {
       case HEARTS, DIAMONDS -> -1;
       case SPADES -> 2;
       case CLUBS -> 3;
    };
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RefactorSwitch")` to the enclosing element.
